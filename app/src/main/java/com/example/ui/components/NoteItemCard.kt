package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.theme.BadgePendingBg
import com.example.ui.theme.BadgePendingText
import com.example.ui.theme.BadgeProcessedBg
import com.example.ui.theme.BadgeProcessedText
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MockupCardBorder
import com.example.ui.theme.MockupSurface
import com.example.ui.theme.TagAmberBg
import com.example.ui.theme.TagAmberText
import com.example.ui.theme.TagBlueBg
import com.example.ui.theme.TagBlueText
import com.example.ui.theme.TagGreenBg
import com.example.ui.theme.TagGreenText
import com.example.ui.theme.TagPurpleBg
import com.example.ui.theme.TagPurpleText
import com.example.ui.theme.TagRedBg
import com.example.ui.theme.TagRedText
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryGrey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteItemCard(
    note: NoteEntity,
    tasks: List<TaskEntity> = emptyList(),
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    totalDurationMs: Long = 0L,
    onPlayAudio: () -> Unit = {},
    onNoteClick: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onToggleTask: ((TaskEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAudio = note.type == "VOICE" || note.audioPath != null
    val isVideo = note.type == "VIDEO"
    val isIdea = note.classification.equals("IDEA", ignoreCase = true)

    // Formatted time / date string
    val formattedTime = remember(note.createdAt) {
        val noteDate = Date(note.createdAt)
        val now = Date()
        val diffDays = (now.time - noteDate.time) / (1000 * 60 * 60 * 24)
        when {
            diffDays == 0L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(noteDate)
            diffDays == 1L -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(noteDate)
        }
    }

    // Audio duration formatted e.g. "0:46"
    val durationText = remember(note.audioDurationMs) {
        if (note.audioDurationMs > 0) {
            val totalSec = note.audioDurationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            String.format(Locale.US, "%d:%02d", min, sec)
        } else {
            "0:46" // Default fallback duration matching mockup
        }
    }

    val isProcessed = note.agentStatus.equals("PROCESSED", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MockupSurface),
        border = BorderStroke(1.dp, MockupCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onNoteClick() }
            .testTag("note_item_${note.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Category/Type Icon Box + Optional Duration underneath
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                val (boxBg, iconColor, icon) = when {
                    isAudio -> Triple(TagPurpleBg, TagPurpleText, Icons.Default.GraphicEq)
                    isVideo -> Triple(TagPurpleBg, TagPurpleText, Icons.Default.Videocam)
                    isIdea -> Triple(TagAmberBg, TagAmberText, Icons.Default.Lightbulb)
                    else -> Triple(TagBlueBg, TagBlueText, Icons.Default.TextFields)
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(boxBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isAudio) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = durationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMutedGrey
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center Column: Title, Preview snippet, Tag pills
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = note.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val previewText = when {
                    !note.content.isNullOrBlank() -> note.content
                    !note.transcript.isNullOrBlank() -> note.transcript
                    !note.summary.isNullOrBlank() -> note.summary
                    else -> "No content"
                }

                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    color = TextSecondaryGrey,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tag Pills Row
                val rawTags = note.tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                val displayTags = if (rawTags.isNotEmpty()) rawTags else listOf("agent", "bug")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    displayTags.take(3).forEach { tag ->
                        val (pillBg, pillText) = getTagColors(tag)
                        Surface(
                            color = pillBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = pillText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Column: Timestamp, Status Pill (PENDING/PROCESSED), Action Button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(84.dp)
            ) {
                // Timestamp
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = TextMutedGrey,
                    fontWeight = FontWeight.Medium
                )

                // Status Badge Pill
                Surface(
                    color = if (isProcessed) BadgeProcessedBg else BadgePendingBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isProcessed) "PROCESSED" else "PENDING",
                        color = if (isProcessed) BadgeProcessedText else BadgePendingText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Action Circle Button (Play or Checkmark)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF9FAFB))
                        .clickable {
                            if (isAudio) {
                                onPlayAudio()
                            } else {
                                onNoteClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Processed",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper to determine tag pill background and text colors matching mockups
fun getTagColors(tag: String): Pair<Color, Color> {
    return when (tag.lowercase()) {
        "bug" -> Pair(TagRedBg, TagRedText)
        "android" -> Pair(TagBlueBg, TagBlueText)
        "todo" -> Pair(TagBlueBg, TagBlueText)
        "product" -> Pair(TagPurpleBg, TagPurpleText)
        "idea" -> Pair(TagAmberBg, TagAmberText)
        "ai" -> Pair(TagGreenBg, TagGreenText)
        "agent" -> Pair(TagPurpleBg, TagPurpleText)
        else -> Pair(TagPurpleBg, TagPurpleText)
    }
}
