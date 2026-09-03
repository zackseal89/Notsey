package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.ui.components.getTagColors
import com.example.ui.theme.BadgePendingBg
import com.example.ui.theme.BadgePendingText
import com.example.ui.theme.BadgeProcessedBg
import com.example.ui.theme.BadgeProcessedText
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldMintBg
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.MockupBackground
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
fun NoteDetailScreen(
    note: NoteEntity,
    allNotes: List<NoteEntity> = emptyList(),
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    totalDurationMs: Long = 0L,
    onPlayAudio: () -> Unit = {},
    onBack: () -> Unit,
    onSelectNote: (Long) -> Unit = {},
    onMarkProcessed: (Long) -> Unit = {},
    onCreateTask: (Long) -> Unit = {},
    onSendToAgent: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMoreSummary by remember { mutableStateOf(false) }

    val isProcessed = note.agentStatus.equals("PROCESSED", ignoreCase = true)

    val formattedDate = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    val tagsList = remember(note.tags) {
        val parsed = note.tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
        if (parsed.isNotEmpty()) parsed else listOf("agent", "bug", "android")
    }

    val durationString = remember(note.audioDurationMs) {
        if (note.audioDurationMs > 0) {
            val totalSec = note.audioDurationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            String.format(Locale.US, "%d:%02d", min, sec)
        } else {
            "0:46"
        }
    }

    // Filter related notes (excluding current note)
    val relatedNotes = remember(allNotes, note.id) {
        val candidates = allNotes.filter { it.id != note.id }
        if (candidates.isNotEmpty()) {
            candidates.take(5)
        } else {
            listOf(
                NoteEntity(
                    id = 101,
                    title = "Implement OAuth state handling",
                    content = "OAuth state validation and PKCE",
                    agentStatus = "PROCESSED",
                    classification = "IDEA",
                    createdAt = note.createdAt - 86400000L * 4
                ),
                NoteEntity(
                    id = 102,
                    title = "Android 14 intent changes",
                    content = "Exported intent filter behavior",
                    agentStatus = "PENDING",
                    classification = "BUG",
                    createdAt = note.createdAt - 86400000L * 6
                ),
                NoteEntity(
                    id = 103,
                    title = "Auth flow improvements",
                    content = "Token refresh retries and backoff",
                    agentStatus = "PROCESSED",
                    classification = "TASK",
                    type = "VOICE",
                    createdAt = note.createdAt - 86400000L * 14
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MockupBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Top App Bar (Back Arrow, Edit, More)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Toast.makeText(context, "Edit note", Toast.LENGTH_SHORT).show() }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show() }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // 2. Tags Row (Top)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tagsList.forEach { tag ->
                    val (pillBg, pillText) = getTagColors(tag)
                    Surface(
                        color = pillBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = pillText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Add Tag Button (+)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { Toast.makeText(context, "Add tag", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Tag",
                        tint = TextSecondaryGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Note Title
            Text(
                text = note.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Status & Date Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status Pill
                Surface(
                    color = if (isProcessed) BadgeProcessedBg else BadgePendingBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isProcessed) BadgeProcessedText else BadgePendingText,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isProcessed) "PROCESSED" else "PENDING",
                            color = if (isProcessed) BadgeProcessedText else BadgePendingText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = TextMutedGrey,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Audio Player Card (matching Mockup Image 2)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MockupSurface),
                border = BorderStroke(1.dp, MockupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Play / Pause Circular Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .clickable { onPlayAudio() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Waveform progress visualizer
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.fillMaxWidth().padding(end = 40.dp)
                            ) {
                                val barHeights = listOf(
                                    8, 14, 22, 16, 26, 30, 20, 15, 24, 18, 12, 10,
                                    16, 20, 14, 18, 12, 16, 14, 10, 8, 6, 8, 6, 8, 6, 4
                                )
                                barHeights.forEachIndexed { index, h ->
                                    val isPlayed = index < 10
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isPlayed) EmeraldPrimary else Color(0xFFD1D5DB))
                                    )
                                }
                            }

                            // Duration text on top right
                            Text(
                                text = durationString,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio Info Footer (file size, format, download, share)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "1.2 MB • m4a",
                            fontSize = 12.sp,
                            color = TextMutedGrey,
                            fontWeight = FontWeight.Medium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { Toast.makeText(context, "Downloading audio...", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = TextSecondaryGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Share",
                                    tint = TextSecondaryGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Transcript Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Transcript",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val textToCopy = note.transcript ?: note.content
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "Transcript copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = EmeraldDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MockupSurface),
                border = BorderStroke(1.dp, MockupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                val transcriptText = when {
                    !note.transcript.isNullOrBlank() -> note.transcript
                    !note.content.isNullOrBlank() -> note.content
                    else -> "When users try to login with Google, the app crashes on callback on Android 14. Need to check the intent handling and update the dependencies."
                }
                Text(
                    text = transcriptText,
                    fontSize = 14.sp,
                    color = TextPrimaryDark,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Summary Section (Mint green tint)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = TagGreenBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TagGreenText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI generated",
                                color = TagGreenText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldMintBg),
                border = BorderStroke(1.dp, EmeraldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val summaryText = when {
                        !note.summary.isNullOrBlank() -> note.summary
                        else -> "Google login crashes on Android 14 during OAuth callback. Likely an intent handling or dependency compatibility issue."
                    }

                    Text(
                        text = summaryText,
                        fontSize = 14.sp,
                        color = TextPrimaryDark,
                        lineHeight = 22.sp,
                        maxLines = if (showMoreSummary) 20 else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMoreSummary = !showMoreSummary },
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showMoreSummary) "Show less" else "Show more",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (showMoreSummary) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Agent Response Section (Lavender tint)
            Text(
                text = "Agent Response",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEDE9FE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Agent",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        if (isProcessed) {
                            Text(
                                text = "${note.agentName ?: "Claude Code"} processed this note",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.agentSummary ?: "Analyzed authentication intent filters in AndroidManifest.xml and verified library dependencies.",
                                fontSize = 13.sp,
                                color = TextSecondaryGrey,
                                lineHeight = 18.sp
                            )
                        } else {
                            Text(
                                text = "Waiting for agent to process this note...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Once an agent processes it, the action plan and results will appear here.",
                                fontSize = 13.sp,
                                color = TextSecondaryGrey,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 9. Related Notes Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Related Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = "See all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "All related notes", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Carousel of Related Notes
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(relatedNotes) { relatedNote ->
                    val relDateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(relatedNote.createdAt))
                    val isRelProcessed = relatedNote.agentStatus.equals("PROCESSED", ignoreCase = true)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MockupSurface),
                        border = BorderStroke(1.dp, MockupCardBorder),
                        modifier = Modifier
                            .width(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectNote(relatedNote.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Icon + Date
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (relIcon, relTint) = when {
                                    relatedNote.classification.equals("BUG", ignoreCase = true) -> Pair(Icons.Default.PestControl, TagRedText)
                                    relatedNote.type == "VOICE" -> Pair(Icons.Default.GraphicEq, TagPurpleText)
                                    else -> Pair(Icons.Default.Lightbulb, TagAmberText)
                                }
                                Icon(
                                    imageVector = relIcon,
                                    contentDescription = null,
                                    tint = relTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = relDateStr,
                                    fontSize = 10.sp,
                                    color = TextMutedGrey,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = relatedNote.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = if (isRelProcessed) BadgeProcessedBg else BadgePendingBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isRelProcessed) "PROCESSED" else "PENDING",
                                    color = if (isRelProcessed) BadgeProcessedText else BadgePendingText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 10. Actions Section
            Text(
                text = "Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row (Mockup 2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // 1. Mark as Processed
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MockupSurface,
                    border = BorderStroke(1.dp, MockupCardBorder),
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMarkProcessed(note.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mark as Processed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark,
                            maxLines = 1
                        )
                    }
                }

                // 2. Create Task
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MockupSurface,
                    border = BorderStroke(1.dp, MockupCardBorder),
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCreateTask(note.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Create Task",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            maxLines = 1
                        )
                    }
                }

                // 3. Send to Agent
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MockupSurface,
                    border = BorderStroke(1.dp, MockupCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSendToAgent(note.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Send to Agent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            maxLines = 1
                        )
                    }
                }

                // 4. More (•••)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MockupSurface,
                    border = BorderStroke(1.dp, MockupCardBorder),
                    modifier = Modifier
                        .weight(0.6f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { Toast.makeText(context, "More actions", Toast.LENGTH_SHORT).show() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "More",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}
