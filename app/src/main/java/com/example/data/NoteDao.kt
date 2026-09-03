package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // ================= NOTES =================
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getAllNotesSync(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND type = 'VOICE' ORDER BY createdAt DESC")
    fun getVoiceNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND type = 'TEXT' ORDER BY createdAt DESC")
    fun getTextNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND classification = :classification ORDER BY createdAt DESC")
    fun getNotesByClassification(classification: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND agentStatus = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND agentStatus = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingNotesSync(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdSync(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE uuid = :uuid LIMIT 1")
    suspend fun getNoteByUuidSync(uuid: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR (transcript IS NOT NULL AND transcript LIKE '%' || :query || '%') OR (summary IS NOT NULL AND summary LIKE '%' || :query || '%') OR tags LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR (transcript IS NOT NULL AND transcript LIKE '%' || :query || '%') OR (summary IS NOT NULL AND summary LIKE '%' || :query || '%') OR tags LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    suspend fun searchNotesSync(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET agentStatus = :status, agentName = :agentName, agentSummary = :summary, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAgentStatus(id: Long, status: String, agentName: String?, summary: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET agentStatus = :status, agentName = :agentName, agentSummary = :summary, updatedAt = :updatedAt WHERE uuid = :uuid")
    suspend fun updateAgentStatusByUuid(uuid: String, status: String, agentName: String?, summary: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET duplicateOfNoteId = :duplicateId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDuplicateStatus(id: Long, duplicateId: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0 AND agentStatus = 'PENDING'")
    suspend fun getPendingCount(): Int

    // ================= AUDIO =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audio: AudioEntity): Long

    @Query("SELECT * FROM audio WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getAudioForNote(noteId: Long): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio WHERE noteId = :noteId ORDER BY createdAt DESC")
    suspend fun getAudioForNoteSync(noteId: Long): List<AudioEntity>

    @Query("SELECT COUNT(*) FROM audio")
    suspend fun getAudioCount(): Int

    // ================= TAGS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("SELECT * FROM tags WHERE noteId = :noteId")
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE noteId = :noteId")
    suspend fun getTagsForNoteSync(noteId: Long): List<TagEntity>

    @Query("DELETE FROM tags WHERE noteId = :noteId")
    suspend fun deleteTagsForNote(noteId: Long)

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int

    // ================= TASKS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getTasksForNote(noteId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE noteId = :noteId ORDER BY createdAt ASC")
    suspend fun getTasksForNoteSync(noteId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    // ================= RELATIONSHIPS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: NoteRelationshipEntity): Long

    @Query("SELECT * FROM note_relationships WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    fun getRelationshipsForNote(noteId: Long): Flow<List<NoteRelationshipEntity>>

    @Query("SELECT * FROM note_relationships WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    suspend fun getRelationshipsForNoteSync(noteId: Long): List<NoteRelationshipEntity>

    @Query("SELECT * FROM note_relationships")
    suspend fun getAllRelationshipsSync(): List<NoteRelationshipEntity>

    @Query("SELECT COUNT(*) FROM note_relationships")
    suspend fun getRelationshipCount(): Int

    // ================= EMBEDDINGS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity): Long

    @Query("SELECT * FROM embeddings WHERE noteId = :noteId LIMIT 1")
    suspend fun getEmbeddingForNote(noteId: Long): EmbeddingEntity?

    @Query("SELECT * FROM embeddings")
    suspend fun getAllEmbeddingsSync(): List<EmbeddingEntity>

    @Query("DELETE FROM embeddings WHERE noteId = :noteId")
    suspend fun deleteEmbeddingForNote(noteId: Long)

    @Query("SELECT COUNT(*) FROM embeddings")
    suspend fun getEmbeddingCount(): Int
}
