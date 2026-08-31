package com.ham.qso.data.local

import androidx.room.*
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.QSOEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QSODao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQSO(qso: QSOEntity): Long

    @Update
    suspend fun updateQSO(qso: QSOEntity)

    @Delete
    suspend fun deleteQSO(qso: QSOEntity)

    @Query("DELETE FROM qso_logs WHERE id = :id")
    suspend fun deleteQSOById(id: Long)

    @Query("SELECT * FROM qso_logs WHERE id = :id")
    suspend fun getQSOById(id: Long): QSOEntity?

    @Query("SELECT * FROM qso_logs WHERE sessionId = :sessionId ORDER BY timestampUtc DESC")
    fun getQSOsForSession(sessionId: Long): Flow<List<QSOEntity>>

    @Query("SELECT * FROM qso_logs ORDER BY timestampUtc DESC")
    fun getAllQSOs(): Flow<List<QSOEntity>>

    @Query("SELECT * FROM qso_logs WHERE sessionId = :sessionId ORDER BY timestampUtc DESC")
    suspend fun getQSOsForSessionDirect(sessionId: Long): List<QSOEntity>

    @Query("SELECT * FROM qso_logs ORDER BY timestampUtc DESC")
    suspend fun getAllQSOsDirect(): List<QSOEntity>

    @Query("""
        SELECT COUNT(*) FROM qso_logs
        WHERE sessionId = :sessionId
        AND UPPER(callsign) = UPPER(:callsign)
        AND band = :band
    """)
    suspend fun countDuplicate(sessionId: Long, callsign: String, band: Band): Int

    @Query("SELECT COUNT(*) FROM qso_logs WHERE sessionId = :sessionId")
    fun getQSOCountForSession(sessionId: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT callsign) FROM qso_logs WHERE sessionId = :sessionId")
    fun getUniqueCallCountForSession(sessionId: Long): Flow<Int>

    @Query("SELECT DISTINCT audioFilePath FROM qso_logs WHERE audioFilePath IS NOT NULL AND audioFilePath != ''")
    fun getAllAudioFilePaths(): Flow<List<String>>

    @Query("SELECT DISTINCT audioFilePath FROM qso_logs WHERE audioFilePath IS NOT NULL AND audioFilePath != ''")
    suspend fun getAllAudioFilePathsDirect(): List<String>

    @Query("SELECT COUNT(*) FROM qso_logs WHERE audioFilePath = :filePath")
    suspend fun countQsoUsingAudioFile(filePath: String): Int
}
