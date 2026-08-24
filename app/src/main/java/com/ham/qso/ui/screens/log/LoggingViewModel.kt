package com.ham.qso.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.data.model.SessionEntity
import com.ham.qso.data.repository.QSORepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoggingUiState(
    val currentSession: SessionEntity? = null,
    val callsign: String = "",
    val rstSent: String = "59",
    val rstRcvd: String = "59",
    val band: Band = Band.BAND_40M,
    val mode: Mode = Mode.SSB,
    val frequencyMhz: String = "7.050",
    val theirGrid: String = "",
    val theirName: String = "",
    val qth: String = "",
    val altitudeMeters: String = "",
    val theirRig: String = "",
    val theirAntenna: String = "",
    val theirPowerWatts: String = "",
    val comment: String = "",
    val isDuplicate: Boolean = false,
    val recentQSOs: List<QSOEntity> = emptyList(),
    val editingQso: QSOEntity? = null,
    val lastLoggedQso: QSOEntity? = null,
    val totalQsoCount: Int = 0,
    val uniqueCallCount: Int = 0,
    val showAdvancedFields: Boolean = false,
    val saveSuccessMessage: String? = null
)

@OptIn(FlowPreview::class)
class LoggingViewModel(
    private val repository: QSORepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoggingUiState())
    val uiState: StateFlow<LoggingUiState> = _uiState.asStateFlow()

    init {
        // 观察当前活跃会话
        viewModelScope.launch {
            repository.currentSession.collect { session ->
                _uiState.update { it.copy(currentSession = session) }
                if (session != null) {
                    observeSessionStats(session.id)
                }
            }
        }

        // 防重通联实时监听 (Debounce 300ms)
        viewModelScope.launch {
            _uiState
                .map { Triple(it.currentSession?.id, it.callsign.trim(), it.band) }
                .distinctUntilChanged()
                .debounce(250)
                .collect { (sessionId, call, band) ->
                    if (sessionId != null && call.length >= 2) {
                        val isDupe = repository.isDuplicate(sessionId, call, band)
                        _uiState.update { it.copy(isDuplicate = isDupe) }
                    } else {
                        _uiState.update { it.copy(isDuplicate = false) }
                    }
                }
        }
    }

    private fun observeSessionStats(sessionId: Long) {
        viewModelScope.launch {
            repository.getQSOsForSession(sessionId).collect { list ->
                _uiState.update {
                    it.copy(
                        recentQSOs = list.take(5),
                        totalQsoCount = list.size,
                        uniqueCallCount = list.map { q -> q.callsign.uppercase() }.distinct().size
                    )
                }
            }
        }
    }

    fun onCallsignChanged(callsign: String) {
        // 自动大写并过滤非法字符
        val sanitized = callsign.uppercase().filter { it.isLetterOrDigit() || it == '/' }
        _uiState.update { it.copy(callsign = sanitized) }
    }

    fun onBandChanged(band: Band) {
        _uiState.update {
            it.copy(
                band = band,
                frequencyMhz = "%.3f".format(band.frequencyMhz)
            )
        }
    }

    fun onModeChanged(mode: Mode) {
        val defaultRst = when (mode) {
            Mode.CW -> "599"
            Mode.FT8, Mode.FT4, Mode.RTTY, Mode.PSK31 -> "-10"
            else -> "59"
        }
        _uiState.update {
            it.copy(
                mode = mode,
                rstSent = defaultRst,
                rstRcvd = defaultRst
            )
        }
    }

    fun onFrequencyChanged(freq: String) = _uiState.update { it.copy(frequencyMhz = freq) }
    fun onRstSentChanged(rst: String) = _uiState.update { it.copy(rstSent = rst) }
    fun onRstRcvdChanged(rst: String) = _uiState.update { it.copy(rstRcvd = rst) }
    fun onTheirGridChanged(grid: String) = _uiState.update { it.copy(theirGrid = grid.uppercase().trim()) }
    fun onTheirNameChanged(name: String) = _uiState.update { it.copy(theirName = name) }
    fun onQthChanged(qth: String) = _uiState.update { it.copy(qth = qth) }
    fun onAltitudeChanged(alt: String) = _uiState.update { it.copy(altitudeMeters = alt) }
    fun onTheirRigChanged(rig: String) = _uiState.update { it.copy(theirRig = rig) }
    fun onTheirAntennaChanged(antenna: String) = _uiState.update { it.copy(theirAntenna = antenna) }
    fun onTheirPowerChanged(pwr: String) = _uiState.update { it.copy(theirPowerWatts = pwr) }
    fun onCommentChanged(comment: String) = _uiState.update { it.copy(comment = comment) }
    fun toggleAdvancedFields() = _uiState.update { it.copy(showAdvancedFields = !it.showAdvancedFields) }
    fun dismissSuccessMessage() = _uiState.update { it.copy(saveSuccessMessage = null) }

    fun onEditQso(qso: QSOEntity?) = _uiState.update { it.copy(editingQso = qso) }

    fun updateQso(qso: QSOEntity) {
        viewModelScope.launch {
            repository.updateQSO(qso)
            _uiState.update {
                it.copy(
                    editingQso = null,
                    saveSuccessMessage = "已更新 QSO: ${qso.callsign}"
                )
            }
        }
    }

    fun deleteQso(qso: QSOEntity) {
        viewModelScope.launch {
            repository.deleteQSO(qso)
            _uiState.update { it.copy(editingQso = null) }
        }
    }

    fun logQSO() {
        val state = _uiState.value
        val call = state.callsign.trim()
        if (call.isBlank()) return

        viewModelScope.launch {
            // 确保有活跃会话，若无则自动创建一个默认会话
            var currentSessionId = state.currentSession?.id
            if (currentSessionId == null) {
                val newSession = SessionEntity(
                    name = "默认户外架台",
                    isCurrent = true
                )
                currentSessionId = repository.insertSession(newSession)
                repository.setCurrentSession(currentSessionId)
            }

            val session = repository.getCurrentSessionDirect()

            val freq = state.frequencyMhz.toDoubleOrNull() ?: state.band.frequencyMhz
            val qso = QSOEntity(
                sessionId = currentSessionId,
                callsign = call,
                rstSent = state.rstSent.ifBlank { "59" },
                rstRcvd = state.rstRcvd.ifBlank { "59" },
                timestampUtc = System.currentTimeMillis(),
                theirGrid = state.theirGrid,
                theirName = state.theirName,
                qth = state.qth,
                altitudeMeters = state.altitudeMeters.toIntOrNull(),
                theirRig = state.theirRig,
                theirAntenna = state.theirAntenna,
                theirPowerWatts = state.theirPowerWatts.toIntOrNull(),
                comment = state.comment,
                band = state.band,
                mode = state.mode,
                frequencyMhz = freq,
                myCallsign = session?.myCallsign ?: "",
                myGrid = session?.myGrid ?: "",
                potaRef = session?.potaRef ?: "",
                sotaRef = session?.sotaRef ?: "",
                txPowerWatts = session?.txPowerWatts ?: 100
            )

            val newId = repository.insertQSO(qso)
            val insertedQso = qso.copy(id = newId)

            // 极速录入重置：清空对方临时字段，保留波段/模式/频率/RST
            _uiState.update {
                it.copy(
                    callsign = "",
                    theirGrid = "",
                    theirName = "",
                    qth = "",
                    altitudeMeters = "",
                    theirRig = "",
                    theirAntenna = "",
                    theirPowerWatts = "",
                    comment = "",
                    isDuplicate = false,
                    lastLoggedQso = insertedQso,
                    saveSuccessMessage = "已成功记录 QSO: $call (${state.band.label} ${state.mode.label})"
                )
            }
        }
    }
}
