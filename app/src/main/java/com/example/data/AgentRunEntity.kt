package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_runs")
data class AgentRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val agentName: String, // "Claude Code", "Codex", "Cursor", "Antigravity"
    val triggerSource: String = "MCP_TOOL", // "MCP_TOOL", "REST_API", "SIMULATOR"
    val status: String = "RUNNING", // "RUNNING", "COMPLETED", "FAILED"
    val targetNoteId: Long? = null,
    val summary: String? = null,
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
