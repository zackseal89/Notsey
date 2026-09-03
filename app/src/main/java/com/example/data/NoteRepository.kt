package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import kotlin.math.sqrt

class NoteRepository(
    private val noteDao: NoteDao,
    private val agentDao: AgentDao
) {

    // ================= NOTES =================
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val voiceNotes: Flow<List<NoteEntity>> = noteDao.getVoiceNotes()
    val textNotes: Flow<List<NoteEntity>> = noteDao.getTextNotes()
    val pendingNotes: Flow<List<NoteEntity>> = noteDao.getPendingNotes()

    suspend fun getAllNotesSync(): List<NoteEntity> = noteDao.getAllNotesSync()
    suspend fun getPendingNotesSync(): List<NoteEntity> = noteDao.getPendingNotesSync()
    fun getNotesByClassification(classification: String): Flow<List<NoteEntity>> = noteDao.getNotesByClassification(classification)

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)
    suspend fun getNoteByIdSync(id: Long): NoteEntity? = noteDao.getNoteByIdSync(id)
    suspend fun getNoteByUuidSync(uuid: String): NoteEntity? = noteDao.getNoteByUuidSync(uuid)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun searchNotesSync(query: String): List<NoteEntity> = noteDao.searchNotesSync(query)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)
    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    suspend fun updateAgentStatus(id: Long, status: String, agentName: String?, summary: String?) {
        noteDao.updateAgentStatus(id, status, agentName, summary)
    }

    suspend fun updateAgentStatusByUuid(uuid: String, status: String, agentName: String?, summary: String?) {
        noteDao.updateAgentStatusByUuid(uuid, status, agentName, summary)
    }

    suspend fun updateDuplicateStatus(id: Long, duplicateId: Long?) {
        noteDao.updateDuplicateStatus(id, duplicateId)
    }

    // ================= AUDIO =================
    suspend fun insertAudio(audio: AudioEntity): Long = noteDao.insertAudio(audio)
    fun getAudioForNote(noteId: Long): Flow<List<AudioEntity>> = noteDao.getAudioForNote(noteId)
    suspend fun getAudioForNoteSync(noteId: Long): List<AudioEntity> = noteDao.getAudioForNoteSync(noteId)

    // ================= TAGS =================
    suspend fun insertTag(tag: TagEntity): Long = noteDao.insertTag(tag)
    suspend fun insertTags(tags: List<TagEntity>) = noteDao.insertTags(tags)
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>> = noteDao.getTagsForNote(noteId)
    suspend fun getTagsForNoteSync(noteId: Long): List<TagEntity> = noteDao.getTagsForNoteSync(noteId)

    // ================= TASKS =================
    suspend fun insertTask(task: TaskEntity): Long = noteDao.insertTask(task)
    suspend fun insertTasks(tasks: List<TaskEntity>) = noteDao.insertTasks(tasks)
    fun getTasksForNote(noteId: Long): Flow<List<TaskEntity>> = noteDao.getTasksForNote(noteId)
    suspend fun getTasksForNoteSync(noteId: Long): List<TaskEntity> = noteDao.getTasksForNoteSync(noteId)
    val allTasks: Flow<List<TaskEntity>> = noteDao.getAllTasks()
    suspend fun getAllTasksSync(): List<TaskEntity> = noteDao.getAllTasksSync()
    suspend fun updateTask(task: TaskEntity) = noteDao.updateTask(task)
    suspend fun setTaskCompleted(id: Long, completed: Boolean) = noteDao.setTaskCompleted(id, completed)
    suspend fun deleteTask(id: Long) = noteDao.deleteTask(id)

    // ================= RELATIONSHIPS =================
    suspend fun insertRelationship(relationship: NoteRelationshipEntity): Long = noteDao.insertRelationship(relationship)
    fun getRelationshipsForNote(noteId: Long): Flow<List<NoteRelationshipEntity>> = noteDao.getRelationshipsForNote(noteId)
    suspend fun getRelationshipsForNoteSync(noteId: Long): List<NoteRelationshipEntity> = noteDao.getRelationshipsForNoteSync(noteId)
    suspend fun getAllRelationshipsSync(): List<NoteRelationshipEntity> = noteDao.getAllRelationshipsSync()

    // ================= EMBEDDINGS & SEMANTIC SEARCH =================
    suspend fun insertEmbedding(embedding: EmbeddingEntity): Long = noteDao.insertEmbedding(embedding)
    suspend fun getEmbeddingForNote(noteId: Long): EmbeddingEntity? = noteDao.getEmbeddingForNote(noteId)
    suspend fun getAllEmbeddingsSync(): List<EmbeddingEntity> = noteDao.getAllEmbeddingsSync()

    suspend fun semanticSearch(queryVector: FloatArray, topK: Int = 10): List<Pair<NoteEntity, Float>> {
        val allEmbeddings = noteDao.getAllEmbeddingsSync()
        if (allEmbeddings.isEmpty() || queryVector.isEmpty()) return emptyList()

        val scored = mutableListOf<Pair<Long, Float>>()
        for (emb in allEmbeddings) {
            val vec = parseVector(emb.embeddingJson)
            if (vec != null) {
                val similarity = cosineSimilarity(queryVector, vec)
                scored.add(Pair(emb.noteId, similarity))
            }
        }

        // Sort descending by similarity
        scored.sortByDescending { it.second }

        val results = mutableListOf<Pair<NoteEntity, Float>>()
        for ((noteId, sim) in scored.take(topK)) {
            val note = noteDao.getNoteByIdSync(noteId)
            if (note != null && !note.isArchived) {
                results.add(Pair(note, sim))
            }
        }
        return results
    }

    private fun parseVector(json: String): FloatArray? {
        return try {
            val arr = JSONArray(json)
            val floats = FloatArray(arr.length())
            for (i in 0 until arr.length()) {
                floats[i] = arr.getDouble(i).toFloat()
            }
            floats
        } catch (e: Exception) {
            null
        }
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        val len = minOf(v1.size, v2.size)
        if (len == 0) return 0f
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in 0 until len) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        val denom = sqrt(norm1) * sqrt(norm2)
        return if (denom > 0f) dot / denom else 0f
    }

    // ================= AGENT RUNS & ACTIONS =================
    val allRuns: Flow<List<AgentRunEntity>> = agentDao.getAllRuns()
    val allActions: Flow<List<AgentActionEntity>> = agentDao.getAllActions()
    val allEvents: Flow<List<ActivityEventEntity>> = agentDao.getAllEvents()

    suspend fun startAgentRun(agentName: String, triggerSource: String, targetNoteId: Long?): Long {
        val run = AgentRunEntity(
            agentName = agentName,
            triggerSource = triggerSource,
            targetNoteId = targetNoteId,
            status = "RUNNING"
        )
        return agentDao.insertRun(run)
    }

    suspend fun completeAgentRun(runId: Long, status: String, summary: String?, error: String?) {
        agentDao.completeRun(runId, status, System.currentTimeMillis(), summary, error)
    }

    suspend fun recordAgentAction(runId: Long?, noteId: Long?, agentName: String, actionType: String, details: String): Long {
        val action = AgentActionEntity(
            runId = runId,
            noteId = noteId,
            agentName = agentName,
            actionType = actionType,
            details = details
        )
        return agentDao.insertAction(action)
    }

    suspend fun recordActivityEvent(eventType: String, noteId: Long?, description: String, payloadJson: String? = null): Long {
        val event = ActivityEventEntity(
            eventType = eventType,
            noteId = noteId,
            description = description,
            payloadJson = payloadJson
        )
        return agentDao.insertEvent(event)
    }

    // ================= STATS & COUNTS =================
    suspend fun getDatabaseTableStats(): Map<String, Int> {
        return mapOf(
            "notes" to noteDao.getTotalCount(),
            "pending_notes" to noteDao.getPendingCount(),
            "audio" to noteDao.getAudioCount(),
            "tags" to noteDao.getTagCount(),
            "tasks" to noteDao.getTaskCount(),
            "note_relationships" to noteDao.getRelationshipCount(),
            "embeddings" to noteDao.getEmbeddingCount(),
            "agent_runs" to agentDao.getAgentRunCount(),
            "agent_actions" to agentDao.getAgentActionCount(),
            "activity_events" to agentDao.getActivityEventCount()
        )
    }
}
