package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.ClassificationFilter
import com.example.ui.StatusFilter
import com.example.ui.TypeFilter
import com.example.ui.components.NoteItemCard
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishBlueContainer
import com.example.ui.theme.PolishBlueOnContainer
import com.example.ui.theme.PolishBluePrimary
import com.example.ui.theme.PolishBorder
import com.example.ui.theme.PolishPending
import com.example.ui.theme.PolishPurpleContainer
import com.example.ui.theme.PolishPurplePrimary
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

@Composable
fun NotesListScreen(
    notes: List<NoteEntity>,
    tasks: List<TaskEntity> = emptyList(),
    searchQuery: String,
    typeFilter: TypeFilter,
    statusFilter: StatusFilter,
    classificationFilter: ClassificationFilter,
    aiProcessingStatus: String?,
    isServerRunning: Boolean,
    serverPort: Int,
    playingNoteId: Long?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TypeFilter) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onClassificationFilterChange: (ClassificationFilter) -> Unit,
    onPlayAudio: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToAgentBridge: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToDatabaseInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Title, DB quick button, Tasks quick button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = "Agent Notes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        color = PolishTextPrimary
                    )
                    Text(
                        text = "AI Transcribed ⇄ Local Room ⇄ MCP Bridge",
                        fontSize = 12.sp,
                        color = PolishTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quick button: Actionable Tasks
                    IconButton(
                        onClick = onNavigateToTasks,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PolishBlueContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Tasks",
                            tint = PolishBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick button: Database Inspector
                    IconButton(
                        onClick = onNavigateToDatabaseInspector,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PolishPurpleContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Database Inspector",
                            tint = PolishPurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // AI Processing Banner (visible during transcription / classification)
            AnimatedVisibility(visible = aiProcessingStatus != null) {
                Surface(
                    color = PolishPurpleContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishPurplePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PolishPurplePrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PolishPurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = aiProcessingStatus ?: "AI processing...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishPurplePrimary
                        )
                    }
                }
            }

            // Syncing / Live Status Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
            ) {
                Surface(
                    color = PolishBlueContainer,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAgentBridge() }
                        .testTag("server_status_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isServerRunning) PolishBluePrimary else PolishPending,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isServerRunning) "Syncing with Claude Code / Codex (Port $serverPort)" else "Local Agent Inactive (Tap to bridge)",
                                color = PolishBlueOnContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Text(
                            text = if (isServerRunning) "MCP LIVE" else "IDLE",
                            color = PolishBlueOnContainer.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Search Bar (semantic + keyword)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search by keywords or semantic meaning...", fontSize = 13.sp, color = PolishTextMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PolishTextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = PolishTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PolishSurface,
                        unfocusedContainerColor = PolishSurface,
                        focusedBorderColor = PolishBluePrimary,
                        unfocusedBorderColor = PolishBorder,
                        focusedTextColor = PolishTextPrimary,
                        unfocusedTextColor = PolishTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_notes_field")
                )
            }

            // Classification Filter Chips Row (All, Ideas, Bugs, Tasks, Features, Meetings, Research)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                val filters = listOf(
                    ClassificationFilter.ALL,
                    ClassificationFilter.IDEA,
                    ClassificationFilter.BUG,
                    ClassificationFilter.TASK,
                    ClassificationFilter.FEATURE,
                    ClassificationFilter.MEETING,
                    ClassificationFilter.RESEARCH
                )
                items(filters) { filterItem ->
                    val isSelected = classificationFilter == filterItem
                    FilterChip(
                        selected = isSelected,
                        onClick = { onClassificationFilterChange(filterItem) },
                        label = { Text(filterItem.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishBluePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = PolishSurface,
                            labelColor = PolishTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PolishBluePrimary else PolishBorder
                        )
                    )
                }
            }

            // Type and Status Filter row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                item {
                    val isPendingSelected = statusFilter == StatusFilter.PENDING
                    FilterChip(
                        selected = isPendingSelected,
                        onClick = {
                            onStatusFilterChange(if (isPendingSelected) StatusFilter.ALL else StatusFilter.PENDING)
                        },
                        label = { Text("Pending Agent", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishPending,
                            selectedLabelColor = Color.White,
                            containerColor = PolishSurface,
                            labelColor = PolishTextSecondary
                        )
                    )
                }

                item {
                    val isVoiceSelected = typeFilter == TypeFilter.VOICE
                    FilterChip(
                        selected = isVoiceSelected,
                        onClick = {
                            onTypeFilterChange(if (isVoiceSelected) TypeFilter.ALL else TypeFilter.VOICE)
                        },
                        label = { Text("Voice Only", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishPurplePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = PolishSurface,
                            labelColor = PolishTextSecondary
                        )
                    )
                }

                item {
                    val isTextSelected = typeFilter == TypeFilter.TEXT
                    FilterChip(
                        selected = isTextSelected,
                        onClick = {
                            onTypeFilterChange(if (isTextSelected) TypeFilter.ALL else TypeFilter.TEXT)
                        },
                        label = { Text("Text Only", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishPurplePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = PolishSurface,
                            labelColor = PolishTextSecondary
                        )
                    )
                }
            }

            // Notes List
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PolishPurpleContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = PolishPurplePrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching notes found" else "No notes recorded yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PolishTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Record a voice note or type an idea.\nAI will transcribe, classify, and extract tasks for coding agents.",
                            fontSize = 13.sp,
                            color = PolishTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteItemCard(
                            note = note,
                            tasks = tasks,
                            isPlaying = isPlaying && playingNoteId == note.id,
                            currentPositionMs = if (playingNoteId == note.id) currentPositionMs else 0L,
                            totalDurationMs = if (playingNoteId == note.id) totalDurationMs else note.audioDurationMs,
                            onPlayAudio = { onPlayAudio(note) },
                            onDelete = { onDeleteNote(note.id) },
                            onToggleTask = onToggleTask
                        )
                    }
                }
            }
        }
    }
}
