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
    HOME,
    NOTES,
    AGENTS,
    ACTIVITY,
    RECORD,
    NOTE_DETAIL
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

    private val _currentTab = MutableStateFlow(SurfaceTab.HOME)
    val currentTab: StateFlow<SurfaceTab> = _currentTab.asStateFlow()

    private val _selectedNoteId = MutableStateFlow<Long?>(null)
    val selectedNoteId: StateFlow<Long?> = _selectedNoteId.asStateFlow()

    val selectedNote: StateFlow<NoteEntity?> = combine(
        repository.allNotes,
        _selectedNoteId
    ) { all, id ->
        all.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
            val existing = repository.getAllNotesSync()
            val hasMockupNote = existing.any { it.title.contains("Google login", ignoreCase = true) }
            if (!hasMockupNote) {
                // If there are legacy notes but not the mockup notes, clear and seed mockup data
                if (existing.isNotEmpty()) {
                    for (n in existing) {
                        repository.deleteNoteById(n.id)
                    }
                }

                // 1. Mockup Note 1: Fix Google login flow (Voice Note)
                val n1 = NoteEntity(
                    title = "Fix Google login flow",
                    content = "Users try to login with Google, the app crashes on callback on Android 14. Need to check the intent handling and update the dependencies.",
                    transcript = "When users try to login with Google, the app crashes on callback on Android 14. Need to check the intent handling and update the dependencies.",
                    summary = "Google login crashes on Android 14 during OAuth callback. Likely an intent handling or dependency compatibility issue.",
                    classification = "BUG",
                    type = "VOICE",
                    audioPath = "/mock/audio/google_login_flow.m4a",
                    audioDurationMs = 46000L,
                    tags = "agent,bug,android",
                    agentStatus = "PENDING",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 25 // 25 mins ago (9:32 AM)
                )
                val id1 = repository.insertNote(n1)
                repository.insertTag(TagEntity(noteId = id1, tag = "agent"))
                repository.insertTag(TagEntity(noteId = id1, tag = "bug"))
                repository.insertTag(TagEntity(noteId = id1, tag = "android"))
                repository.insertTask(TaskEntity(noteId = id1, title = "Inspect Android 14 intent filter export", priority = "HIGH"))
                repository.insertTask(TaskEntity(noteId = id1, title = "Update Play Services Auth SDK", priority = "MEDIUM"))
                val emb1 = GeminiAiService.generateLocalDeterministicEmbedding("${n1.title} ${n1.content}")
                repository.insertEmbedding(EmbeddingEntity(noteId = id1, embeddingJson = JSONArray(emb1.toList()).toString()))
                repository.recordActivityEvent("VOICE_TRANSCRIBED", id1, "Transcribed note #$id1: Fix Google login flow")

                // 2. Mockup Note 2: AI code review assistant (Idea Note)
                val n2 = NoteEntity(
                    title = "AI code review assistant",
                    content = "Building an AI agent that reviews PRs and suggests improvements across Kotlin and compose architecture.",
                    transcript = null,
                    summary = "Building an AI agent that reviews PRs and suggests improvements...",
                    classification = "IDEA",
                    type = "TEXT",
                    tags = "idea,ai",
                    agentStatus = "PROCESSED",
                    agentName = "Claude Code",
                    agentSummary = "Analyzed repository pull request workflows and configured GitHub Action triggers.",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 105 // ~1.5h ago (8:15 AM)
                )
                val id2 = repository.insertNote(n2)
                repository.insertTag(TagEntity(noteId = id2, tag = "idea"))
                repository.insertTag(TagEntity(noteId = id2, tag = "ai"))
                repository.insertTask(TaskEntity(noteId = id2, title = "Connect webhook endpoint", isCompleted = true, priority = "HIGH"))
                val emb2 = GeminiAiService.generateLocalDeterministicEmbedding("${n2.title} ${n2.content}")
                repository.insertEmbedding(EmbeddingEntity(noteId = id2, embeddingJson = JSONArray(emb2.toList()).toString()))
                repository.recordActivityEvent("NOTE_CREATED", id2, "Created idea note #$id2")

                // 3. Mockup Note 3: Onboarding copy update (Task Note)
                val n3 = NoteEntity(
                    title = "Onboarding copy update",
                    content = "Update step 2 and 3 microcopy to improve clarity and conversion rate for mobile onboarding.",
                    transcript = null,
                    summary = "Update step 2 and 3 microcopy to improve clarity and conversion.",
                    classification = "TASK",
                    type = "TEXT",
                    tags = "todo,product",
                    agentStatus = "PENDING",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 // Yesterday
                )
                val id3 = repository.insertNote(n3)
                repository.insertTag(TagEntity(noteId = id3, tag = "todo"))
                repository.insertTag(TagEntity(noteId = id3, tag = "product"))
                repository.insertTask(TaskEntity(noteId = id3, title = "Draft new microcopy for Step 2", priority = "MEDIUM"))
                val emb3 = GeminiAiService.generateLocalDeterministicEmbedding("${n3.title} ${n3.content}")
                repository.insertEmbedding(EmbeddingEntity(noteId = id3, embeddingJson = JSONArray(emb3.toList()).toString()))
                repository.recordActivityEvent("NOTE_CREATED", id3, "Created task note #$id3")

                // 4. Related Note: Implement OAuth state handling
                val n4 = NoteEntity(
                    title = "Implement OAuth state handling",
                    content = "Verify redirect URIs and secure PKCE handshake state on Android 14.",
                    classification = "IDEA",
                    type = "TEXT",
                    tags = "auth,security",
                    agentStatus = "PROCESSED",
                    agentName = "Antigravity",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 4 // May 20
                )
                val id4 = repository.insertNote(n4)

                // 5. Related Note: Android 14 intent changes
                val n5 = NoteEntity(
                    title = "Android 14 intent changes",
                    content = "Detailed documentation of intent security policies and exported components.",
                    classification = "BUG",
                    type = "TEXT",
                    tags = "android,security",
                    agentStatus = "PENDING",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 6 // May 18
                )
                val id5 = repository.insertNote(n5)

                // 6. Related Note: Auth flow improvements
                val n6 = NoteEntity(
                    title = "Auth flow improvements",
                    content = "Voice discussion on simplifying authentication lifecycle and session tokens.",
                    classification = "TASK",
                    type = "VOICE",
                    audioDurationMs = 28000L,
                    tags = "auth,flow",
                    agentStatus = "PROCESSED",
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 14 // May 10
                )
                val id6 = repository.insertNote(n6)

                // Insert Relationships between n1 and related notes
                repository.insertRelationship(NoteRelationshipEntity(sourceNoteId = id1, targetNoteId = id4, relationshipType = "RELATED", confidence = 0.88f, explanation = "Both involve OAuth authentication"))
                repository.insertRelationship(NoteRelationshipEntity(sourceNoteId = id1, targetNoteId = id5, relationshipType = "RELATED", confidence = 0.92f, explanation = "Both involve Android 14 intent changes"))
                repository.insertRelationship(NoteRelationshipEntity(sourceNoteId = id1, targetNoteId = id6, relationshipType = "RELATED", confidence = 0.82f, explanation = "Both relate to authentication improvements"))

                refreshStats()
            }
        }
    }

    fun setTab(tab: SurfaceTab) {
        _currentTab.value = tab
        if (tab != SurfaceTab.NOTE_DETAIL) {
            _selectedNoteId.value = null
        }
        if (tab == SurfaceTab.ACTIVITY) {
            refreshStats()
        }
    }

    fun openNoteDetail(noteId: Long) {
        _selectedNoteId.value = noteId
        _currentTab.value = SurfaceTab.NOTE_DETAIL
    }

    fun closeNoteDetail() {
        _selectedNoteId.value = null
        _currentTab.value = SurfaceTab.HOME
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

                val transcript = if (result.transcript.isNotBlank()) {
                    result.transcript
                } else {
                    GeminiAiService.transcribeAudio(audioFile)
                }

                _aiProcessingStatus.value = "Classifying note & extracting actionable tasks..."
                val existingNotes = repository.getAllNotesSync().map { Pair(it.id, it.title) }
                val aiAnalysis = GeminiAiService.analyzeAndClassifyNote(transcript, existingNotes)

                val finalTitle = customTitle?.ifBlank { aiAnalysis.title } ?: aiAnalysis.title
                val finalTags = customTags?.ifBlank { aiAnalysis.tags.joinToString(",") } ?: aiAnalysis.tags.joinToString(",")

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

                val tagEntities = aiAnalysis.tags.map { TagEntity(noteId = noteId, tag = it) }
                if (tagEntities.isNotEmpty()) {
                    repository.insertTags(tagEntities)
                }

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
                val embVector = GeminiAiService.generateEmbedding("$finalTitle $transcript ${aiAnalysis.summary}")
                repository.insertEmbedding(
                    EmbeddingEntity(
                        noteId = noteId,
                        embeddingJson = JSONArray(embVector.toList()).toString(),
                        dimension = embVector.size
                    )
                )

                repository.recordActivityEvent(
                    eventType = "VOICE_TRANSCRIBED",
                    noteId = noteId,
                    description = "Transcribed and classified as ${aiAnalysis.classification}: '$finalTitle'"
                )

                server.logEvent("Phone", "NEW_VOICE_NOTE", "Processed note #$noteId: $finalTitle")
                refreshStats()
            }
            _aiProcessingStatus.value = null
            _isSaving.value = false
            _currentTab.value = SurfaceTab.HOME
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

            val tagEntities = aiAnalysis.tags.map { TagEntity(noteId = noteId, tag = it) }
            if (tagEntities.isNotEmpty()) {
                repository.insertTags(tagEntities)
            }

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

            repository.recordActivityEvent(
                eventType = "NOTE_CREATED",
                noteId = noteId,
                description = "Created text note #$noteId: $finalTitle"
            )

            server.logEvent("Phone", "NEW_TEXT_NOTE", "Created: $finalTitle (#$noteId)")
            refreshStats()

            _aiProcessingStatus.value = null
            _isSaving.value = false
            _currentTab.value = SurfaceTab.HOME
        }
    }

    fun markNoteProcessed(noteId: Long) {
        viewModelScope.launch {
            repository.updateAgentStatus(noteId, "PROCESSED", "Claude Code", "Task executed and resolved successfully.")
            repository.recordActivityEvent("NOTE_PROCESSED", noteId, "Marked note #$noteId as PROCESSED")
            refreshStats()
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
            if (_selectedNoteId.value == id) {
                _selectedNoteId.value = null
                _currentTab.value = SurfaceTab.HOME
            }
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
