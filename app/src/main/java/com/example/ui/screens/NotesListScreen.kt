package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.ClassificationFilter
import com.example.ui.StatusFilter
import com.example.ui.TypeFilter
import com.example.ui.components.NoteItemCard
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldMintBg
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MockupBackground
import com.example.ui.theme.MockupCardBorder
import com.example.ui.theme.MockupSurface
import com.example.ui.theme.TagGreenBg
import com.example.ui.theme.TagGreenText
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryGrey

@Composable
fun NotesListScreen(
    notes: List<NoteEntity>,
    tasks: List<TaskEntity> = emptyList(),
    searchQuery: String = "",
    typeFilter: TypeFilter = TypeFilter.ALL,
    statusFilter: StatusFilter = StatusFilter.ALL,
    classificationFilter: ClassificationFilter = ClassificationFilter.ALL,
    aiProcessingStatus: String? = null,
    isServerRunning: Boolean = true,
    serverPort: Int = 8080,
    playingNoteId: Long? = null,
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    totalDurationMs: Long = 0L,
    onSearchChange: (String) -> Unit = {},
    onTypeFilterChange: (TypeFilter) -> Unit = {},
    onStatusFilterChange: (StatusFilter) -> Unit = {},
    onClassificationFilterChange: (ClassificationFilter) -> Unit = {},
    onPlayAudio: (NoteEntity) -> Unit = {},
    onDeleteNote: (Long) -> Unit = {},
    onToggleTask: (TaskEntity) -> Unit = {},
    onNavigateToCapture: () -> Unit = {},
    onNavigateToAgentBridge: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToDatabaseInspector: () -> Unit = {},
    onSelectNote: (Long) -> Unit = {},
    onSaveTextNote: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQuickTextDialog by remember { mutableStateOf(false) }
    var quickDialogType by remember { mutableStateOf("TEXT") } // "TEXT", "VIDEO", "IDEA", "IMPORT"
    var quickTitle by remember { mutableStateOf("") }
    var quickContent by remember { mutableStateOf("") }

    val pendingCount = notes.count { it.agentStatus.equals("PENDING", ignoreCase = true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MockupBackground),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Top Bar: Robot Logo + "Agent Notes" + Notification Bell with green dot
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
            ) {
                // Logo & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(8.dp))
                            .background(EmeraldMintBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Agent Notes Logo",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Agent Notes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                // Notification Bell with green dot
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "$pendingCount notes awaiting agent attention", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Green Notification Indicator Dot
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                }
            }
        }

        // 2. Greeting Section: "Good morning, Zack 👋" + "3 notes need your attention"
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Good morning, Zack 👋",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$pendingCount notes need your attention",
                    fontSize = 14.sp,
                    color = TextSecondaryGrey,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // AI Processing Banner if active
        if (aiProcessingStatus != null) {
            item {
                Surface(
                    color = TagGreenBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldDark,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = aiProcessingStatus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TagGreenText
                        )
                    }
                }
            }
        }

        // 3. Hero Recording Card (Mockup Image 1)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldMintBg),
                border = BorderStroke(1.dp, EmeraldBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onNavigateToCapture() }
                    .testTag("hero_record_card")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp)
                ) {
                    // Audio Waveform & Mic Button Centerpiece
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        // Background soft waveform bars
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            val waveHeights = listOf(
                                8, 14, 20, 26, 36, 44, 30, 20, 14, 18, 28, 40,
                                52, 40, 24, 16, 20, 32, 46, 34, 22, 14, 20, 10
                            )
                            waveHeights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(EmeraldBorder.copy(alpha = 0.8f))
                                )
                            }
                        }

                        // Central Glow Ring + Emerald Mic Button
                        Box(contentAlignment = Alignment.Center) {
                            // Glow halo
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGlow)
                            )

                            // Main Mic Button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tap to record",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Hold to capture your thoughts",
                        fontSize = 13.sp,
                        color = TextSecondaryGrey
                    )
                }
            }
        }

        // 4. Quick Action Row (4 Cards)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.TextFields,
                    label = "Text Note",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        quickDialogType = "TEXT"
                        quickTitle = ""
                        quickContent = ""
                        showQuickTextDialog = true
                    }
                )

                QuickActionCard(
                    icon = Icons.Default.Videocam,
                    label = "Video Note",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        quickDialogType = "VIDEO"
                        quickTitle = "Video Memo"
                        quickContent = ""
                        showQuickTextDialog = true
                    }
                )

                QuickActionCard(
                    icon = Icons.Default.Lightbulb,
                    label = "Quick Idea",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        quickDialogType = "IDEA"
                        quickTitle = "New Idea"
                        quickContent = ""
                        showQuickTextDialog = true
                    }
                )

                QuickActionCard(
                    icon = Icons.Default.FileUpload,
                    label = "Import",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        Toast.makeText(context, "Import audio file or text notes", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 5. Recent Notes Section Header: "Recent Notes" + "See all"
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Recent Notes",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = "See all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark,
                    modifier = Modifier
                        .clickable {
                            Toast.makeText(context, "All ${notes.size} notes", Toast.LENGTH_SHORT).show()
                        }
                        .padding(4.dp)
                )
            }
        }

        // 6. Recent Notes Cards List
        if (notes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes recorded yet. Tap the green microphone above to create your first note!",
                        color = TextSecondaryGrey,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(notes, key = { it.id }) { note ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    NoteItemCard(
                        note = note,
                        tasks = tasks,
                        isPlaying = isPlaying && playingNoteId == note.id,
                        currentPositionMs = if (playingNoteId == note.id) currentPositionMs else 0L,
                        totalDurationMs = if (playingNoteId == note.id) totalDurationMs else note.audioDurationMs,
                        onPlayAudio = { onPlayAudio(note) },
                        onNoteClick = { onSelectNote(note.id) },
                        onDelete = { onDeleteNote(note.id) },
                        onToggleTask = onToggleTask
                    )
                }
            }
        }
    }

    // Quick Note Dialog for Quick Action cards
    if (showQuickTextDialog) {
        Dialog(onDismissRequest = { showQuickTextDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MockupSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = when (quickDialogType) {
                            "VIDEO" -> "New Video Memo"
                            "IDEA" -> "New Quick Idea"
                            else -> "New Text Note"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = quickTitle,
                        onValueChange = { quickTitle = it },
                        placeholder = { Text("Note Title", color = TextMutedGrey) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MockupCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quickContent,
                        onValueChange = { quickContent = it },
                        placeholder = { Text("Write your thoughts or requirements...", color = TextMutedGrey) },
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MockupCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showQuickTextDialog = false }) {
                            Text("Cancel", color = TextSecondaryGrey)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (quickContent.isNotBlank()) {
                                    val tag = when (quickDialogType) {
                                        "VIDEO" -> "video,agent"
                                        "IDEA" -> "idea,ai"
                                        else -> "todo,product"
                                    }
                                    onSaveTextNote(quickTitle.trim(), quickContent.trim(), tag)
                                    showQuickTextDialog = false
                                }
                            },
                            enabled = quickContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Note", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MockupSurface),
        border = BorderStroke(1.dp, MockupCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimaryDark,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryGrey,
                maxLines = 1
            )
        }
    }
}
