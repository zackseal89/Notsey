package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiNoteEnhancer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isGeminiConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun structureAndTagNote(rawContent: String): EnhancedNoteResult = withContext(Dispatchers.IO) {
        if (!isGeminiConfigured()) {
            // Local fallback extraction
            val words = rawContent.trim().split("\\s+".toRegex())
            val title = if (words.size <= 6) rawContent.trim() else words.take(6).joinToString(" ") + "..."
            val tags = mutableListOf<String>()
            val lower = rawContent.lowercase()
            if (lower.contains("bug") || lower.contains("fix") || lower.contains("error")) tags.add("bug")
            if (lower.contains("todo") || lower.contains("task") || lower.contains("need")) tags.add("todo")
            if (lower.contains("idea") || lower.contains("feature") || lower.contains("concept")) tags.add("idea")
            if (lower.contains("claude") || lower.contains("agent") || lower.contains("code")) tags.add("agent")
            if (tags.isEmpty()) tags.add("note")

            return@withContext EnhancedNoteResult(
                title = if (title.isBlank()) "Quick Voice Note" else title,
                summary = rawContent,
                tags = tags.joinToString(",")
            )
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val prompt = """
                Extract a concise title (under 6 words), a clean polished summary/transcript, and 2-4 tags (comma-separated, e.g. 'claude,bug,ui') from the following voice or text note for an AI developer agent.
                Return ONLY valid JSON in this exact format:
                {
                   "title": "Title here",
                   "summary": "Clean formatted text or bullet points",
                   "tags": "tag1,tag2,tag3"
                }

                Note text:
                "$rawContent"
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
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
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallback(rawContent)
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

            EnhancedNoteResult(
                title = parsed.optString("title", "Quick Voice Note"),
                summary = parsed.optString("summary", rawContent),
                tags = parsed.optString("tags", "agent,note")
            )
        } catch (e: Exception) {
            fallback(rawContent)
        }
    }

    private fun fallback(rawContent: String): EnhancedNoteResult {
        val words = rawContent.trim().split("\\s+".toRegex())
        val title = if (words.size <= 6) rawContent.trim() else words.take(6).joinToString(" ") + "..."
        return EnhancedNoteResult(
            title = if (title.isBlank()) "Quick Voice Note" else title,
            summary = rawContent,
            tags = "voice,note"
        )
    }
}

data class EnhancedNoteResult(
    val title: String,
    val summary: String,
    val tags: String
)
