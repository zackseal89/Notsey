package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.theme.PolishBluePrimary
import com.example.ui.theme.PolishBorder
import com.example.ui.theme.PolishPending
import com.example.ui.theme.PolishPendingContainer
import com.example.ui.theme.PolishPurpleContainer
import com.example.ui.theme.PolishPurplePill
import com.example.ui.theme.PolishPurplePrimary
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.PolishVoiceTagBg
import com.example.ui.theme.PolishVoiceTagText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AudioPlayerBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val formatTime: (Long) -> String = { ms ->
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%02d:%02d", min, sec)
    }

    Surface(
        color = PolishPurplePill.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(34.dp)
                    .background(PolishPurplePrimary, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = PolishPurplePrimary,
                    trackColor = PolishBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        fontSize = 10.sp,
                        color = PolishTextMuted
                    )
                    Text(
                        text = formatTime(totalDurationMs),
                        fontSize = 10.sp,
                        color = PolishTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun NoteItemCard(
    note: NoteEntity,
    tasks: List<TaskEntity> = emptyList(),
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onPlayAudio: () -> Unit,
    onDelete: () -> Unit,
    onToggleTask: ((TaskEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val formattedDate = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurface),
        border = BorderStroke(1.dp, PolishBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .testTag("note_item_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Type badge, Classification pill, Date & Agent Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type Icon + Classification + Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (typeIcon, typeBg, typeTint) = when (note.type) {
                        "VOICE" -> Triple(Icons.Default.Mic, PolishVoiceTagBg, PolishVoiceTagText)
                        "VIDEO" -> Triple(Icons.Default.Videocam, PolishPurpleContainer, PolishPurplePrimary)
                        else -> Triple(Icons.Default.Description, Color(0xFFE8DEF8), Color(0xFF4A4458))
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(typeBg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = note.type,
                            tint = typeTint,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Classification Pill
                    val (classColor, classBg) = when (note.classification) {
                        "BUG" -> Pair(Color(0xFFB3261E), Color(0xFFF9DEDC))
                        "TASK" -> Pair(Color(0xFF1E6B37), Color(0xFFD4EBD8))
                        "IDEA" -> Pair(Color(0xFF6750A4), Color(0xFFEADDFF))
                        "FEATURE" -> Pair(Color(0xFF00639B), Color(0xFFD3E4FF))
                        "MEETING" -> Pair(Color(0xFF7D5260), Color(0xFFFFD8E4))
                        "RESEARCH" -> Pair(Color(0xFF5B5B7E), Color(0xFFE1E0F9))
                        else -> Pair(PolishTextSecondary, PolishSurfaceVariant)
                    }

                    Surface(
                        color = classBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = note.classification,
                            color = classColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = PolishTextMuted
                    )
                }

                // Agent status chip
                val isProcessed = note.agentStatus == "PROCESSED"
                Surface(
                    color = if (isProcessed) PolishSuccessContainer else PolishPendingContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isProcessed) PolishSuccess else PolishPending, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isProcessed) (note.agentName ?: "Ready") else "Pending Agent",
                            color = if (isProcessed) PolishSuccess else PolishPending,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Duplicate Note Banner if flagged
            if (note.duplicateOfNoteId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFE69C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detected duplicate of Note #${note.duplicateOfNoteId}",
                            color = Color(0xFF856404),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishTextPrimary,
                maxLines = if (expanded) 10 else 2,
                overflow = TextOverflow.Ellipsis
            )

            // AI-generated Executive Summary
            if (!note.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.summary,
                    fontSize = 13.sp,
                    color = PolishTextSecondary,
                    maxLines = if (expanded) 20 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            } else if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontSize = 13.sp,
                    color = PolishTextSecondary,
                    maxLines = if (expanded) 20 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // Local Voice Transcript on Device
            if (note.type == "VOICE" && !note.transcript.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PolishVoiceTagBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PolishBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Transcript",
                                tint = PolishVoiceTagText,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VOICE TRANSCRIPT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = PolishVoiceTagText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.transcript,
                            fontSize = 12.sp,
                            color = PolishTextPrimary,
                            lineHeight = 17.sp,
                            maxLines = if (expanded) 15 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Audio Player Bar if VOICE note
            if (note.type == "VOICE" && note.audioPath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                AudioPlayerBar(
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    totalDurationMs = if (isPlaying && totalDurationMs > 0) totalDurationMs else note.audioDurationMs,
                    onTogglePlay = onPlayAudio
                )
            }

            // Actionable Tasks Section (if tasks exist)
            val noteTasks = tasks.filter { it.noteId == note.id }
            if (noteTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PolishSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PolishBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Tasks",
                                    tint = PolishBluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACTIONABLE TASKS (${noteTasks.count { it.isCompleted }}/${noteTasks.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = PolishBluePrimary
                                )
                            }
                        }

                        val displayedTasks = if (expanded) noteTasks else noteTasks.take(2)
                        for (task in displayedTasks) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTask?.invoke(task) }
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (task.isCompleted) "Completed" else "Pending",
                                    tint = if (task.isCompleted) PolishSuccess else PolishTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = task.title,
                                    fontSize = 12.sp,
                                    color = if (task.isCompleted) PolishTextMuted else PolishTextPrimary,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                                if (task.priority == "HIGH") {
                                    Text(
                                        text = "HIGH",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB3261E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Agent Summary Section if PROCESSED
            if (note.agentSummary != null && note.agentSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = PolishSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Agent response",
                            tint = PolishBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${note.agentName ?: "Agent"} Insight:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishBluePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = note.agentSummary,
                                fontSize = 12.sp,
                                color = PolishTextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Tags and Actions footer
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val tagList = note.tags.split(",").filter { it.isNotBlank() }
                    for (tag in tagList.take(3)) {
                        Surface(
                            color = PolishPurplePill,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = PolishPurplePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Expand and Delete buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = PolishTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = PolishTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
