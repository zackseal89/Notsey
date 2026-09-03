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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.SurfaceTab
import com.example.ui.screens.AgentBridgeScreen
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.DatabaseInspectorScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MockupBackground
import com.example.ui.theme.MockupCardBorder
import com.example.ui.theme.MockupSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextPrimaryDark

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
    val allNotes by viewModel.repository.allNotes.collectAsStateWithLifecycle(emptyList())
    val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()
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

    val showBottomBar = currentTab != SurfaceTab.RECORD && currentTab != SurfaceTab.NOTE_DETAIL

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MockupBackground,
        bottomBar = {
            if (showBottomBar) {
                MockupBottomNavigation(
                    currentTab = currentTab,
                    onSelectTab = { tab -> viewModel.setTab(tab) },
                    modifier = Modifier.navigationBarsPadding()
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
                label = "screenTransition"
            ) { targetSurface ->
                when (targetSurface) {
                    SurfaceTab.HOME, SurfaceTab.NOTES -> {
                        NotesListScreen(
                            notes = if (targetSurface == SurfaceTab.HOME) allNotes.take(6) else notes,
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
                            onNavigateToAgentBridge = { viewModel.setTab(SurfaceTab.AGENTS) },
                            onNavigateToTasks = { viewModel.setTab(SurfaceTab.ACTIVITY) },
                            onNavigateToDatabaseInspector = { viewModel.setTab(SurfaceTab.ACTIVITY) },
                            onSelectNote = { id -> viewModel.openNoteDetail(id) },
                            onSaveTextNote = viewModel::saveTextNote
                        )
                    }

                    SurfaceTab.NOTE_DETAIL -> {
                        val activeNote = selectedNote ?: allNotes.firstOrNull()
                        if (activeNote != null) {
                            NoteDetailScreen(
                                note = activeNote,
                                allNotes = allNotes,
                                isPlaying = isPlaying && playingNoteId == activeNote.id,
                                currentPositionMs = if (playingNoteId == activeNote.id) currentPositionMs else 0L,
                                totalDurationMs = if (playingNoteId == activeNote.id) totalDurationMs else activeNote.audioDurationMs,
                                onPlayAudio = { viewModel.playNoteAudio(activeNote) },
                                onBack = { viewModel.closeNoteDetail() },
                                onSelectNote = { id -> viewModel.openNoteDetail(id) },
                                onMarkProcessed = { id -> viewModel.markNoteProcessed(id) },
                                onCreateTask = { _ -> viewModel.setTab(SurfaceTab.ACTIVITY) },
                                onSendToAgent = { _ -> viewModel.setTab(SurfaceTab.AGENTS) }
                            )
                        } else {
                            viewModel.closeNoteDetail()
                        }
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
                            onCancelRecording = {
                                viewModel.cancelVoiceRecording()
                                viewModel.setTab(SurfaceTab.HOME)
                            },
                            onSaveTextNote = viewModel::saveTextNote,
                            onBack = { viewModel.setTab(SurfaceTab.HOME) }
                        )
                    }

                    SurfaceTab.AGENTS -> {
                        AgentBridgeScreen(
                            isServerRunning = isServerRunning,
                            serverUrl = serverUrl,
                            serverPort = serverPort,
                            recentLogs = recentLogs,
                            onToggleServer = viewModel::toggleServer,
                            onSimulateAgentQuery = viewModel::simulateAgentQuery,
                            onBack = { viewModel.setTab(SurfaceTab.HOME) }
                        )
                    }

                    SurfaceTab.ACTIVITY -> {
                        DatabaseInspectorScreen(
                            tableStats = tableStats,
                            recentEvents = recentEvents,
                            onRefresh = viewModel::refreshStats,
                            onBack = { viewModel.setTab(SurfaceTab.HOME) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MockupBottomNavigation(
    currentTab: SurfaceTab,
    onSelectTab: (SurfaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MockupSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MockupCardBorder),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_navigation_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // 1. Home
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentTab == SurfaceTab.HOME,
                onClick = { onSelectTab(SurfaceTab.HOME) },
                testTag = "nav_home_tab"
            )

            // 2. Notes
            BottomNavItem(
                icon = Icons.Default.Description,
                label = "Notes",
                isSelected = currentTab == SurfaceTab.NOTES,
                onClick = { onSelectTab(SurfaceTab.NOTES) },
                testTag = "nav_notes_tab"
            )

            // 3. Agents
            BottomNavItem(
                icon = Icons.Default.SmartToy,
                label = "Agents",
                isSelected = currentTab == SurfaceTab.AGENTS,
                onClick = { onSelectTab(SurfaceTab.AGENTS) },
                testTag = "nav_agents_tab"
            )

            // 4. Activity
            BottomNavItem(
                icon = Icons.Default.Leaderboard,
                label = "Activity",
                isSelected = currentTab == SurfaceTab.ACTIVITY,
                onClick = { onSelectTab(SurfaceTab.ACTIVITY) },
                testTag = "nav_activity_tab"
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) EmeraldPrimary else TextMutedGrey,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) EmeraldDark else TextMutedGrey
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Green Indicator Underline Bar for selected tab (Mockup Image 1)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(EmeraldPrimary)
            )
        } else {
            Spacer(modifier = Modifier.height(2.5.dp))
        }
    }
}
