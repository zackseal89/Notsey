package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_relationships",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceNoteId"]),
        Index(value = ["targetNoteId"]),
        Index(value = ["sourceNoteId", "targetNoteId"])
    ]
)
data class NoteRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceNoteId: Long,
    val targetNoteId: Long,
    val relationshipType: String = "RELATED", // "DUPLICATE", "RELATED", "BLOCKS", "DEPENDS_ON", "CHILD_OF"
    val confidence: Float = 0.8f,
    val explanation: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
