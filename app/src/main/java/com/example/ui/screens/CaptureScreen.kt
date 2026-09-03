package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishBlueContainer
import com.example.ui.theme.PolishBlueOnContainer
import com.example.ui.theme.PolishBluePrimary
import com.example.ui.theme.PolishBorder
import com.example.ui.theme.PolishPending
import com.example.ui.theme.PolishPendingContainer
import com.example.ui.theme.PolishPurpleContainer
import com.example.ui.theme.PolishPurplePill
import com.example.ui.theme.PolishPurplePrimary
import com.example.ui.theme.PolishRecordRed
import com.example.ui.theme.PolishRecordRedContainer
import com.example.ui.theme.PolishSuccess
import com.example.ui.theme.PolishSuccessContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import java.util.Locale

@Composable
fun CaptureScreen(
    isRecording: Boolean,
    amplitude: Float,
    elapsedSeconds: Int,
    liveTranscript: String,
    isSaving: Boolean,
    onStartRecording: () -> Boolean,
    onStopRecordingAndSave: (String?, String?) -> Unit,
    onCancelRecording: () -> Unit,
    onSaveTextNote: (String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Voice, 1: Text, 2: Video

    // Text inputs
    var titleInput by remember { mutableStateOf("") }
    var textContentInput by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf("agent", "idea")) }

    // Audio Permission Launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            onStartRecording()
        }
    }

    val availableTags = listOf("agent", "idea", "todo", "bug", "claude", "architecture")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Surface(
            color = PolishSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PolishTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Instant Note Capture",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                    Text(
                        text = "Syncs immediately to local agents & MCP",
                        fontSize = 12.sp,
                        color = PolishBluePrimary
                    )
                }
            }
        }

        // Mode Switcher Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PolishSurfaceVariant,
            contentColor = PolishBluePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PolishBluePrimary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "🎙️ Voice",
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 0) PolishBluePrimary else PolishTextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "✍️ Text",
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 1) PolishBluePrimary else PolishTextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        "🎥 Video",
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 2) PolishBluePrimary else PolishTextSecondary
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Contents
        when (selectedTab) {
            0 -> {
                // VOICE CAPTURE (Prioritized & Frictionless)
                VoiceCaptureContent(
                    isRecording = isRecording,
                    amplitude = amplitude,
                    elapsedSeconds = elapsedSeconds,
                    liveTranscript = liveTranscript,
                    isSaving = isSaving,
                    titleInput = titleInput,
                    onTitleChange = { titleInput = it },
                    selectedTags = selectedTags,
                    onToggleTag = { tag ->
                        selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                    },
                    availableTags = availableTags,
                    onMicClick = {
                        if (isRecording) {
                            onStopRecordingAndSave(
                                titleInput.ifBlank { null },
                                selectedTags.joinToString(",")
                            )
                        } else {
                            if (hasAudioPermission) {
                                onStartRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onCancel = onCancelRecording
                )
            }

            1 -> {
                // TEXT CAPTURE
                TextCaptureContent(
                    title = titleInput,
                    onTitleChange = { titleInput = it },
                    content = textContentInput,
                    onContentChange = { textContentInput = it },
                    selectedTags = selectedTags,
                    availableTags = availableTags,
                    isSaving = isSaving,
                    onToggleTag = { tag ->
                        selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                    },
                    onSave = {
                        if (textContentInput.isNotBlank()) {
                            onSaveTextNote(
                                titleInput.trim(),
                                textContentInput.trim(),
                                selectedTags.joinToString(",")
                            )
                        }
                    }
                )
            }

            2 -> {
                // VIDEO MEMO
                VideoMemoContent(
                    onSaveAsNote = { memo ->
                        onSaveTextNote(
                            "Video Memo Note",
                            memo,
                            "video,agent"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun VoiceCaptureContent(
    isRecording: Boolean,
    amplitude: Float,
    elapsedSeconds: Int,
    liveTranscript: String,
    isSaving: Boolean,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    availableTags: List<String>,
    onMicClick: () -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Status prompt
        Text(
            text = if (isRecording) "Recording Voice Note..." else "Tap to Record Voice Note",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isRecording) PolishRecordRed else PolishTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isRecording) "Speak clearly. Real-time transcript is active." else "Zero friction: single tap to speak, single tap to sync to agent.",
            fontSize = 13.sp,
            color = PolishTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Main Mic Button with Professional Polish circular container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            if (isRecording) {
                // Pulse aura
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .background(PolishRecordRedContainer.copy(alpha = 0.6f), CircleShape)
                )
            } else {
                // Outer subtle container ring
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .background(PolishBlueContainer, CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                )
            }

            // Central Button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) PolishRecordRed else PolishBluePrimary)
                    .clickable { onMicClick() }
                    .testTag("record_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                } else {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Live Timer
        Text(
            text = formatSeconds(elapsedSeconds),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) PolishRecordRed else PolishTextMuted
        )

        // Waveform Visualizer
        Spacer(modifier = Modifier.height(16.dp))
        LiveWaveformVisualizer(
            isRecording = isRecording,
            amplitude = amplitude,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        // Live Transcript Preview Box
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            color = PolishSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, PolishBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LIVE SPEECH TRANSCRIPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishBluePrimary,
                        letterSpacing = 1.sp
                    )
                    if (isRecording) {
                        Surface(
                            color = PolishRecordRedContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "LISTENING",
                                color = PolishRecordRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (liveTranscript.isNotBlank()) liveTranscript else if (isRecording) "Listening to your voice..." else "Your speech transcript will automatically appear here.",
                    fontSize = 14.sp,
                    color = if (liveTranscript.isNotBlank()) PolishTextPrimary else PolishTextMuted,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Optional Title and Tag Customization
        OutlinedTextField(
            value = titleInput,
            onValueChange = onTitleChange,
            placeholder = { Text("Note Title (Optional - auto-generated if empty)", color = PolishTextMuted, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PolishSurface,
                unfocusedContainerColor = PolishSurface,
                focusedBorderColor = PolishBluePrimary,
                unfocusedBorderColor = PolishBorder,
                focusedTextColor = PolishTextPrimary,
                unfocusedTextColor = PolishTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Tag chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (tag in availableTags.take(4)) {
                val isSelected = selectedTags.contains(tag)
                Surface(
                    color = if (isSelected) PolishPurplePill else PolishSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) PolishPurplePrimary else PolishBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleTag(tag) }
                ) {
                    Text(
                        text = "#$tag",
                        color = if (isSelected) PolishPurplePrimary else PolishTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons if recording
        if (isRecording) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishSurfaceVariant),
                    border = BorderStroke(1.dp, PolishBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = PolishTextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Discard", color = PolishTextSecondary)
                }

                Button(
                    onClick = onMicClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBluePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Sync", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LiveWaveformVisualizer(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until barCount) {
            val factor = ((i % 7) + 1) / 7f
            val baseHeight = if (isRecording) (12 + amplitude * 32 * factor).coerceIn(4f, 40f) else 4f
            val barColor = if (isRecording) {
                if (i % 2 == 0) PolishBluePrimary else PolishPurplePrimary
            } else PolishBorder

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun TextCaptureContent(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    selectedTags: Set<String>,
    availableTags: List<String>,
    isSaving: Boolean,
    onToggleTag: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Type Note for Local Agent",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = PolishTextPrimary
        )
        Text(
            text = "Notes are saved to the shared database and exposed to Claude Code / Antigravity via MCP.",
            fontSize = 13.sp,
            color = PolishTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Note Title (e.g. Optimize SQL query in Agent)", color = PolishTextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
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
                .testTag("text_note_title")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text("Write your thoughts, task requirements, or agent instructions here...", color = PolishTextMuted) },
            minLines = 6,
            shape = RoundedCornerShape(14.dp),
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
                .testTag("text_note_content")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tags
        Text("Tags:", fontSize = 12.sp, color = PolishTextMuted, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (tag in availableTags) {
                val isSelected = selectedTags.contains(tag)
                Surface(
                    color = if (isSelected) PolishPurplePill else PolishSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) PolishPurplePrimary else PolishBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleTag(tag) }
                ) {
                    Text(
                        text = "#$tag",
                        color = if (isSelected) PolishPurplePrimary else PolishTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = content.isNotBlank() && !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = PolishBluePrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_text_note_button")
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Note to Shared Database", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun VideoMemoContent(
    onSaveAsNote: (String) -> Unit
) {
    var memoText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Video & Multimodal Note",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = PolishTextPrimary
        )
        Text(
            text = "Record quick video memos or attach visual bug summaries for your local agents.",
            fontSize = 13.sp,
            color = PolishTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = PolishSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, PolishBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(PolishPurpleContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = PolishPurplePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Video Note Capture", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PolishTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "You can capture a video memo and add notes that local agents can analyze.",
                    fontSize = 13.sp,
                    color = PolishTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = memoText,
            onValueChange = { memoText = it },
            placeholder = { Text("Describe the video note or attach context...", color = PolishTextMuted) },
            minLines = 3,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PolishSurface,
                unfocusedContainerColor = PolishSurface,
                focusedBorderColor = PolishBluePrimary,
                unfocusedBorderColor = PolishBorder,
                focusedTextColor = PolishTextPrimary,
                unfocusedTextColor = PolishTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (memoText.isNotBlank()) {
                    onSaveAsNote(memoText)
                }
            },
            enabled = memoText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = PolishBluePrimary, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Video Memo", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
