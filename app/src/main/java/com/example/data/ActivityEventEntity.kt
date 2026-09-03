package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_events",
    indices = [Index(value = ["noteId"]), Index(value = ["timestamp"])]
)
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String, // "NOTE_CREATED", "VOICE_TRANSCRIBED", "AI_ENHANCED", "AGENT_RETRIEVAL", "TASK_COMPLETED", "RELATIONSHIP_FOUND"
    val noteId: Long? = null,
    val description: String,
    val payloadJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
