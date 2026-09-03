package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {

    // ================= AGENT RUNS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: AgentRunEntity): Long

    @Update
    suspend fun updateRun(run: AgentRunEntity)

    @Query("UPDATE agent_runs SET status = :status, completedAt = :completedAt, summary = :summary, error = :error WHERE id = :id")
    suspend fun completeRun(id: Long, status: String, completedAt: Long = System.currentTimeMillis(), summary: String?, error: String?)

    @Query("SELECT * FROM agent_runs ORDER BY startedAt DESC")
    fun getAllRuns(): Flow<List<AgentRunEntity>>

    @Query("SELECT * FROM agent_runs ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentRunsSync(limit: Int = 20): List<AgentRunEntity>

    @Query("SELECT COUNT(*) FROM agent_runs")
    suspend fun getAgentRunCount(): Int

    // ================= AGENT ACTIONS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: AgentActionEntity): Long

    @Query("SELECT * FROM agent_actions ORDER BY timestamp DESC")
    fun getAllActions(): Flow<List<AgentActionEntity>>

    @Query("SELECT * FROM agent_actions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getActionsForNote(noteId: Long): Flow<List<AgentActionEntity>>

    @Query("SELECT * FROM agent_actions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActionsSync(limit: Int = 30): List<AgentActionEntity>

    @Query("SELECT COUNT(*) FROM agent_actions")
    suspend fun getAgentActionCount(): Int

    // ================= ACTIVITY EVENTS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEventEntity): Long

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEventsSync(limit: Int = 50): List<ActivityEventEntity>

    @Query("SELECT COUNT(*) FROM activity_events")
    suspend fun getActivityEventCount(): Int
}
