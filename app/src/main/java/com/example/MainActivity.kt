package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.SurfaceTab
import com.example.ui.screens.AgentBridgeScreen
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.DatabaseInspectorScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.screens.TasksScreen
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPurplePill
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AgentNotesApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AgentNotesApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val classificationFilter by viewModel.classificationFilter.collectAsStateWithLifecycle()
    val aiProcessingStatus by viewModel.aiProcessingStatus.collectAsStateWithLifecycle()
    val tableStats by viewModel.tableStats.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()

    // Recorder states
    val isRecording by viewModel.recorder.isRecording.collectAsStateWithLifecycle()
    val amplitude by viewModel.recorder.amplitude.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.recorder.elapsedSeconds.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.recorder.liveTranscript.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    // Player states
    val playingNoteId by viewModel.player.playingNoteId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.player.currentPositionMs.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.player.totalDurationMs.collectAsStateWithLifecycle()

    // Server states
    val isServerRunning by viewModel.server.isRunning.collectAsStateWithLifecycle()
    val serverUrl by viewModel.server.serverUrl.collectAsStateWithLifecycle()
    val serverPort by viewModel.server.port.collectAsStateWithLifecycle()
    val recentLogs by viewModel.server.recentLogs.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PolishBackground,
        bottomBar = {
            NavigationBar(
                containerColor = PolishSurfaceVariant,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == SurfaceTab.NOTES,
                    onClick = { viewModel.setTab(SurfaceTab.NOTES) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Notes"
                        )
                    },
                    label = { Text("Stream", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PolishTextPrimary,
                        selectedTextColor = PolishTextPrimary,
                        indicatorColor = PolishPurplePill,
                        unselectedIconColor = PolishTextSecondary,
                        unselectedTextColor = PolishTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_notes_tab")
                )

                NavigationBarItem(
                    selected = currentTab == SurfaceTab.RECORD,
                    onClick = { viewModel.setTab(SurfaceTab.RECORD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Capture"
                        )
                    },
                    label = { Text("Capture", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PolishTextPrimary,
                        selectedTextColor = PolishTextPrimary,
                        indicatorColor = PolishPurplePill,
                        unselectedIconColor = PolishTextSecondary,
                        unselectedTextColor = PolishTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_capture_tab")
                )

                NavigationBarItem(
                    selected = currentTab == SurfaceTab.TASKS,
                    onClick = { viewModel.setTab(SurfaceTab.TASKS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Tasks"
                        )
                    },
                    label = { Text("Tasks", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PolishTextPrimary,
                        selectedTextColor = PolishTextPrimary,
                        indicatorColor = PolishPurplePill,
                        unselectedIconColor = PolishTextSecondary,
                        unselectedTextColor = PolishTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tasks_tab")
                )

                NavigationBarItem(
                    selected = currentTab == SurfaceTab.AGENT_BRIDGE,
                    onClick = { viewModel.setTab(SurfaceTab.AGENT_BRIDGE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Bridge & MCP"
                        )
                    },
                    label = { Text("MCP Bridge", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PolishTextPrimary,
                        selectedTextColor = PolishTextPrimary,
                        indicatorColor = PolishPurplePill,
                        unselectedIconColor = PolishTextSecondary,
                        unselectedTextColor = PolishTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_bridge_tab")
                )

                NavigationBarItem(
                    selected = currentTab == SurfaceTab.DATABASE_INSPECTOR,
                    onClick = { viewModel.setTab(SurfaceTab.DATABASE_INSPECTOR) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Database"
                        )
                    },
                    label = { Text("Database", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PolishTextPrimary,
                        selectedTextColor = PolishTextPrimary,
                        indicatorColor = PolishPurplePill,
                        unselectedIconColor = PolishTextSecondary,
                        unselectedTextColor = PolishTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_database_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "surfaceTransition"
            ) { targetSurface ->
                when (targetSurface) {
                    SurfaceTab.NOTES -> {
                        NotesListScreen(
                            notes = notes,
                            tasks = allTasks,
                            searchQuery = searchQuery,
                            typeFilter = typeFilter,
                            statusFilter = statusFilter,
                            classificationFilter = classificationFilter,
                            aiProcessingStatus = aiProcessingStatus,
                            isServerRunning = isServerRunning,
                            serverPort = serverPort,
                            playingNoteId = playingNoteId,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            totalDurationMs = totalDurationMs,
                            onSearchChange = viewModel::setSearchQuery,
                            onTypeFilterChange = viewModel::setTypeFilter,
                            onStatusFilterChange = viewModel::setStatusFilter,
                            onClassificationFilterChange = viewModel::setClassificationFilter,
                            onPlayAudio = viewModel::playNoteAudio,
                            onDeleteNote = viewModel::deleteNote,
                            onToggleTask = viewModel::toggleTaskCompletion,
                            onNavigateToCapture = { viewModel.setTab(SurfaceTab.RECORD) },
                            onNavigateToAgentBridge = { viewModel.setTab(SurfaceTab.AGENT_BRIDGE) },
                            onNavigateToTasks = { viewModel.setTab(SurfaceTab.TASKS) },
                            onNavigateToDatabaseInspector = { viewModel.setTab(SurfaceTab.DATABASE_INSPECTOR) }
                        )
                    }

                    SurfaceTab.RECORD -> {
                        CaptureScreen(
                            isRecording = isRecording,
                            amplitude = amplitude,
                            elapsedSeconds = elapsedSeconds,
                            liveTranscript = liveTranscript,
                            isSaving = isSaving,
                            onStartRecording = viewModel::startVoiceRecording,
                            onStopRecordingAndSave = viewModel::stopVoiceRecordingAndSave,
                            onCancelRecording = viewModel::cancelVoiceRecording,
                            onSaveTextNote = viewModel::saveTextNote,
                            onBack = { viewModel.setTab(SurfaceTab.NOTES) }
                        )
                    }

                    SurfaceTab.TASKS -> {
                        TasksScreen(
                            tasks = allTasks,
                            notes = notes,
                            onToggleTask = viewModel::toggleTaskCompletion,
                            onBack = { viewModel.setTab(SurfaceTab.NOTES) }
                        )
                    }

                    SurfaceTab.AGENT_BRIDGE -> {
                        AgentBridgeScreen(
                            isServerRunning = isServerRunning,
                            serverUrl = serverUrl,
                            serverPort = serverPort,
                            recentLogs = recentLogs,
                            onToggleServer = viewModel::toggleServer,
                            onSimulateAgentQuery = viewModel::simulateAgentQuery,
                            onBack = { viewModel.setTab(SurfaceTab.NOTES) }
                        )
                    }

                    SurfaceTab.DATABASE_INSPECTOR -> {
                        DatabaseInspectorScreen(
                            tableStats = tableStats,
                            recentEvents = recentEvents,
                            onRefresh = viewModel::refreshStats,
                            onBack = { viewModel.setTab(SurfaceTab.NOTES) }
                        )
                    }
                }
            }
        }
    }
}
