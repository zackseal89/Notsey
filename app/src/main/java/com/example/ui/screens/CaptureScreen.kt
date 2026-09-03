package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.getTagColors
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldMintBg
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.MockupBackground
import com.example.ui.theme.MockupCardBorder
import com.example.ui.theme.MockupSurface
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryGrey
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
    var isPaused by remember { mutableStateOf(false) }

    // Selected tags
    var selectedTags by remember { mutableStateOf(listOf("agent", "bug", "todo")) }

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
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-start recording when entering this screen if not already recording
    LaunchedEffect(Unit) {
        if (!isRecording && !isSaving) {
            if (hasAudioPermission) {
                onStartRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Pulse animation for recording red dot
    val transition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    // Cursor blink animation
    val cursorVisible by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    // Timer formatted: 00:00:18
    val formattedTimer = remember(elapsedSeconds) {
        val hours = elapsedSeconds / 3600
        val mins = (elapsedSeconds % 3600) / 60
        val secs = elapsedSeconds % 60
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MockupBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Top Bar: Back Arrow, Centered "Voice Recording", Settings Gear
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Voice Recording",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            IconButton(onClick = { Toast.makeText(context, "Voice Recording Settings", Toast.LENGTH_SHORT).show() }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Status Indicator: Centered Red Dot + "Recording"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = if (isRecording && !isPaused) dotAlpha else 1f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPaused) "Paused" else "Recording",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryGrey
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Digital Timer (00:00:18)
        Text(
            text = formattedTimer,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Dynamic Audio Waveform Visualizer (Green bars)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalBars = 32
                for (i in 0 until totalBars) {
                    val multiplier = when {
                        i in 10..22 -> 1.0f - (kotlin.math.abs(i - 16) * 0.08f)
                        else -> 0.35f
                    }
                    val baseHeight = if (isRecording && !isPaused) {
                        (8 + amplitude * 48 * multiplier + (i % 5) * 4).coerceIn(6f, 56f)
                    } else {
                        8f
                    }

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(baseHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(EmeraldPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 5. Live Transcript Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MockupSurface),
            border = BorderStroke(1.dp, EmeraldBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header row: Sparkle + "Live transcript" | Language dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live transcript",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { Toast.makeText(context, "Select Language", Toast.LENGTH_SHORT).show() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "English (US)",
                            fontSize = 13.sp,
                            color = TextSecondaryGrey,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondaryGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transcript content with animated blinking cursor
                val transcriptDisplay = if (liveTranscript.isNotBlank()) {
                    liveTranscript
                } else {
                    "We need to refactor the authentication module and fix the token refresh logic. Also update the onboarding copy for better clarity..."
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = transcriptDisplay,
                        fontSize = 14.sp,
                        color = TextPrimaryDark,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isRecording && cursorVisible > 0.5f) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(16.dp)
                                .background(EmeraldDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags Row inside card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedTags.forEach { tag ->
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // + Add tag button
                    Surface(
                        color = MockupSurface,
                        border = BorderStroke(1.dp, MockupCardBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { Toast.makeText(context, "Add custom tag", Toast.LENGTH_SHORT).show() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextSecondaryGrey,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add tag",
                                fontSize = 12.sp,
                                color = TextSecondaryGrey,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Sound Level Meter Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sound level",
                        fontSize = 13.sp,
                        color = TextSecondaryGrey,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Good",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dot Matrix Meter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalDots = 28
                val filledDots = if (isRecording && !isPaused) ((amplitude * 35).toInt() + 14).coerceIn(4, totalDots) else 8

                for (i in 0 until totalDots) {
                    val isFilled = i < filledDots
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) EmeraldPrimary else Color(0xFFE5E7EB))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 7. Recording Controls: 3 Circular Buttons (Pause, Stop, Save)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Left: Pause Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MockupSurface)
                        .border(1.dp, MockupCardBorder, CircleShape)
                        .clickable { isPaused = !isPaused },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPaused) "Resume" else "Pause",
                    fontSize = 13.sp,
                    color = TextSecondaryGrey,
                    fontWeight = FontWeight.Medium
                )
            }

            // Center: Large Green Stop Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    // Outer Soft Halo Ring
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(EmeraldGlow)
                    )

                    // Inner Emerald Button
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .clickable {
                                onStopRecordingAndSave(null, selectedTags.joinToString(","))
                            }
                            .testTag("stop_recording_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            // Rounded Square Stop icon
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stop",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }

            // Right: Save Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MockupSurface)
                        .border(1.dp, MockupCardBorder, CircleShape)
                        .clickable {
                            onStopRecordingAndSave(null, selectedTags.joinToString(","))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save",
                    fontSize = 13.sp,
                    color = TextSecondaryGrey,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 8. Tip Banner at Bottom
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MockupSurface),
            border = BorderStroke(1.dp, MockupCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EmeraldMintBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Tip: Speak clearly for better results",
                    fontSize = 13.sp,
                    color = TextSecondaryGrey,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
