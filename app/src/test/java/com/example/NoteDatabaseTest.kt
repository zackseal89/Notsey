package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ActivityEventEntity
import com.example.data.AgentActionEntity
import com.example.data.AgentDao
import com.example.data.AgentRunEntity
import com.example.data.AppDatabase
import com.example.data.AudioEntity
import com.example.data.EmbeddingEntity
import com.example.data.NoteDao
import com.example.data.NoteEntity
import com.example.data.NoteRelationshipEntity
import com.example.data.TagEntity
import com.example.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoteDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var agentDao: AgentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
        agentDao = database.agentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve text note locally`() = runBlocking {
        val textNote = NoteEntity(
            title = "Architecture Plan",
            content = "Design the clean room database architecture with NoteEntity and NoteDao.",
            transcript = null,
            classification = "TASK",
            type = "TEXT",
            tags = "architecture,design",
            agentStatus = "PENDING"
        )

        val id = noteDao.insertNote(textNote)
        assertTrue(id > 0)

        val retrieved = noteDao.getNoteByIdSync(id)
        assertNotNull(retrieved)
        assertEquals("Architecture Plan", retrieved?.title)
        assertEquals("Design the clean room database architecture with NoteEntity and NoteDao.", retrieved?.content)
        assertNull(retrieved?.transcript)
        assertEquals("TASK", retrieved?.classification)
        assertEquals("TEXT", retrieved?.type)
        assertEquals("PENDING", retrieved?.agentStatus)
    }

    @Test
    fun `insert and retrieve voice note with transcript locally`() = runBlocking {
        val voiceNote = NoteEntity(
            title = "Voice Memo: Sync logic",
            content = "Summary of sync logic discussion.",
            transcript = "We should allow Claude Code and Antigravity to poll the Room database via the local MCP bridge.",
            classification = "IDEA",
            type = "VOICE",
            audioPath = "/data/user/0/com.example/files/recording_1.m4a",
            audioDurationMs = 14200L,
            tags = "voice,mcp,agent",
            agentStatus = "PENDING"
        )

        val id = noteDao.insertNote(voiceNote)
        assertTrue(id > 0)

        val retrieved = noteDao.getNoteByIdSync(id)
        assertNotNull(retrieved)
        assertEquals("Voice Memo: Sync logic", retrieved?.title)
        assertEquals("We should allow Claude Code and Antigravity to poll the Room database via the local MCP bridge.", retrieved?.transcript)
        assertEquals("IDEA", retrieved?.classification)
        assertEquals("VOICE", retrieved?.type)
        assertEquals(14200L, retrieved?.audioDurationMs)
        assertEquals("/data/user/0/com.example/files/recording_1.m4a", retrieved?.audioPath)
    }

    @Test
    fun `verify audio, tags, tasks, relationships and embeddings tables`() = runBlocking {
        val noteId = noteDao.insertNote(
            NoteEntity(
                title = "Cache token fix",
                content = "Need to secure token in Room",
                classification = "BUG",
                type = "VOICE"
            )
        )

        // Audio table
        noteDao.insertAudio(
            AudioEntity(
                noteId = noteId,
                filePath = "/path/to/token_fix.m4a",
                durationMs = 8500L,
                fileSizeBytes = 24000L
            )
        )
        val audios = noteDao.getAudioForNoteSync(noteId)
        assertTrue(audios.isNotEmpty())
        assertEquals("/path/to/token_fix.m4a", audios[0].filePath)

        // Tags table
        noteDao.insertTags(listOf(TagEntity(noteId = noteId, tag = "security"), TagEntity(noteId = noteId, tag = "bug")))
        val tags = noteDao.getTagsForNoteSync(noteId)
        assertEquals(2, tags.size)

        // Tasks table
        noteDao.insertTasks(
            listOf(
                TaskEntity(noteId = noteId, title = "Write migration script", priority = "HIGH"),
                TaskEntity(noteId = noteId, title = "Update MCP endpoint", priority = "MEDIUM")
            )
        )
        val tasks = noteDao.getTasksForNoteSync(noteId)
        assertEquals(2, tasks.size)

        // Toggle task
        val task1 = tasks[0]
        noteDao.setTaskCompleted(task1.id, true)
        val updatedTasks = noteDao.getTasksForNoteSync(noteId)
        assertTrue(updatedTasks.first { it.id == task1.id }.isCompleted)

        // Embeddings table
        noteDao.insertEmbedding(
            EmbeddingEntity(
                noteId = noteId,
                embeddingJson = "[0.12, 0.45, -0.22]",
                dimension = 3
            )
        )
        val embedding = noteDao.getEmbeddingForNote(noteId)
        assertNotNull(embedding)
        assertEquals(3, embedding?.dimension)

        // Note relationships
        val otherNoteId = noteDao.insertNote(NoteEntity(title = "Another note", content = "Content"))
        noteDao.insertRelationship(
            NoteRelationshipEntity(
                sourceNoteId = noteId,
                targetNoteId = otherNoteId,
                relationshipType = "RELATED",
                confidence = 0.82f,
                explanation = "Similar security topic"
            )
        )
        val relations = noteDao.getRelationshipsForNoteSync(noteId)
        assertEquals(1, relations.size)
        assertEquals("RELATED", relations[0].relationshipType)

        // Agent Runs and Actions
        val runId = agentDao.insertRun(
            AgentRunEntity(
                agentName = "Claude Code",
                triggerSource = "MCP_TOOL",
                status = "COMPLETED"
            )
        )
        assertTrue(runId > 0)

        agentDao.insertAction(
            AgentActionEntity(
                runId = runId,
                agentName = "Claude Code",
                actionType = "LIST_NOTES",
                details = "{\"limit\": 10}"
            )
        )
        val actions = agentDao.getRecentActionsSync(10)
        assertEquals(1, actions.size)
        assertEquals("LIST_NOTES", actions[0].actionType)

        // Activity event
        agentDao.insertEvent(
            ActivityEventEntity(
                eventType = "TEST_EVENT",
                noteId = noteId,
                description = "Automated test event"
            )
        )
        val events = agentDao.getRecentEventsSync(10)
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun `search notes by speech transcript`() = runBlocking {
        noteDao.insertNote(
            NoteEntity(
                title = "Meeting 1",
                content = "Discussion notes",
                transcript = "Make sure the database uses SQLite with Room persistence",
                type = "VOICE"
            )
        )
        noteDao.insertNote(
            NoteEntity(
                title = "Meeting 2",
                content = "UI notes",
                transcript = "Fix the card padding on the homescreen",
                type = "VOICE"
            )
        )

        val searchResults = noteDao.searchNotesSync("Room persistence")
        assertEquals(1, searchResults.size)
        assertEquals("Meeting 1", searchResults[0].title)
    }

    @Test
    fun `filter voice notes and text notes`() = runBlocking {
        noteDao.insertNote(
            NoteEntity(
                title = "Text Note 1",
                content = "Plain text content",
                type = "TEXT"
            )
        )
        noteDao.insertNote(
            NoteEntity(
                title = "Voice Note 1",
                content = "Voice content",
                transcript = "Recorded voice audio transcript",
                type = "VOICE"
            )
        )

        val voiceNotes = noteDao.getVoiceNotes().first()
        val textNotes = noteDao.getTextNotes().first()

        assertEquals(1, voiceNotes.size)
        assertEquals("Voice Note 1", voiceNotes[0].title)
        assertEquals("Recorded voice audio transcript", voiceNotes[0].transcript)

        assertEquals(1, textNotes.size)
        assertEquals("Text Note 1", textNotes[0].title)
    }

    @Test
    fun `update agent status on note`() = runBlocking {
        val id = noteDao.insertNote(
            NoteEntity(
                title = "Pending Task",
                content = "Awaiting agent execution",
                transcript = "Check database query performance",
                type = "VOICE",
                agentStatus = "PENDING"
            )
        )

        noteDao.updateAgentStatus(id, "PROCESSED", "Claude Code", "Optimized index and verified queries.")

        val updated = noteDao.getNoteByIdSync(id)
        assertEquals("PROCESSED", updated?.agentStatus)
        assertEquals("Claude Code", updated?.agentName)
        assertEquals("Optimized index and verified queries.", updated?.agentSummary)
    }

    @Test
    fun `delete note from database`() = runBlocking {
        val id = noteDao.insertNote(
            NoteEntity(
                title = "Temporary Note",
                content = "Will be deleted",
                type = "TEXT"
            )
        )

        assertEquals(1, noteDao.getTotalCount())
        noteDao.deleteNoteById(id)
        assertEquals(0, noteDao.getTotalCount())
    }
}
