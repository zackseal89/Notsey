package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiService
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.data.ActivityEventEntity
import com.example.data.AudioEntity
import com.example.data.AppDatabase
import com.example.data.EmbeddingEntity
import com.example.data.NoteEntity
import com.example.data.NoteRelationshipEntity
import com.example.data.NoteRepository
import com.example.data.TagEntity
import com.example.data.TaskEntity
import com.example.server.AgentMcpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.util.Locale

enum class SurfaceTab {
    NOTES,
    RECORD,
    TASKS,
    AGENT_BRIDGE,
    DATABASE_INSPECTOR
}

enum class TypeFilter {
    ALL,
    VOICE,
    TEXT,
    VIDEO
}

enum class StatusFilter {
    ALL,
    PENDING,
    PROCESSED
}

enum class ClassificationFilter(val displayName: String, val code: String?) {
    ALL("All", null),
    IDEA("Ideas", "IDEA"),
    BUG("Bugs", "BUG"),
    TASK("Tasks", "TASK"),
    FEATURE("Features", "FEATURE"),
    MEETING("Meetings", "MEETING"),
    RESEARCH("Research", "RESEARCH")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = NoteRepository(database.noteDao(), database.agentDao())

    val recorder = AudioRecorderManager(application)
    val player = AudioPlayerManager()
    val server = AgentMcpServer(application, repository)

    private val _currentTab = MutableStateFlow(SurfaceTab.NOTES)
    val currentTab: StateFlow<SurfaceTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow(TypeFilter.ALL)
    val typeFilter: StateFlow<TypeFilter> = _typeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    private val _classificationFilter = MutableStateFlow(ClassificationFilter.ALL)
    val classificationFilter: StateFlow<ClassificationFilter> = _classificationFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _aiProcessingStatus = MutableStateFlow<String?>(null)
    val aiProcessingStatus: StateFlow<String?> = _aiProcessingStatus.asStateFlow()

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEvents: StateFlow<List<ActivityEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _tableStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tableStats: StateFlow<Map<String, Int>> = _tableStats.asStateFlow()

    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        repository.allNotes,
        _searchQuery,
        _typeFilter,
        _statusFilter,
        _classificationFilter
    ) { allNotes, query, type, status, classFilter ->
        allNotes.filter { note ->
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    (note.transcript?.contains(query, ignoreCase = true) == true) ||
                    (note.summary?.contains(query, ignoreCase = true) == true) ||
                    note.tags.contains(query, ignoreCase = true)

            val matchesType = when (type) {
                TypeFilter.ALL -> true
                TypeFilter.VOICE -> note.type == "VOICE"
                TypeFilter.TEXT -> note.type == "TEXT"
                TypeFilter.VIDEO -> note.type == "VIDEO"
            }

            val matchesStatus = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> note.agentStatus == "PENDING"
                StatusFilter.PROCESSED -> note.agentStatus == "PROCESSED"
            }

            val matchesClassification = classFilter.code == null || note.classification.equals(classFilter.code, ignoreCase = true)

            matchesQuery && matchesType && matchesStatus && matchesClassification
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-start embedded Agent MCP server so it's ready out of the box
        server.start(8080)
        refreshStats()
        seedSampleDataIfEmpty()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _tableStats.value = repository.getDatabaseTableStats()
        }
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            val count = repository.getAllNotesSync().size
            if (count == 0) {
                // Seed Note 1: Task
                val n1 = NoteEntity(
                    title = "Implement OAuth token caching",
                    content = "When user authenticates, cache the JWT token securely so Codex and Claude Code can reuse it without re-prompting every turn.",
                    transcript = "When user authenticates, cache the JWT token securely so Codex and Claude Code can reuse it without re-prompting every turn.",
                    summary = "Cache JWT credentials locally in Room to avoid repetitive user authentication prompts.",
                    classification = "TASK",
                    type = "VOICE",
                    tags = "security,auth,agent",
                    agentStatus = "PENDING"
                )
                val id1 = repository.insertNote(n1)
                repository.insertTag(TagEntity(noteId = id1, tag = "auth"))
                repository.insertTag(TagEntity(noteId = id1, tag = "security"))
                repository.insertTask(TaskEntity(noteId = id1, title = "Define Room entity for token storage", priority = "HIGH"))
                repository.insertTask(TaskEntity(noteId = id1, title = "Add encryption cipher for SharedPreferences/Room", priority = "MEDIUM"))
                val emb1 = GeminiAiService.generateLocalDeterministicEmbedding("${n1.title} ${n1.content}")
                repository.insertEmbedding(EmbeddingEntity(noteId = id1, embeddingJson = JSONArray(emb1.toList()).toString()))
                repository.recordActivityEvent("NOTE_CREATED", id1, "Seeded note #$id1 (TASK)")

                // Seed Note 2: Idea
                val n2 = NoteEntity(
                    title = "MCP server bi-directional sync",
                    content = "Expose structured MCP tools to Claude Code, Codex, Cursor, and Antigravity for real-time mobile note collaboration.",
                    transcript = null,
                    summary = "Local SQLite Room database exposes MCP JSON-RPC protocol over port 8080 for coding assistants.",
                    classification = "IDEA",
                    type = "TEXT",
                    tags = "mcp,database,claude,antigravity",
                    agentStatus = "PROCESSED",
                    agentName = "Claude Code",
                    agentSummary = "Analyzed schema and verified 9 core tables with MCP JSON-RPC v2.0 protocol."
                )
                val id2 = repository.insertNote(n2)
                repository.insertTag(TagEntity(noteId = id2, tag = "mcp"))
                repository.insertTag(TagEntity(noteId = id2, tag = "database"))
                repository.insertTask(TaskEntity(noteId = id2, title = "Expose /mcp endpoint", isCompleted = true, priority = "HIGH"))
                val emb2 = GeminiAiService.generateLocalDeterministicEmbedding("${n2.title} ${n2.content}")
                repository.insertEmbedding(EmbeddingEntity(noteId = id2, embeddingJson = JSONArray(emb2.toList()).toString()))
                repository.recordActivityEvent("NOTE_CREATED", id2, "Seeded note #$id2 (IDEA)")

                // Seed Note 3: Bug
                val n3 = NoteEntity(
                    title = "Fix audio streaming buffer stall",
                    content = "Audio playback pauses for 500ms on network drop. Need chunked transfer encoding on /api/notes/{id}/audio.",
                    transcript = "Audio playback pauses for 500ms on network drop. Need chunked transfer encoding on /api/notes/{id}/audio.",
                    summary = "Prevent buffer under-runs by providing 8KB chunked streaming in HTTP response header.",
                    classification = "BUG",
                    type = "VOICE",
                    tags = "audio,streaming,bug",
                    agentStatus = "PENDING"
                )
                val id3 = repository.insertNote(n3)
                repository.insertTag(TagEntity(noteId = id3, tag = "audio"))
                repository.insertTag(TagEntity(noteId = id3, tag = "bug"))
                repository.insertTask(TaskEntity(noteId = id3, title = "Adjust socket buffer size to 16KB", priority = "HIGH"))
                val emb3 = GeminiAiService.generateLocalDeterministicEmbedding("${n3.title} ${n3.content}")
                repository.insertEmbedding(EmbeddingEntity(noteId = id3, embeddingJson = JSONArray(emb3.toList()).toString()))
                repository.recordActivityEvent("NOTE_CREATED", id3, "Seeded note #$id3 (BUG)")

                // Link relationship between n1 and n2
                repository.insertRelationship(NoteRelationshipEntity(
                    sourceNoteId = id1,
                    targetNoteId = id2,
                    relationshipType = "RELATED",
                    confidence = 0.78f,
                    explanation = "Both involve agent authentication and server bridge integration"
                ))

                refreshStats()
            }
        }
    }

    fun setTab(tab: SurfaceTab) {
        _currentTab.value = tab
        if (tab == SurfaceTab.DATABASE_INSPECTOR) {
            refreshStats()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: TypeFilter) {
        _typeFilter.value = filter
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun setClassificationFilter(filter: ClassificationFilter) {
        _classificationFilter.value = filter
    }

    fun startVoiceRecording(): Boolean {
        player.stop()
        return recorder.startRecording()
    }

    fun stopVoiceRecordingAndSave(customTitle: String? = null, customTags: String? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            _aiProcessingStatus.value = "Transcribing audio recording..."
            val result = recorder.stopRecording()
            if (result != null) {
                val audioFile = File(result.filePath)

                // 1. Audio Transcription using gemini-3.5-transcribe / gemini-3.5-flash
                val transcript = if (result.transcript.isNotBlank()) {
                    result.transcript
                } else {
                    GeminiAiService.transcribeAudio(audioFile)
                }

                _aiProcessingStatus.value = "Classifying note & extracting actionable tasks..."
                val existingNotes = repository.getAllNotesSync().map { Pair(it.id, it.title) }
                val aiAnalysis = GeminiAiService.analyzeAndClassifyNote(transcript, existingNotes)

                // 2. Determine title and tags
                val finalTitle = customTitle?.ifBlank { aiAnalysis.title } ?: aiAnalysis.title
                val finalTags = customTags?.ifBlank { aiAnalysis.tags.joinToString(",") } ?: aiAnalysis.tags.joinToString(",")

                // 3. Insert NoteEntity
                val note = NoteEntity(
                    title = finalTitle,
                    content = aiAnalysis.summary,
                    transcript = transcript,
                    summary = aiAnalysis.summary,
                    type = "VOICE",
                    classification = aiAnalysis.classification,
                    audioPath = result.filePath,
                    audioDurationMs = result.durationMs,
                    tags = finalTags,
                    duplicateOfNoteId = aiAnalysis.duplicateNoteId,
                    agentStatus = "PENDING"
                )
                val noteId = repository.insertNote(note)

                // 4. Insert AudioEntity into 'audio' table
                repository.insertAudio(
                    AudioEntity(
                        noteId = noteId,
                        filePath = result.filePath,
                        durationMs = result.durationMs,
                        fileSizeBytes = audioFile.length(),
                        mimeType = "audio/mp4",
                        sampleRate = 44100
                    )
                )

                // 5. Insert TagEntities into 'tags' table
                val tagEntities = aiAnalysis.tags.map { TagEntity(noteId = noteId, tag = it) }
                if (tagEntities.isNotEmpty()) {
                    repository.insertTags(tagEntities)
                }

                // 6. Insert TaskEntities into 'tasks' table
                val taskEntities = aiAnalysis.tasks.map {
                    TaskEntity(
                        noteId = noteId,
                        title = it.title,
                        priority = it.priority,
                        dueDate = it.dueDate
                    )
                }
                if (taskEntities.isNotEmpty()) {
                    repository.insertTasks(taskEntities)
                }

                _aiProcessingStatus.value = "Generating semantic embeddings..."
                // 7. Generate & Store Embedding in 'embeddings' table
                val embVector = GeminiAiService.generateEmbedding("$finalTitle $transcript ${aiAnalysis.summary}")
                repository.insertEmbedding(
                    EmbeddingEntity(
                        noteId = noteId,
                        embeddingJson = JSONArray(embVector.toList()).toString(),
                        dimension = embVector.size
                    )
                )

                // 8. Detect Duplicates and Related Notes via Vector Cosine Similarity
                val relatedCandidates = repository.semanticSearch(embVector, topK = 5)
                for ((otherNote, sim) in relatedCandidates) {
                    if (otherNote.id != noteId) {
                        val simScore = String.format(Locale.US, "%.2f", sim)
                        if (sim >= 0.85f) {
                            // High similarity: Mark as duplicate
                            repository.updateDuplicateStatus(noteId, otherNote.id)
                            repository.insertRelationship(
                                NoteRelationshipEntity(
                                    sourceNoteId = noteId,
                                    targetNoteId = otherNote.id,
                                    relationshipType = "DUPLICATE",
                                    confidence = sim,
                                    explanation = "Detected duplicate via semantic similarity ($simScore)"
                                )
                            )
                        } else if (sim >= 0.65f) {
                            // Medium-high similarity: Mark as related
                            repository.insertRelationship(
                                NoteRelationshipEntity(
                                    sourceNoteId = noteId,
                                    targetNoteId = otherNote.id,
                                    relationshipType = "RELATED",
                                    confidence = sim,
                                    explanation = "Related note detected via semantic similarity ($simScore)"
                                )
                            )
                        }
                    }
                }

                // 9. Record Activity Event in 'activity_events' table
                repository.recordActivityEvent(
                    eventType = "VOICE_TRANSCRIBED",
                    noteId = noteId,
                    description = "Transcribed and classified as ${aiAnalysis.classification}: '$finalTitle'"
                )

                server.logEvent("Phone", "NEW_VOICE_NOTE", "Processed note #$noteId [${aiAnalysis.classification}]: $finalTitle")
                refreshStats()
            }
            _aiProcessingStatus.value = null
            _isSaving.value = false
            _currentTab.value = SurfaceTab.NOTES
        }
    }

    fun cancelVoiceRecording() {
        recorder.cancelRecording()
    }

    fun saveTextNote(title: String, content: String, tags: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _aiProcessingStatus.value = "Analyzing note & classifying..."

            val existingNotes = repository.getAllNotesSync().map { Pair(it.id, it.title) }
            val aiAnalysis = GeminiAiService.analyzeAndClassifyNote(content, existingNotes)

            val finalTitle = title.ifBlank { aiAnalysis.title }
            val finalTags = tags.ifBlank { aiAnalysis.tags.joinToString(",") }

            val note = NoteEntity(
                title = finalTitle,
                content = content,
                transcript = null,
                summary = aiAnalysis.summary,
                type = "TEXT",
                classification = aiAnalysis.classification,
                tags = finalTags,
                duplicateOfNoteId = aiAnalysis.duplicateNoteId,
                agentStatus = "PENDING"
            )
            val noteId = repository.insertNote(note)

            // Insert Tags
            val tagEntities = aiAnalysis.tags.map { TagEntity(noteId = noteId, tag = it) }
            if (tagEntities.isNotEmpty()) {
                repository.insertTags(tagEntities)
            }

            // Insert Tasks
            val taskEntities = aiAnalysis.tasks.map {
                TaskEntity(
                    noteId = noteId,
                    title = it.title,
                    priority = it.priority,
                    dueDate = it.dueDate
                )
            }
            if (taskEntities.isNotEmpty()) {
                repository.insertTasks(taskEntities)
            }

            _aiProcessingStatus.value = "Creating semantic embeddings..."
            val embVector = GeminiAiService.generateEmbedding("$finalTitle $content")
            repository.insertEmbedding(
                EmbeddingEntity(
                    noteId = noteId,
                    embeddingJson = JSONArray(embVector.toList()).toString(),
                    dimension = embVector.size
                )
            )

            // Detect duplicates & related notes
            val relatedCandidates = repository.semanticSearch(embVector, topK = 5)
            for ((otherNote, sim) in relatedCandidates) {
                if (otherNote.id != noteId) {
                    val simScore = String.format(Locale.US, "%.2f", sim)
                    if (sim >= 0.85f) {
                        repository.updateDuplicateStatus(noteId, otherNote.id)
                        repository.insertRelationship(
                            NoteRelationshipEntity(
                                sourceNoteId = noteId,
                                targetNoteId = otherNote.id,
                                relationshipType = "DUPLICATE",
                                confidence = sim,
                                explanation = "Detected duplicate via semantic similarity ($simScore)"
                            )
                        )
                    } else if (sim >= 0.65f) {
                        repository.insertRelationship(
                            NoteRelationshipEntity(
                                sourceNoteId = noteId,
                                targetNoteId = otherNote.id,
                                relationshipType = "RELATED",
                                confidence = sim,
                                explanation = "Related note detected via semantic similarity ($simScore)"
                            )
                        )
                    }
                }
            }

            repository.recordActivityEvent(
                eventType = "NOTE_CREATED",
                noteId = noteId,
                description = "Created text note #$noteId [${aiAnalysis.classification}]: $finalTitle"
            )

            server.logEvent("Phone", "NEW_TEXT_NOTE", "Created: $finalTitle (#$noteId)")
            refreshStats()

            _aiProcessingStatus.value = null
            _isSaving.value = false
            _currentTab.value = SurfaceTab.NOTES
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, !task.isCompleted)
            repository.recordActivityEvent(
                eventType = "TASK_UPDATED",
                noteId = task.noteId,
                description = "Task '${task.title}' marked ${if (!task.isCompleted) "completed" else "pending"}"
            )
            refreshStats()
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            if (player.playingNoteId.value == id) {
                player.stop()
            }
            repository.deleteNoteById(id)
            repository.recordActivityEvent("NOTE_DELETED", id, "Deleted note #$id")
            server.logEvent("Phone", "DELETE_NOTE", "Deleted note #$id")
            refreshStats()
        }
    }

    fun playNoteAudio(note: NoteEntity) {
        if (note.audioPath != null) {
            player.playNoteAudio(note.id, note.audioPath)
        }
    }

    fun toggleServer() {
        if (server.isRunning.value) {
            server.stop()
        } else {
            server.start(server.port.value)
        }
    }

    fun simulateAgentQuery(agentName: String) {
        viewModelScope.launch {
            server.simulateAgentRetrieval(agentName)
            refreshStats()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.stop()
        recorder.cancelRecording()
        server.stop()
    }
}
