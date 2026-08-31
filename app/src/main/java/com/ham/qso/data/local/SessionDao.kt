package com.ham.qso.data.local

import androidx.room.*
import com.ham.qso.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSessionDirect(): SessionEntity?

    @Query("UPDATE sessions SET isCurrent = 0")
    suspend fun clearCurrentSessionFlag()

    @Query("UPDATE sessions SET isCurrent = 1 WHERE id = :sessionId")
    suspend fun setSessionActive(sessionId: Long)
}
