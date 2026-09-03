package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.server.AgentLogEvent
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

@Composable
fun AgentBridgeScreen(
    isServerRunning: Boolean,
    serverUrl: String,
    serverPort: Int,
    recentLogs: List<AgentLogEvent>,
    onToggleServer: () -> Unit,
    onSimulateAgentQuery: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("MCP") } // "MCP", "REST", "SIMULATOR"

    val mcpConfigSnippet = remember(serverUrl) {
        """
{
  "mcpServers": {
    "phone-notes": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote-client",
        "$serverUrl/mcp"
      ]
    }
  }
}
        """.trimIndent()
    }

    val adbSnippet = "adb forward tcp:$serverPort tcp:$serverPort"
    val curlSnippet = "curl -X GET \"http://localhost:$serverPort/api/notes?status=pending\""

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
                        text = "Agent Bridge & Shared DB",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                    Text(
                        text = "Connects Phone App ⇄ Local Laptop Agents",
                        fontSize = 12.sp,
                        color = PolishBluePrimary
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Three Surfaces Architecture Diagram
            ThreeSurfacesDiagram()

            Spacer(modifier = Modifier.height(16.dp))

            // Bridge Server Control Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                border = BorderStroke(1.dp, PolishBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isServerRunning) PolishSuccess else PolishPending,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServerRunning) "SERVER ACTIVE" else "SERVER STOPPED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (isServerRunning) PolishSuccess else PolishPending
                            )
                        }

                        Button(
                            onClick = onToggleServer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServerRunning) PolishRecordRedContainer else PolishSuccessContainer,
                                contentColor = if (isServerRunning) PolishRecordRed else PolishSuccess
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("toggle_server_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isServerRunning) "Stop" else "Start", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Network Endpoint:", fontSize = 12.sp, color = PolishTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PolishSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, PolishBorder), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = serverUrl,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = PolishBluePrimary
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(serverUrl))
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy URL",
                                tint = PolishTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Agent Setup & Simulator Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("MCP" to "MCP Server", "REST" to "REST / ADB", "SIMULATOR" to "Test Agent").forEach { (key, label) ->
                    val isSelected = selectedTab == key
                    Surface(
                        color = if (isSelected) PolishBluePrimary else PolishSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) PolishBluePrimary else PolishBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = key }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else PolishTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                "MCP" -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishSurface),
                        border = BorderStroke(1.dp, PolishBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Claude Code / Claude Desktop Setup",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add this snippet to your claude_desktop_config.json or cursor / code agent:",
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PolishSurfaceVariant, RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, PolishBorder), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = mcpConfigSnippet,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = PolishTextPrimary,
                                    lineHeight = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(mcpConfigSnippet))
                                    Toast.makeText(context, "MCP configuration copied!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PolishBluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy MCP Config JSON", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Supported MCP Tools:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PolishTextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            listOf(
                                "list_notes" to "Retrieve recent voice/text notes with status filter",
                                "get_note" to "Fetch full transcribed note text and audio streaming URL",
                                "mark_note_processed" to "Record agent's code/action plan back onto the note",
                                "create_note" to "Agent pushes tasks directly to user's phone"
                            ).forEach { (tool, desc) ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("• $tool: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PolishBluePrimary)
                                    Text(desc, fontSize = 12.sp, color = PolishTextSecondary)
                                }
                            }
                        }
                    }
                }

                "REST" -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishSurface),
                        border = BorderStroke(1.dp, PolishBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Zero-Config USB / LAN Access",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "If your phone is plugged in via USB, forward the port to your laptop:",
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PolishSurfaceVariant, RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, PolishBorder), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = adbSnippet,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = PolishSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(adbSnippet))
                                    Toast.makeText(context, "ADB command copied!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PolishSurfaceVariant),
                                border = BorderStroke(1.dp, PolishBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Copy ADB Forward Command", fontSize = 12.sp, color = PolishTextPrimary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Fetch Pending Notes (cURL):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PolishSurfaceVariant, RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, PolishBorder), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = curlSnippet,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = PolishBluePrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                "SIMULATOR" -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishSurface),
                        border = BorderStroke(1.dp, PolishBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Simulate Local Agent Roundtrip",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Test how Claude Code or Antigravity retrieves a pending note, processes it, and sends the action plan back to your phone:",
                                fontSize = 12.sp,
                                color = PolishTextSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onSimulateAgentQuery("Claude Code") },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("simulate_claude_button")
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Claude Code", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = { onSimulateAgentQuery("Antigravity") },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPurplePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("simulate_antigravity_button")
                                ) {
                                    Icon(Icons.Default.Laptop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Antigravity", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Real-Time Agent Logs
            Text(
                text = "Live Agent Activity Log",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-time stream of agent queries & MCP tool executions",
                fontSize = 12.sp,
                color = PolishTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (recentLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PolishSurface, RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, PolishBorder), RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No agent queries yet. Waiting for incoming connections...", fontSize = 13.sp, color = PolishTextMuted)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentLogs.take(8).forEach { event ->
                        Surface(
                            color = PolishSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PolishBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Text(
                                    text = event.timestamp,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = PolishTextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = PolishPurplePill,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = event.agentName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPurplePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = event.detail,
                                    fontSize = 12.sp,
                                    color = PolishTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreeSurfacesDiagram() {
    Surface(
        color = PolishSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PolishBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "THREE CONNECTED SURFACES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PolishBluePrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Surface 1: Phone App
                SurfaceNode(
                    icon = Icons.Default.Smartphone,
                    title = "Surface 1",
                    subtitle = "Phone App\n(Voice/Text)",
                    accentColor = PolishBluePrimary,
                    containerColor = PolishBlueContainer
                )

                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = PolishTextMuted,
                    modifier = Modifier.size(20.dp)
                )

                // Surface 2: Shared DB & MCP
                SurfaceNode(
                    icon = Icons.Default.Storage,
                    title = "Surface 2",
                    subtitle = "Shared DB\n& MCP Server",
                    accentColor = PolishPurplePrimary,
                    containerColor = PolishPurpleContainer
                )

                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = PolishTextMuted,
                    modifier = Modifier.size(20.dp)
                )

                // Surface 3: Local Agent
                SurfaceNode(
                    icon = Icons.Default.Laptop,
                    title = "Surface 3",
                    subtitle = "Local Agent\n(Claude/Codex)",
                    accentColor = PolishSuccess,
                    containerColor = PolishSuccessContainer
                )
            }
        }
    }
}

@Composable
fun SurfaceNode(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    containerColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(86.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(containerColor, CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = PolishTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}
