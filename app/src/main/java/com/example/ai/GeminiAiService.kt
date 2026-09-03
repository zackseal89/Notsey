package com.example.ai

import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

data class AiAnalysisResult(
    val title: String,
    val summary: String,
    val classification: String, // "IDEA", "BUG", "TASK", "FEATURE", "MEETING", "RESEARCH", "MISC"
    val tags: List<String>,
    val tasks: List<ExtractedTask>,
    val duplicateNoteId: Long? = null,
    val relatedNoteIds: List<Long> = emptyList()
)

data class ExtractedTask(
    val title: String,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val dueDate: String? = null
)

object GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isGeminiConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Transcribe audio recording using gemini-3.5-transcribe (or gemini-3.5-flash with inline audio data)
     */
    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext "Empty audio recording"
        }

        if (!isGeminiConfigured()) {
            return@withContext fallbackTranscription(audioFile)
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val bytes = audioFile.readBytes()
            // Cap at 15MB if very large
            val base64Audio = Base64.encodeToString(bytes.take(15 * 1024 * 1024).toByteArray(), Base64.NO_WRAP)

            // Using gemini-3.5-transcribe / gemini-3.5-flash with audio inline data
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-transcribe:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Transcribe the spoken audio into clear, accurate text verbatim. Do not add commentary or introductory remarks.")
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // If gemini-3.5-transcribe model fails or is unavailable, fallback to gemini-3.5-flash
                return@withContext transcribeWithFlash(base64Audio, apiKey, audioFile)
            }

            val responseBody = response.body?.string() ?: ""
            val root = JSONObject(responseBody)
            val transcript = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")?.trim() ?: ""

            if (transcript.isNotBlank()) transcript else fallbackTranscription(audioFile)
        } catch (e: Exception) {
            fallbackTranscription(audioFile)
        }
    }

    private fun transcribeWithFlash(base64Audio: String, apiKey: String, file: File): String {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Transcribe the spoken audio accurately into text verbatim.")
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
            }
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val root = JSONObject(body)
            root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")?.trim()?.ifBlank { fallbackTranscription(file) } ?: fallbackTranscription(file)
        } catch (e: Exception) {
            fallbackTranscription(file)
        }
    }

    private fun fallbackTranscription(audioFile: File): String {
        return "Voice recording captured (${audioFile.name}, ${audioFile.length() / 1024} KB). Ready for Claude Code and Antigravity processing."
    }

    /**
     * Analyze note content: generate title, summary, classify as IDEA/BUG/TASK/etc.,
     * extract tags, extract actionable tasks, and check for duplicates/relations against existing notes.
     */
    suspend fun analyzeAndClassifyNote(
        rawContent: String,
        existingNotesSummary: List<Pair<Long, String>> = emptyList()
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        if (!isGeminiConfigured() || rawContent.isBlank()) {
            return@withContext fallbackAnalysis(rawContent)
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val existingContext = if (existingNotesSummary.isNotEmpty()) {
                val listStr = existingNotesSummary.take(15).joinToString("\n") { (id, title) -> "[$id]: $title" }
                "\nExisting notes for duplicate/relation detection:\n$listStr"
            } else ""

            val prompt = """
                You are an AI assistant for developer agents (Claude Code, Codex, Cursor, Antigravity).
                Analyze the following note text.
                1. Generate a concise title (under 6 words).
                2. Generate a clean, high-density executive summary (1-3 sentences or bullet points).
                3. Classify into ONE category strictly from: ["IDEA", "BUG", "TASK", "FEATURE", "MEETING", "RESEARCH", "MISC"].
                4. Extract 2-5 relevant tags (lowercase, e.g. ["mcp", "android", "security"]).
                5. Extract any actionable tasks/subtasks, each with title, priority ("HIGH", "MEDIUM", "LOW"), and dueDate if mentioned.
                6. Check if this is a duplicate or related to any existing note IDs.
                $existingContext

                Note text:
                "$rawContent"

                Return ONLY raw JSON with this exact schema:
                {
                   "title": "Title under 6 words",
                   "summary": "Executive summary",
                   "classification": "IDEA",
                   "tags": ["tag1", "tag2"],
                   "tasks": [
                      {"title": "Actionable task 1", "priority": "HIGH", "dueDate": null}
                   ],
                   "duplicateOfNoteId": null,
                   "relatedNoteIds": []
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallbackAnalysis(rawContent)
            }

            val root = JSONObject(responseBody)
            val candidateText = root
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val cleanedJson = candidateText.replace("```json", "").replace("```", "").trim()
            val parsed = JSONObject(cleanedJson)

            val title = parsed.optString("title").ifBlank { fallbackTitle(rawContent) }
            val summary = parsed.optString("summary").ifBlank { rawContent }
            val classification = parsed.optString("classification", "IDEA").uppercase()
            val validClassification = if (classification in listOf("IDEA", "BUG", "TASK", "FEATURE", "MEETING", "RESEARCH", "MISC")) {
                classification
            } else "IDEA"

            val tagsList = mutableListOf<String>()
            val tagsArr = parsed.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    val t = tagsArr.optString(i).trim().lowercase()
                    if (t.isNotBlank()) tagsList.add(t)
                }
            }
            if (tagsList.isEmpty()) tagsList.add(validClassification.lowercase())

            val tasksList = mutableListOf<ExtractedTask>()
            val tasksArr = parsed.optJSONArray("tasks")
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val tObj = tasksArr.optJSONObject(i)
                    if (tObj != null) {
                        val taskTitle = tObj.optString("title")
                        val priority = tObj.optString("priority", "MEDIUM")
                        val dueDate = if (tObj.isNull("dueDate")) null else tObj.optString("dueDate")
                        if (taskTitle.isNotBlank()) {
                            tasksList.add(ExtractedTask(title = taskTitle, priority = priority, dueDate = dueDate))
                        }
                    }
                }
            }

            val dupId = if (parsed.isNull("duplicateOfNoteId")) null else parsed.optLong("duplicateOfNoteId")
            val relatedIds = mutableListOf<Long>()
            val relArr = parsed.optJSONArray("relatedNoteIds")
            if (relArr != null) {
                for (i in 0 until relArr.length()) {
                    relatedIds.add(relArr.getLong(i))
                }
            }

            AiAnalysisResult(
                title = title,
                summary = summary,
                classification = validClassification,
                tags = tagsList,
                tasks = tasksList,
                duplicateNoteId = if (dupId != null && dupId > 0) dupId else null,
                relatedNoteIds = relatedIds
            )
        } catch (e: Exception) {
            fallbackAnalysis(rawContent)
        }
    }

    /**
     * Generate semantic vector embeddings for intelligent search and context retrieval.
     * Uses gemini-embedding-2-preview per gemini-api skill.
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext FloatArray(768) { 0f }

        if (!isGeminiConfigured()) {
            return@withContext generateLocalDeterministicEmbedding(text)
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2-preview:embedContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("model", "models/gemini-embedding-2-preview")
                put("content", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text.take(2000)) })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext generateLocalDeterministicEmbedding(text)
            }

            val root = JSONObject(body)
            val valuesArr = root.optJSONObject("embedding")?.optJSONArray("values")
            if (valuesArr != null && valuesArr.length() > 0) {
                val floats = FloatArray(valuesArr.length())
                for (i in 0 until valuesArr.length()) {
                    floats[i] = valuesArr.getDouble(i).toFloat()
                }
                floats
            } else {
                generateLocalDeterministicEmbedding(text)
            }
        } catch (e: Exception) {
            generateLocalDeterministicEmbedding(text)
        }
    }

    /**
     * Local deterministic high-dimensional embedding generator for 100% offline reliability.
     * Generates a normalized 768-dimensional pseudo-semantic vector using word hash projections.
     */
    fun generateLocalDeterministicEmbedding(text: String): FloatArray {
        val dim = 768
        val vector = FloatArray(dim) { 0f }
        val tokens = text.lowercase().split("\\W+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return vector

        for (token in tokens) {
            val hash = token.hashCode()
            val idx1 = Math.abs(hash) % dim
            val idx2 = Math.abs(hash * 31 + 17) % dim
            val idx3 = Math.abs(hash * 37 + 53) % dim
            vector[idx1] += 1.0f
            vector[idx2] += 0.5f
            vector[idx3] += 0.25f
        }

        // Normalize vector to unit length
        var norm = 0f
        for (v in vector) {
            norm += v * v
        }
        val sqrtNorm = sqrt(norm)
        if (sqrtNorm > 0f) {
            for (i in 0 until dim) {
                vector[i] /= sqrtNorm
            }
        }
        return vector
    }

    private fun fallbackAnalysis(rawContent: String): AiAnalysisResult {
        val lower = rawContent.lowercase()
        val classification = when {
            lower.contains("bug") || lower.contains("error") || lower.contains("crash") || lower.contains("fail") || lower.contains("issue") -> "BUG"
            lower.contains("todo") || lower.contains("task") || lower.contains("action item") || lower.contains("implement") -> "TASK"
            lower.contains("feature") || lower.contains("add support") || lower.contains("new capability") -> "FEATURE"
            lower.contains("meeting") || lower.contains("sync") || lower.contains("discussed") || lower.contains("call") -> "MEETING"
            lower.contains("research") || lower.contains("investigate") || lower.contains("explore") -> "RESEARCH"
            lower.contains("idea") || lower.contains("proposal") || lower.contains("what if") -> "IDEA"
            else -> "IDEA"
        }

        val tags = mutableListOf<String>()
        tags.add(classification.lowercase())
        if (lower.contains("mcp") || lower.contains("claude") || lower.contains("agent")) tags.add("agent")
        if (lower.contains("database") || lower.contains("room") || lower.contains("sqlite")) tags.add("database")
        if (lower.contains("ui") || lower.contains("compose") || lower.contains("screen")) tags.add("ui")

        val tasks = mutableListOf<ExtractedTask>()
        val lines = rawContent.lines().filter { it.isNotBlank() }
        for (line in lines) {
            val trimmed = line.trim().removePrefix("-").removePrefix("*").trim()
            if (trimmed.startsWith("TODO", ignoreCase = true) ||
                trimmed.startsWith("FIX", ignoreCase = true) ||
                trimmed.startsWith("Need to", ignoreCase = true) ||
                trimmed.startsWith("Must", ignoreCase = true)) {
                tasks.add(ExtractedTask(title = trimmed, priority = if (classification == "BUG") "HIGH" else "MEDIUM"))
            }
        }
        if (tasks.isEmpty() && classification in listOf("TASK", "BUG")) {
            tasks.add(ExtractedTask(title = fallbackTitle(rawContent), priority = if (classification == "BUG") "HIGH" else "MEDIUM"))
        }

        return AiAnalysisResult(
            title = fallbackTitle(rawContent),
            summary = rawContent,
            classification = classification,
            tags = tags,
            tasks = tasks
        )
    }

    private fun fallbackTitle(text: String): String {
        val words = text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return if (words.size <= 6) text.trim().ifBlank { "Untitled Note" } else words.take(6).joinToString(" ") + "..."
    }
}
