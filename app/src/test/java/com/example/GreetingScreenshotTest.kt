package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.NoteEntity
import com.example.ui.ClassificationFilter
import com.example.ui.StatusFilter
import com.example.ui.TypeFilter
import com.example.ui.screens.NotesListScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun testNotesListScreenScreenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        NotesListScreen(
          notes = listOf(
            NoteEntity(
              id = 1,
              title = "Agent Note Bridge",
              content = "Voice note from phone syncing to Claude Code and Antigravity.",
              type = "VOICE",
              tags = "voice,mcp,claude",
              agentStatus = "PENDING"
            )
          ),
          tasks = emptyList(),
          searchQuery = "",
          typeFilter = TypeFilter.ALL,
          statusFilter = StatusFilter.ALL,
          classificationFilter = ClassificationFilter.ALL,
          aiProcessingStatus = null,
          isServerRunning = true,
          serverPort = 8080,
          playingNoteId = null,
          isPlaying = false,
          currentPositionMs = 0L,
          totalDurationMs = 0L,
          onSearchChange = {},
          onTypeFilterChange = {},
          onStatusFilterChange = {},
          onClassificationFilterChange = {},
          onPlayAudio = {},
          onDeleteNote = {},
          onToggleTask = {},
          onNavigateToCapture = {},
          onNavigateToAgentBridge = {},
          onNavigateToTasks = {},
          onNavigateToDatabaseInspector = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
