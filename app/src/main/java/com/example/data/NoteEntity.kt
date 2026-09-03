package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val transcript: String? = null, // Voice note speech-to-text transcript stored locally on device
    val type: String = "VOICE", // "VOICE", "TEXT", "VIDEO"
    val classification: String = "IDEA", // "IDEA", "BUG", "TASK", "FEATURE", "MEETING", "RESEARCH", "MISC"
    val summary: String? = null, // AI-generated concise summary
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val videoUri: String? = null,
    val tags: String = "", // Comma-separated e.g. "feature,agent,prompt"
    val duplicateOfNoteId: Long? = null, // Note ID if detected as duplicate
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val agentStatus: String = "PENDING", // "PENDING", "RETRIEVED", "PROCESSED"
    val agentName: String? = null, // e.g. "Claude Code", "Antigravity", "Codex", "Cursor"
    val agentSummary: String? = null, // Action items, code changes, or summary from the agent
    val isArchived: Boolean = false
)
