package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio",
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
data class AudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val filePath: String,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "audio/mp4",
    val sampleRate: Int = 44100,
    val createdAt: Long = System.currentTimeMillis()
)
