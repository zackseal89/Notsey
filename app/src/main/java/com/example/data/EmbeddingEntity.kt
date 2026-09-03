package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "embeddings",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class EmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val embeddingJson: String, // Serialized JSON float array e.g. "[0.123, -0.456, ...]"
    val modelVersion: String = "gemini-embedding-2-preview",
    val dimension: Int = 768,
    val createdAt: Long = System.currentTimeMillis()
)
