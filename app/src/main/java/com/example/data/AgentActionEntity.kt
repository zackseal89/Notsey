package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_actions",
    foreignKeys = [
        ForeignKey(
            entity = AgentRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["runId"]), Index(value = ["noteId"])]
)
data class AgentActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long? = null,
    val noteId: Long? = null,
    val agentName: String,
    val actionType: String, // "TRANSCRIPTION", "CLASSIFICATION", "TASK_EXTRACTION", "CONTEXT_RETRIEVAL", "WRITE_RESULT", "SEARCH", "CREATE_NOTE", "UPDATE_NOTE"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
