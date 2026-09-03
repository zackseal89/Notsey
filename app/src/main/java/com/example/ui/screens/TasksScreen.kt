package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishBlueContainer
import com.example.ui.theme.PolishBluePrimary
import com.example.ui.theme.PolishBorder
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    notes: List<NoteEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterCompletedOnly by remember { mutableStateOf<Boolean?>(null) }

    val filteredTasks = tasks.filter { task ->
        when (filterCompletedOnly) {
            true -> task.isCompleted
            false -> !task.isCompleted
            null -> true
        }
    }

    val noteMap = remember(notes) { notes.associateBy { it.id } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PolishTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Actionable Tasks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Text(
                            text = "${tasks.count { it.isCompleted }}/${tasks.size} Completed • Extracted by AI",
                            fontSize = 12.sp,
                            color = PolishTextSecondary
                        )
                    }
                }
            }

            // Filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                FilterChip(
                    selected = filterCompletedOnly == null,
                    onClick = { filterCompletedOnly = null },
                    label = { Text("All (${tasks.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PolishBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = filterCompletedOnly == false,
                    onClick = { filterCompletedOnly = false },
                    label = { Text("Pending (${tasks.count { !it.isCompleted }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PolishBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = filterCompletedOnly == true,
                    onClick = { filterCompletedOnly = true },
                    label = { Text("Done (${tasks.count { it.isCompleted }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PolishBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = PolishTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tasks found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val associatedNote = noteMap[task.noteId]

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onToggleTask(task) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (task.isCompleted) "Completed" else "Pending",
                                    tint = if (task.isCompleted) PolishSuccess else PolishTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (task.isCompleted) PolishTextMuted else PolishTextPrimary,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )

                                    if (associatedNote != null) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "From: ${associatedNote.title}",
                                            fontSize = 11.sp,
                                            color = PolishTextSecondary
                                        )
                                    }
                                }

                                if (task.priority == "HIGH") {
                                    Surface(
                                        color = Color(0xFFF9DEDC),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "HIGH",
                                            color = Color(0xFFB3261E),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
