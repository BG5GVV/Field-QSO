package com.ham.qso.ui.screens.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.data.model.SessionEntity
import com.ham.qso.data.repository.QSORepository
import com.ham.qso.domain.adif.AdifExporter
import com.ham.qso.domain.adif.AdifImporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LogbookUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val selectedSessionId: Long? = null, // null means all sessions
    val searchQuery: String = "",
    val bandFilter: Band? = null,
    val modeFilter: Mode? = null,
    val qsoList: List<QSOEntity> = emptyList(),
    val editingQso: QSOEntity? = null,
    val viewingQso: QSOEntity? = null,
    val exportShareContent: Pair<String, String>? = null, // Pair(filename, content)
    val infoMessage: String? = null
)

class LogbookViewModel(
    private val repository: QSORepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogbookUiState())
    val uiState: StateFlow<LogbookUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
            }
        }

        // 联合查询筛选 QSO 列表
        viewModelScope.launch {
            combine(
                repository.getAllQSOs(),
                _uiState.map { it.selectedSessionId }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged(),
                _uiState.map { it.bandFilter }.distinctUntilChanged(),
                _uiState.map { it.modeFilter }.distinctUntilChanged()
            ) { allQSOs, sessionId, query, band, mode ->
                allQSOs.filter { qso ->
                    (sessionId == null || qso.sessionId == sessionId) &&
                    (band == null || qso.band == band) &&
                    (mode == null || qso.mode == mode) &&
                    (query.isBlank() ||
                        qso.callsign.contains(query, ignoreCase = true) ||
                        qso.theirGrid.contains(query, ignoreCase = true) ||
                        qso.theirName.contains(query, ignoreCase = true) ||
                        qso.comment.contains(query, ignoreCase = true))
                }
            }.collect { filtered ->
                _uiState.update { it.copy(qsoList = filtered) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun onSessionFilterChanged(sessionId: Long?) = _uiState.update { it.copy(selectedSessionId = sessionId) }
    fun onBandFilterChanged(band: Band?) = _uiState.update { it.copy(bandFilter = band) }
    fun onModeFilterChanged(mode: Mode?) = _uiState.update { it.copy(modeFilter = mode) }

    fun onViewQso(qso: QSOEntity?) = _uiState.update { it.copy(viewingQso = qso) }
    fun onEditQso(qso: QSOEntity?) = _uiState.update { it.copy(editingQso = qso) }

    fun updateQso(qso: QSOEntity) {
        viewModelScope.launch {
            repository.updateQSO(qso)
            _uiState.update { it.copy(editingQso = null, infoMessage = "已更新通联记录: ${qso.callsign}") }
        }
    }

    fun deleteQso(qso: QSOEntity) {
        viewModelScope.launch {
            repository.deleteQSO(qso)
            _uiState.update {
                it.copy(
                    editingQso = null,
                    viewingQso = null,
                    infoMessage = "已删除通联: ${qso.callsign}"
                )
            }
        }
    }

    fun exportAdif() {
        val qsos = _uiState.value.qsoList
        if (qsos.isEmpty()) {
            _uiState.update { it.copy(infoMessage = "当前筛选列表无通联记录可导出") }
            return
        }
        val adifContent = AdifExporter.exportAdif(qsos)
        val filename = "FieldQSO_Log_${System.currentTimeMillis() / 1000}.adi"
        _uiState.update { it.copy(exportShareContent = Pair(filename, adifContent)) }
    }

    fun exportCsv() {
        val qsos = _uiState.value.qsoList
        if (qsos.isEmpty()) {
            _uiState.update { it.copy(infoMessage = "当前筛选列表无通联记录可导出") }
            return
        }
        val csvContent = AdifExporter.exportCsv(qsos)
        val filename = "FieldQSO_Log_${System.currentTimeMillis() / 1000}.csv"
        _uiState.update { it.copy(exportShareContent = Pair(filename, csvContent)) }
    }

    fun importAdifContent(content: String, targetSessionId: Long) {
        viewModelScope.launch {
            val parsed = AdifImporter.parseAdif(content, targetSessionId)
            if (parsed.isEmpty()) {
                _uiState.update { it.copy(infoMessage = "未能解析到有效 ADIF 记录") }
                return@launch
            }
            var count = 0
            for (qso in parsed) {
                repository.insertQSO(qso)
                count++
            }
            _uiState.update { it.copy(infoMessage = "成功导入 $count 条 ADIF 通联记录！") }
        }
    }

    fun clearExportShareContent() = _uiState.update { it.copy(exportShareContent = null) }
    fun dismissInfoMessage() = _uiState.update { it.copy(infoMessage = null) }
}
