package com.ham.qso.data.repository

import com.ham.qso.data.local.AppPreferences
import com.ham.qso.data.local.QSODao
import com.ham.qso.data.local.SessionDao
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 通联日志与会话仓储层 (Repository)
 */
class QSORepository(
    private val qsoDao: QSODao,
    private val sessionDao: SessionDao,
    private val appPreferences: AppPreferences? = null
) {
    // ── Preferences ─────────────────────────────────────────────
    fun getLastBand(): Band = appPreferences?.lastBand ?: Band.BAND_40M

    fun saveLastBand(band: Band) {
        appPreferences?.lastBand = band
    }

    fun getLastMode(): Mode = appPreferences?.lastMode ?: Mode.SSB

    fun saveLastMode(mode: Mode) {
        appPreferences?.lastMode = mode
    }

    fun getLastFrequencyMhz(): String =
        appPreferences?.lastFrequencyMhz ?: "%.3f".format(getLastBand().frequencyMhz)

    fun saveLastFrequencyMhz(freq: String) {
        appPreferences?.lastFrequencyMhz = freq
    }
    // ── Session ─────────────────────────────────────────────────
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    val currentSession: Flow<SessionEntity?> = sessionDao.getCurrentSession()

    suspend fun getCurrentSessionDirect(): SessionEntity? = sessionDao.getCurrentSessionDirect()

    suspend fun insertSession(session: SessionEntity): Long = sessionDao.insertSession(session)

    suspend fun updateSession(session: SessionEntity) = sessionDao.updateSession(session)

    suspend fun deleteSession(session: SessionEntity) = sessionDao.deleteSession(session)

    /** 切换当前活跃会话（两步原子操作，由 Repository 保证） */
    suspend fun setCurrentSession(sessionId: Long) {
        sessionDao.clearCurrentSessionFlag()
        sessionDao.setSessionActive(sessionId)
    }

    // ── QSO ─────────────────────────────────────────────────────
    fun getQSOsForSession(sessionId: Long): Flow<List<QSOEntity>> =
        qsoDao.getQSOsForSession(sessionId)

    fun getAllQSOs(): Flow<List<QSOEntity>> = qsoDao.getAllQSOs()

    suspend fun getQSOsForSessionDirect(sessionId: Long): List<QSOEntity> =
        qsoDao.getQSOsForSessionDirect(sessionId)

    suspend fun getAllQSOsDirect(): List<QSOEntity> = qsoDao.getAllQSOsDirect()

    suspend fun insertQSO(qso: QSOEntity): Long = qsoDao.insertQSO(qso)

    suspend fun updateQSO(qso: QSOEntity) = qsoDao.updateQSO(qso)

    suspend fun deleteQSO(qso: QSOEntity) = qsoDao.deleteQSO(qso)

    suspend fun deleteQSOById(id: Long) = qsoDao.deleteQSOById(id)

    suspend fun isDuplicate(sessionId: Long, callsign: String, band: Band): Boolean =
        qsoDao.countDuplicate(sessionId, callsign, band) > 0

    fun getQSOCountForSession(sessionId: Long): Flow<Int> =
        qsoDao.getQSOCountForSession(sessionId)

    fun getUniqueCallCountForSession(sessionId: Long): Flow<Int> =
        qsoDao.getUniqueCallCountForSession(sessionId)

    fun getAllAudioFilePaths(): Flow<List<String>> = qsoDao.getAllAudioFilePaths()

    suspend fun getAllAudioFilePathsDirect(): List<String> = qsoDao.getAllAudioFilePathsDirect()

    suspend fun countQsoUsingAudioFile(filePath: String): Int = qsoDao.countQsoUsingAudioFile(filePath)
}
