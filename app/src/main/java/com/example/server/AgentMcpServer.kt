package com.example.server

import android.content.Context
import android.util.Log
import com.example.ai.GeminiAiService
import com.example.data.AudioEntity
import com.example.data.NoteEntity
import com.example.data.NoteRelationshipEntity
import com.example.data.NoteRepository
import com.example.data.TagEntity
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AgentLogEvent(
    val timestamp: String,
    val agentName: String,
    val method: String,
    val detail: String
)

class AgentMcpServer(
    private val context: Context,
    private val repository: NoteRepository
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _port = MutableStateFlow(8080)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://localhost:8080")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<AgentLogEvent>>(emptyList())
    val recentLogs: StateFlow<List<AgentLogEvent>> = _recentLogs.asStateFlow()

    fun start(targetPort: Int = 8080) {
        if (_isRunning.value) return
        _port.value = targetPort

        serverJob = serverScope.launch {
            try {
                serverSocket = ServerSocket(targetPort).apply {
                    reuseAddress = true
                }
                val localIp = NetworkUtils.getLocalIpAddress(context)
                _serverUrl.value = "http://$localIp:$targetPort"
                _isRunning.value = true
                logEvent("System", "SERVER_START", "MCP Server active on port $targetPort")

                while (isActive && _isRunning.value) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e("AgentMcpServer", "Failed to start server on port $targetPort", e)
                logEvent("System", "ERROR", "Failed to start: ${e.message}")
                stop()
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        logEvent("System", "SERVER_STOP", "MCP Server stopped")
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var agentHeader = "Agent Client"
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                val lower = line!!.lowercase()
                if (lower.startsWith("content-length:")) {
                    contentLength = line!!.substring(15).trim().toIntOrNull() ?: 0
                }
                if (lower.startsWith("user-agent:") || lower.startsWith("x-agent-name:")) {
                    val headerVal = line!!.substringAfter(":").trim()
                    if (headerVal.contains("Claude", ignoreCase = true)) agentHeader = "Claude Code"
                    else if (headerVal.contains("Codex", ignoreCase = true)) agentHeader = "Codex"
                    else if (headerVal.contains("Cursor", ignoreCase = true)) agentHeader = "Cursor"
                    else if (headerVal.contains("Antigravity", ignoreCase = true)) agentHeader = "Antigravity"
                    else if (headerVal.isNotBlank()) agentHeader = headerVal
                }
            }

            val body = if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val r = reader.read(buffer, readTotal, contentLength - readTotal)
                    if (r == -1) break
                    readTotal += r
                }
                String(buffer, 0, readTotal)
            } else ""

            routeRequest(method, path, body, out, agentHeader)
        } catch (e: Exception) {
            Log.e("AgentMcpServer", "Client handling error", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {}
        }
    }

    private suspend fun routeRequest(
        method: String,
        path: String,
        body: String,
        out: OutputStream,
        agentHeader: String
    ) {
        val cleanPath = path.substringBefore("?")
        val queryParams = parseQueryParams(path.substringAfter("?", ""))

        if (method == "OPTIONS") {
            sendResponse(out, 200, "OK", "application/json", "")
            return
        }

        when {
            cleanPath == "/" || cleanPath == "/api/status" -> {
                val stats = repository.getDatabaseTableStats()
                val json = JSONObject().apply {
                    put("service", "Agent Notes MCP Server")
                    put("version", "2.0.0")
                    put("status", "online")
                    put("tables", JSONObject(stats))
                    put("supported_agents", JSONArray(listOf("Claude Code", "Codex", "Cursor", "Antigravity")))
                    put("endpoints", JSONObject().apply {
                        put("mcp_jsonrpc", "/mcp")
                        put("notes", "/api/notes")
                        put("tasks", "/api/tasks")
                        put("schema", "/api/schema")
                    })
                }
                logEvent(agentHeader, "STATUS", "Checked server status")
                sendResponse(out, 200, "OK", "application/json", json.toString(2))
            }

            cleanPath == "/api/schema" && method == "GET" -> {
                val schema = getDatabaseSchemaJson()
                logEvent(agentHeader, "GET_SCHEMA", "Inspected database schema")
                sendResponse(out, 200, "OK", "application/json", schema.toString(2))
            }

            cleanPath == "/api/notes" && method == "GET" -> {
                val statusFilter = queryParams["status"]
                val classificationFilter = queryParams["classification"]
                val typeFilter = queryParams["type"]
                val limit = queryParams["limit"]?.toIntOrNull() ?: 50

                val notes = if (statusFilter.equals("pending", ignoreCase = true)) {
                    repository.getPendingNotesSync()
                } else {
                    repository.getAllNotesSync()
                }.filter { note ->
                    (typeFilter == null || note.type.equals(typeFilter, ignoreCase = true)) &&
                    (classificationFilter == null || note.classification.equals(classificationFilter, ignoreCase = true))
                }.take(limit)

                val array = JSONArray()
                for (note in notes) {
                    array.put(fullNoteToJson(note))
                }
                val responseJson = JSONObject().apply {
                    put("count", notes.size)
                    put("notes", array)
                }
                logEvent(agentHeader, "GET_NOTES", "Retrieved ${notes.size} notes")
                sendResponse(out, 200, "OK", "application/json", responseJson.toString(2))
            }

            cleanPath.startsWith("/api/notes/") && cleanPath.endsWith("/context") && method == "GET" -> {
                val idStr = cleanPath.removePrefix("/api/notes/").removeSuffix("/context")
                val id = idStr.toLongOrNull()
                if (id != null) {
                    val contextJson = retrieveContextForNote(id, agentHeader)
                    if (contextJson != null) {
                        logEvent(agentHeader, "RETRIEVE_CONTEXT", "Retrieved rich context for note #$id")
                        sendResponse(out, 200, "OK", "application/json", contextJson.toString(2))
                    } else {
                        sendResponse(out, 404, "Not Found", "application/json", "{\"error\":\"Note #$id not found\"}")
                    }
                } else {
                    sendResponse(out, 400, "Bad Request", "application/json", "{\"error\":\"Invalid note ID\"}")
                }
            }

            cleanPath.startsWith("/api/notes/") && cleanPath.endsWith("/audio") && method == "GET" -> {
                val idStr = cleanPath.removePrefix("/api/notes/").removeSuffix("/audio")
                val id = idStr.toLongOrNull()
                val note = if (id != null) repository.getNoteByIdSync(id) else null
                if (note?.audioPath != null && File(note.audioPath).exists()) {
                    logEvent(agentHeader, "GET_AUDIO", "Streamed voice note audio #${note.id}")
                    sendAudioFile(out, File(note.audioPath))
                } else {
                    sendResponse(out, 404, "Not Found", "application/json", "{\"error\":\"Audio file not found\"}")
                }
            }

            cleanPath.startsWith("/api/notes/") && method == "GET" -> {
                val idStr = cleanPath.removePrefix("/api/notes/")
                val id = idStr.toLongOrNull()
                val note = if (id != null) repository.getNoteByIdSync(id) else repository.getNoteByUuidSync(idStr)
                if (note != null) {
                    logEvent(agentHeader, "GET_NOTE", "Retrieved note #${note.id}: ${note.title}")
                    sendResponse(out, 200, "OK", "application/json", fullNoteToJson(note).toString(2))
                } else {
                    sendResponse(out, 404, "Not Found", "application/json", "{\"error\":\"Note not found\"}")
                }
            }

            cleanPath == "/api/tasks" && method == "GET" -> {
                val tasks = repository.getAllTasksSync()
                val array = JSONArray()
                for (t in tasks) {
                    array.put(JSONObject().apply {
                        put("id", t.id)
                        put("noteId", t.noteId)
                        put("title", t.title)
                        put("isCompleted", t.isCompleted)
                        put("priority", t.priority)
                        put("dueDate", t.dueDate ?: JSONObject.NULL)
                    })
                }
                logEvent(agentHeader, "GET_TASKS", "Retrieved ${tasks.size} tasks")
                sendResponse(out, 200, "OK", "application/json", array.toString(2))
            }

            cleanPath.startsWith("/api/notes/") && (method == "PATCH" || method == "POST") -> {
                val idStr = cleanPath.removePrefix("/api/notes/")
                val id = idStr.toLongOrNull()
                if (id == null) {
                    sendResponse(out, 400, "Bad Request", "application/json", "{\"error\":\"Invalid ID\"}")
                    return
                }
                val bodyJson = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
                val status = bodyJson.optString("agentStatus", "PROCESSED")
                val summary = bodyJson.optString("agentSummary", bodyJson.optString("summary", ""))
                val agent = bodyJson.optString("agentName", agentHeader)

                repository.updateAgentStatus(id, status, agent, summary)

                val runId = repository.startAgentRun(agent, "REST_API", id)
                repository.recordAgentAction(runId, id, agent, "WRITE_RESULT", summary)
                repository.completeAgentRun(runId, "COMPLETED", summary, null)
                repository.recordActivityEvent("AGENT_UPDATE", id, "$agent updated note #$id to $status")

                logEvent(agent, "PATCH_NOTE", "Updated note #$id -> $status")
                sendResponse(out, 200, "OK", "application/json", "{\"status\":\"ok\",\"noteId\":$id}")
            }

            // Model Context Protocol (MCP) JSON-RPC 2.0 Handler
            cleanPath == "/mcp" && method == "POST" -> {
                handleMcpJsonRpc(body, out, agentHeader)
            }

            else -> {
                sendResponse(out, 404, "Not Found", "application/json", "{\"error\":\"Unknown route $path\"}")
            }
        }
    }

    private suspend fun handleMcpJsonRpc(body: String, out: OutputStream, agentHeader: String) {
        try {
            val json = JSONObject(body)
            val id = json.opt("id")
            val method = json.optString("method", "")
            val params = json.optJSONObject("params")

            when (method) {
                "initialize" -> {
                    val result = JSONObject().apply {
                        put("protocolVersion", "2024-11-05")
                        put("serverInfo", JSONObject().apply {
                            put("name", "agent-notes-mcp")
                            put("version", "2.0.0")
                        })
                        put("capabilities", JSONObject().apply {
                            put("tools", JSONObject().apply {
                                put("listChanged", false)
                            })
                        })
                    }
                    logEvent(agentHeader, "MCP_INIT", "Initialized MCP session for $agentHeader")
                    sendMcpSuccess(out, id, result)
                }

                "tools/list" -> {
                    val tools = getMcpToolsSpecification()
                    val result = JSONObject().apply {
                        put("tools", tools)
                    }
                    logEvent(agentHeader, "MCP_TOOLS_LIST", "Listed ${tools.length()} MCP tools")
                    sendMcpSuccess(out, id, result)
                }

                "tools/call" -> {
                    val toolName = params?.optString("name", "") ?: ""
                    val args = params?.optJSONObject("arguments") ?: JSONObject()
                    handleMcpToolCall(id, toolName, args, out, agentHeader)
                }

                else -> {
                    val error = JSONObject().apply {
                        put("code", -32601)
                        put("message", "Method not found: $method")
                    }
                    sendMcpError(out, id, error)
                }
            }
        } catch (e: Exception) {
            val error = JSONObject().apply {
                put("code", -32700)
                put("message", "Parse error: ${e.message}")
            }
            sendMcpError(out, null, error)
        }
    }

    private suspend fun handleMcpToolCall(
        id: Any?,
        toolName: String,
        args: JSONObject,
        out: OutputStream,
        agentHeader: String
    ) {
        val runId = repository.startAgentRun(agentHeader, "MCP_TOOL", null)

        when (toolName) {
            "list_notes" -> {
                val status = args.optString("status", "all")
                val classification = args.optString("classification", "")
                val limit = args.optInt("limit", 15)

                val notes = if (status.equals("pending", ignoreCase = true)) {
                    repository.getPendingNotesSync()
                } else {
                    repository.getAllNotesSync()
                }.filter {
                    classification.isBlank() || it.classification.equals(classification, ignoreCase = true)
                }.take(limit)

                val array = JSONArray()
                for (n in notes) {
                    array.put(fullNoteToJson(n))
                }

                repository.recordAgentAction(runId, null, agentHeader, "LIST_NOTES", "Retrieved ${notes.size} notes (status=$status, classification=$classification)")
                repository.completeAgentRun(runId, "COMPLETED", "Fetched ${notes.size} notes", null)
                logEvent(agentHeader, "MCP_TOOL_CALL", "list_notes -> ${notes.size} items")
                sendMcpToolResult(out, id, array.toString(2))
            }

            "get_note" -> {
                val noteId = args.optLong("id", -1L)
                val note = repository.getNoteByIdSync(noteId)
                if (note != null) {
                    repository.recordAgentAction(runId, noteId, agentHeader, "GET_NOTE", "Fetched note #${note.id}")
                    repository.completeAgentRun(runId, "COMPLETED", "Retrieved note #$noteId", null)
                    logEvent(agentHeader, "MCP_TOOL_CALL", "get_note(#$noteId)")
                    sendMcpToolResult(out, id, fullNoteToJson(note).toString(2))
                } else {
                    repository.completeAgentRun(runId, "FAILED", null, "Note #$noteId not found")
                    sendMcpToolResult(out, id, "Note with ID $noteId not found", isError = true)
                }
            }

            "retrieve_context" -> {
                val noteId = args.optLong("note_id", -1L)
                val contextJson = retrieveContextForNote(noteId, agentHeader)
                if (contextJson != null) {
                    repository.recordAgentAction(runId, noteId, agentHeader, "CONTEXT_RETRIEVAL", "Retrieved context package for note #$noteId")
                    repository.recordActivityEvent("AGENT_RETRIEVAL", noteId, "$agentHeader retrieved context for note #$noteId")
                    repository.completeAgentRun(runId, "COMPLETED", "Retrieved context for note #$noteId", null)
                    logEvent(agentHeader, "MCP_TOOL_CALL", "retrieve_context(#$noteId)")
                    sendMcpToolResult(out, id, contextJson.toString(2))
                } else {
                    repository.completeAgentRun(runId, "FAILED", null, "Note #$noteId not found")
                    sendMcpToolResult(out, id, "Note with ID $noteId not found", isError = true)
                }
            }

            "search_notes" -> {
                val query = args.optString("query", "")
                val semantic = args.optBoolean("semantic", true)
                val limit = args.optInt("limit", 10)

                val resultsArray = JSONArray()
                if (semantic && query.isNotBlank()) {
                    val queryVector = GeminiAiService.generateEmbedding(query)
                    val scoredNotes = repository.semanticSearch(queryVector, limit)
                    for ((note, score) in scoredNotes) {
                        val obj = fullNoteToJson(note).apply {
                            put("semanticSimilarity", String.format(Locale.US, "%.3f", score))
                        }
                        resultsArray.put(obj)
                    }
                } else {
                    val keywordNotes = repository.searchNotesSync(query).take(limit)
                    for (note in keywordNotes) {
                        resultsArray.put(fullNoteToJson(note))
                    }
                }

                repository.recordAgentAction(runId, null, agentHeader, "SEARCH", "Searched '$query' (semantic=$semantic) -> ${resultsArray.length()} results")
                repository.completeAgentRun(runId, "COMPLETED", "Search completed", null)
                logEvent(agentHeader, "MCP_TOOL_CALL", "search_notes('$query', semantic=$semantic) -> ${resultsArray.length()} matches")
                sendMcpToolResult(out, id, resultsArray.toString(2))
            }

            "write_agent_result" -> {
                val noteId = args.optLong("note_id", -1L)
                val status = args.optString("status", "PROCESSED")
                val summary = args.optString("summary", "")
                val details = args.optString("action_details", summary)

                repository.updateAgentStatus(noteId, status, agentHeader, summary)
                repository.recordAgentAction(runId, noteId, agentHeader, "WRITE_RESULT", details)
                repository.recordActivityEvent("AGENT_PROCESSED", noteId, "$agentHeader wrote result to note #$noteId: $summary")
                repository.completeAgentRun(runId, "COMPLETED", summary, null)

                logEvent(agentHeader, "MCP_TOOL_CALL", "write_agent_result(#$noteId): $summary")
                sendMcpToolResult(out, id, "Successfully updated note #$noteId to $status with result: $summary")
            }

            "create_note" -> {
                val title = args.optString("title", "Agent Response")
                val content = args.optString("content", "")
                val classification = args.optString("classification", "TASK")
                val tags = args.optString("tags", "agent,auto")

                val note = NoteEntity(
                    title = title,
                    content = content,
                    type = "TEXT",
                    classification = classification,
                    tags = tags,
                    agentStatus = "PROCESSED",
                    agentName = agentHeader,
                    agentSummary = "Created by $agentHeader"
                )
                val newId = repository.insertNote(note)

                // Add extracted tasks if passed
                val tasksArr = args.optJSONArray("tasks")
                if (tasksArr != null) {
                    val taskEntities = mutableListOf<TaskEntity>()
                    for (i in 0 until tasksArr.length()) {
                        val tStr = tasksArr.optString(i)
                        if (tStr.isNotBlank()) {
                            taskEntities.add(TaskEntity(noteId = newId, title = tStr))
                        }
                    }
                    if (taskEntities.isNotEmpty()) repository.insertTasks(taskEntities)
                }

                // Generate embedding for new note
                val embVector = GeminiAiService.generateEmbedding("$title $content")
                repository.insertEmbedding(com.example.data.EmbeddingEntity(
                    noteId = newId,
                    embeddingJson = JSONArray(embVector.toList()).toString()
                ))

                repository.recordAgentAction(runId, newId, agentHeader, "CREATE_NOTE", "Created note #$newId")
                repository.recordActivityEvent("NOTE_CREATED", newId, "$agentHeader created note #$newId: $title")
                repository.completeAgentRun(runId, "COMPLETED", "Created note #$newId", null)

                logEvent(agentHeader, "MCP_TOOL_CALL", "create_note -> #$newId ($title)")
                sendMcpToolResult(out, id, "Created note #$newId with title: '$title'")
            }

            "add_tasks" -> {
                val noteId = args.optLong("note_id", -1L)
                val tasksArr = args.optJSONArray("tasks")
                var addedCount = 0
                if (noteId > 0 && tasksArr != null) {
                    val list = mutableListOf<TaskEntity>()
                    for (i in 0 until tasksArr.length()) {
                        val item = tasksArr.get(i)
                        if (item is JSONObject) {
                            list.add(TaskEntity(
                                noteId = noteId,
                                title = item.optString("title"),
                                priority = item.optString("priority", "MEDIUM"),
                                dueDate = if (item.isNull("dueDate")) null else item.optString("dueDate")
                            ))
                        } else if (item is String && item.isNotBlank()) {
                            list.add(TaskEntity(noteId = noteId, title = item))
                        }
                    }
                    repository.insertTasks(list)
                    addedCount = list.size
                }

                repository.recordAgentAction(runId, noteId, agentHeader, "TASK_EXTRACTION", "Added $addedCount tasks to note #$noteId")
                repository.completeAgentRun(runId, "COMPLETED", "Added $addedCount tasks", null)
                logEvent(agentHeader, "MCP_TOOL_CALL", "add_tasks(#$noteId) -> $addedCount tasks")
                sendMcpToolResult(out, id, "Added $addedCount actionable tasks to note #$noteId")
            }

            "link_notes" -> {
                val sourceId = args.optLong("source_note_id", -1L)
                val targetId = args.optLong("target_note_id", -1L)
                val relationshipType = args.optString("relationship_type", "RELATED")
                val confidence = args.optDouble("confidence", 0.85).toFloat()
                val explanation = args.optString("explanation", "Linked by $agentHeader")

                if (sourceId > 0 && targetId > 0) {
                    val rel = NoteRelationshipEntity(
                        sourceNoteId = sourceId,
                        targetNoteId = targetId,
                        relationshipType = relationshipType,
                        confidence = confidence,
                        explanation = explanation
                    )
                    repository.insertRelationship(rel)
                    if (relationshipType.equals("DUPLICATE", ignoreCase = true)) {
                        repository.updateDuplicateStatus(sourceId, targetId)
                    }

                    repository.recordAgentAction(runId, sourceId, agentHeader, "RELATIONSHIP", "Linked #$sourceId to #$targetId as $relationshipType")
                    repository.completeAgentRun(runId, "COMPLETED", "Linked notes", null)
                    logEvent(agentHeader, "MCP_TOOL_CALL", "link_notes(#$sourceId -> #$targetId as $relationshipType)")
                    sendMcpToolResult(out, id, "Successfully linked note #$sourceId and #$targetId as $relationshipType")
                } else {
                    repository.completeAgentRun(runId, "FAILED", null, "Invalid note IDs")
                    sendMcpToolResult(out, id, "Invalid note IDs provided", isError = true)
                }
            }

            "list_tasks" -> {
                val tasks = repository.getAllTasksSync()
                val isCompletedFilter = if (args.has("is_completed")) args.optBoolean("is_completed") else null
                val filtered = tasks.filter { isCompletedFilter == null || it.isCompleted == isCompletedFilter }

                val arr = JSONArray()
                for (t in filtered) {
                    arr.put(JSONObject().apply {
                        put("id", t.id)
                        put("noteId", t.noteId)
                        put("title", t.title)
                        put("isCompleted", t.isCompleted)
                        put("priority", t.priority)
                        put("dueDate", t.dueDate ?: JSONObject.NULL)
                    })
                }
                repository.completeAgentRun(runId, "COMPLETED", "Listed ${filtered.size} tasks", null)
                logEvent(agentHeader, "MCP_TOOL_CALL", "list_tasks -> ${filtered.size} items")
                sendMcpToolResult(out, id, arr.toString(2))
            }

            "get_database_schema" -> {
                val schema = getDatabaseSchemaJson()
                repository.completeAgentRun(runId, "COMPLETED", "Returned database schema", null)
                logEvent(agentHeader, "MCP_TOOL_CALL", "get_database_schema")
                sendMcpToolResult(out, id, schema.toString(2))
            }

            else -> {
                repository.completeAgentRun(runId, "FAILED", null, "Unknown tool: $toolName")
                sendMcpToolResult(out, id, "Unknown tool: $toolName", isError = true)
            }
        }
    }

    private suspend fun retrieveContextForNote(noteId: Long, agentHeader: String): JSONObject? {
        val note = repository.getNoteByIdSync(noteId) ?: return null
        val tasks = repository.getTasksForNoteSync(noteId)
        val audioList = repository.getAudioForNoteSync(noteId)
        val tags = repository.getTagsForNoteSync(noteId)
        val relationships = repository.getRelationshipsForNoteSync(noteId)

        // Related notes retrieval
        val relatedNotesArr = JSONArray()
        for (rel in relationships) {
            val otherId = if (rel.sourceNoteId == noteId) rel.targetNoteId else rel.sourceNoteId
            val otherNote = repository.getNoteByIdSync(otherId)
            if (otherNote != null) {
                relatedNotesArr.put(JSONObject().apply {
                    put("id", otherNote.id)
                    put("title", otherNote.title)
                    put("classification", otherNote.classification)
                    put("relationshipType", rel.relationshipType)
                    put("confidence", rel.confidence)
                    put("explanation", rel.explanation ?: JSONObject.NULL)
                })
            }
        }

        val localIp = NetworkUtils.getLocalIpAddress(context)
        val currentPort = _port.value

        return JSONObject().apply {
            put("targetNote", fullNoteToJson(note))
            put("tasks", JSONArray().apply {
                for (t in tasks) {
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("isCompleted", t.isCompleted)
                        put("priority", t.priority)
                        put("dueDate", t.dueDate ?: JSONObject.NULL)
                    })
                }
            })
            put("audioFiles", JSONArray().apply {
                for (a in audioList) {
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("streamUrl", "http://$localIp:$currentPort/api/notes/${note.id}/audio")
                        put("durationMs", a.durationMs)
                        put("fileSizeBytes", a.fileSizeBytes)
                        put("mimeType", a.mimeType)
                    })
                }
            })
            put("tags", JSONArray(tags.map { it.tag }))
            put("relatedNotes", relatedNotesArr)
            put("duplicateOf", note.duplicateOfNoteId ?: JSONObject.NULL)
            put("contextInstructions", "Target agent ($agentHeader): You can inspect the tasks, audio recording, and related notes above. Use write_agent_result to update the note with your code resolution or summary.")
        }
    }

    private suspend fun fullNoteToJson(note: NoteEntity): JSONObject {
        val localIp = NetworkUtils.getLocalIpAddress(context)
        val currentPort = _port.value
        val tasks = repository.getTasksForNoteSync(note.id)

        return JSONObject().apply {
            put("id", note.id)
            put("uuid", note.uuid)
            put("title", note.title)
            put("content", note.content)
            put("transcript", note.transcript ?: JSONObject.NULL)
            put("summary", note.summary ?: JSONObject.NULL)
            put("type", note.type)
            put("classification", note.classification)
            put("tags", note.tags)
            put("duplicateOfNoteId", note.duplicateOfNoteId ?: JSONObject.NULL)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
            put("agentStatus", note.agentStatus)
            put("agentName", note.agentName ?: JSONObject.NULL)
            put("agentSummary", note.agentSummary ?: JSONObject.NULL)
            if (note.type == "VOICE" && note.audioPath != null) {
                put("audioUrl", "http://$localIp:$currentPort/api/notes/${note.id}/audio")
                put("durationMs", note.audioDurationMs)
            }
            put("tasksCount", tasks.size)
            put("tasks", JSONArray().apply {
                for (t in tasks) {
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("isCompleted", t.isCompleted)
                        put("priority", t.priority)
                    })
                }
            })
        }
    }

    private fun getMcpToolsSpecification(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("name", "list_notes")
                put("description", "Fetch recent voice and text notes recorded on user's Android phone with classification and status filters")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("status", JSONObject().apply {
                            put("type", "string")
                            put("description", "Filter by status: 'pending', 'processed', or 'all'")
                        })
                        put("classification", JSONObject().apply {
                            put("type", "string")
                            put("description", "Filter by classification: 'IDEA', 'BUG', 'TASK', 'FEATURE', 'MEETING', 'RESEARCH', 'MISC'")
                        })
                        put("limit", JSONObject().apply {
                            put("type", "integer")
                            put("description", "Max number of notes to retrieve (default 15)")
                        })
                    })
                })
            })

            put(JSONObject().apply {
                put("name", "get_note")
                put("description", "Get full transcribed text, classification, summary, tasks, audio URL, and metadata for a specific note")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("id", JSONObject().apply {
                            put("type", "integer")
                            put("description", "The numeric ID of the note")
                        })
                    })
                    put("required", JSONArray().apply { put("id") })
                })
            })

            put(JSONObject().apply {
                put("name", "retrieve_context")
                put("description", "Retrieve rich context for a note including full transcript, extracted tasks, audio streams, and related notes")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("note_id", JSONObject().apply {
                            put("type", "integer")
                            put("description", "Target note ID to retrieve context package for")
                        })
                    })
                    put("required", JSONArray().apply { put("note_id") })
                })
            })

            put(JSONObject().apply {
                put("name", "search_notes")
                put("description", "Search notes using AI semantic embeddings or keywords across transcripts, titles, and summaries")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Search query text or semantic concept")
                        })
                        put("semantic", JSONObject().apply {
                            put("type", "boolean")
                            put("description", "Set to true for AI vector cosine similarity search (default true)")
                        })
                        put("limit", JSONObject().apply {
                            put("type", "integer")
                            put("description", "Max number of notes to return")
                        })
                    })
                    put("required", JSONArray().apply { put("query") })
                })
            })

            put(JSONObject().apply {
                put("name", "write_agent_result")
                put("description", "Write an agent's resolution, generated code, or action plan back to the phone, marking the note as PROCESSED")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("note_id", JSONObject().apply {
                            put("type", "integer")
                            put("description", "The note ID being processed")
                        })
                        put("status", JSONObject().apply {
                            put("type", "string")
                            put("description", "Status (typically 'PROCESSED')")
                        })
                        put("summary", JSONObject().apply {
                            put("type", "string")
                            put("description", "Executive summary of the agent's work")
                        })
                        put("action_details", JSONObject().apply {
                            put("type", "string")
                            put("description", "Detailed action items, git commit info, or code changes")
                        })
                    })
                    put("required", JSONArray().apply { put("note_id"); put("summary") })
                })
            })

            put(JSONObject().apply {
                put("name", "create_note")
                put("description", "Create a new follow-up note or agent response directly in the user's phone database")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("title", JSONObject().apply { put("type", "string") })
                        put("content", JSONObject().apply { put("type", "string") })
                        put("classification", JSONObject().apply { put("type", "string") })
                        put("tags", JSONObject().apply { put("type", "string") })
                        put("tasks", JSONObject().apply {
                            put("type", "array")
                            put("items", JSONObject().apply { put("type", "string") })
                        })
                    })
                    put("required", JSONArray().apply { put("title"); put("content") })
                })
            })

            put(JSONObject().apply {
                put("name", "add_tasks")
                put("description", "Add structured actionable checklist items to a note on the user's phone")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("note_id", JSONObject().apply { put("type", "integer") })
                        put("tasks", JSONObject().apply {
                            put("type", "array")
                            put("description", "Array of task title strings or task objects")
                        })
                    })
                    put("required", JSONArray().apply { put("note_id"); put("tasks") })
                })
            })

            put(JSONObject().apply {
                put("name", "link_notes")
                put("description", "Establish relationship or duplicate link between two notes")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("source_note_id", JSONObject().apply { put("type", "integer") })
                        put("target_note_id", JSONObject().apply { put("type", "integer") })
                        put("relationship_type", JSONObject().apply {
                            put("type", "string")
                            put("description", "'DUPLICATE', 'RELATED', 'BLOCKS', 'DEPENDS_ON'")
                        })
                        put("confidence", JSONObject().apply { put("type", "number") })
                        put("explanation", JSONObject().apply { put("type", "string") })
                    })
                    put("required", JSONArray().apply { put("source_note_id"); put("target_note_id") })
                })
            })

            put(JSONObject().apply {
                put("name", "list_tasks")
                put("description", "List all actionable tasks across notes, with completion filter")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("is_completed", JSONObject().apply { put("type", "boolean") })
                    })
                })
            })

            put(JSONObject().apply {
                put("name", "get_database_schema")
                put("description", "Get the SQLite schema definition of all 9 core tables for Agent Notes")
                put("inputSchema", JSONObject().apply {
                    put("type", "object")
                })
            })
        }
    }

    private fun getDatabaseSchemaJson(): JSONObject {
        return JSONObject().apply {
            put("database", "agent_notes_db (SQLite via Room)")
            put("tables", JSONObject().apply {
                put("notes", "id (PK), uuid, title, content, transcript, type, classification, summary, audioPath, audioDurationMs, videoUri, tags, duplicateOfNoteId, createdAt, updatedAt, agentStatus, agentName, agentSummary, isArchived")
                put("audio", "id (PK), noteId (FK), filePath, durationMs, fileSizeBytes, mimeType, sampleRate, createdAt")
                put("tags", "id (PK), noteId (FK), tag")
                put("tasks", "id (PK), noteId (FK), title, isCompleted, priority, dueDate, createdAt")
                put("note_relationships", "id (PK), sourceNoteId (FK), targetNoteId (FK), relationshipType, confidence, explanation, createdAt")
                put("embeddings", "id (PK), noteId (FK), embeddingJson, modelVersion, dimension, createdAt")
                put("agent_runs", "id (PK), agentName, triggerSource, status, targetNoteId, summary, error, startedAt, completedAt")
                put("agent_actions", "id (PK), runId (FK), noteId, agentName, actionType, details, timestamp")
                put("activity_events", "id (PK), eventType, noteId, description, payloadJson, timestamp")
            })
        }
    }

    private fun sendMcpToolResult(out: OutputStream, id: Any?, text: String, isError: Boolean = false) {
        val result = JSONObject().apply {
            put("content", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                })
            })
            if (isError) put("isError", true)
        }
        sendMcpSuccess(out, id, result)
    }

    private fun sendMcpSuccess(out: OutputStream, id: Any?, result: JSONObject) {
        val json = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id ?: JSONObject.NULL)
            put("result", result)
        }
        sendResponse(out, 200, "OK", "application/json", json.toString())
    }

    private fun sendMcpError(out: OutputStream, id: Any?, error: JSONObject) {
        val json = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id ?: JSONObject.NULL)
            put("error", error)
        }
        sendResponse(out, 200, "OK", "application/json", json.toString())
    }

    private fun sendAudioFile(out: OutputStream, file: File) {
        try {
            val length = file.length()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: audio/mp4\r\n" +
                    "Content-Length: $length\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray())
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                }
            }
            out.flush()
        } catch (e: Exception) {
            // Client may have disconnected
        }
    }

    private fun sendResponse(
        out: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        try {
            val bodyBytes = body.toByteArray(Charsets.UTF_8)
            val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                    "Content-Type: $contentType; charset=utf-8\r\n" +
                    "Content-Length: ${bodyBytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, PATCH, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: Content-Type, Authorization, User-Agent, X-Agent-Name\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray())
            out.write(bodyBytes)
            out.flush()
        } catch (e: Exception) {
            // Client closed
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = queryString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                result[key] = value
            } else if (pair.isNotEmpty()) {
                result[pair] = ""
            }
        }
        return result
    }

    fun logEvent(agentName: String, method: String, detail: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val event = AgentLogEvent(timestamp, agentName, method, detail)
        val current = _recentLogs.value.toMutableList()
        current.add(0, event)
        if (current.size > 25) {
            current.removeAt(current.size - 1)
        }
        _recentLogs.value = current
    }

    // Helper to simulate a local agent request in-app
    suspend fun simulateAgentRetrieval(agentName: String = "Claude Code") {
        val pendingNotes = repository.getPendingNotesSync()
        if (pendingNotes.isEmpty()) {
            logEvent(agentName, "SIMULATION", "Queried pending notes: 0 pending found")
            return
        }
        val target = pendingNotes.first()
        val runId = repository.startAgentRun(agentName, "SIMULATOR", target.id)
        logEvent(agentName, "SIMULATION", "Retrieved note #${target.id}: '${target.title}'")

        val simulatedSummary = when (target.classification) {
            "BUG" -> "Root cause identified and test case drafted. Action plan: Patch applied to local branch."
            "TASK" -> "Task requirements decomposed. Executed automated migration and tests."
            "IDEA" -> "Architectural feasibility reviewed. Drafted prototype specification."
            else -> "Voice note analyzed. Action plan: Implemented requested logic and generated architectural summary."
        }

        repository.updateAgentStatus(
            id = target.id,
            status = "PROCESSED",
            agentName = agentName,
            summary = simulatedSummary
        )
        repository.recordAgentAction(runId, target.id, agentName, "WRITE_RESULT", simulatedSummary)
        repository.completeAgentRun(runId, "COMPLETED", simulatedSummary, null)
        repository.recordActivityEvent("AGENT_PROCESSED", target.id, "$agentName simulated execution: $simulatedSummary")

        logEvent(agentName, "SIMULATION", "Marked note #${target.id} PROCESSED: $simulatedSummary")
    }
}
