package com.ham.qso.ui.screens.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.data.model.SessionEntity
import com.ham.qso.data.repository.QSORepository
import com.ham.qso.domain.adif.AdifExporter
import com.ham.qso.domain.adif.AdifImporter
import com.ham.qso.domain.utils.MaidenheadUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class HistoryUiState(
    val currentSession: SessionEntity? = null,
    val allQSOs: List<QSOEntity> = emptyList(),
    val filteredQSOs: List<QSOEntity> = emptyList(),
    val searchQuery: String = "",
    val filterBand: Band? = null,
    val filterMode: Mode? = null,
    val filterGridEmptyOnly: Boolean = false,

    // 编辑对话框状态
    val editingQSO: QSOEntity? = null,
    val editCallsign: String = "",
    val editRstSent: String = "",
    val editRstRcvd: String = "",
    val editBand: Band = Band.BAND_40M,
    val editMode: Mode = Mode.SSB,
    val editTheirGrid: String = "",
    val editTheirName: String = "",
    val editQth: String = "",
    val editAltitudeMeters: String = "",
    val editTheirRig: String = "",
    val editTheirAntenna: String = "",
    val editTheirPowerWatts: String = "",
    val editComment: String = "",

    val exportSuccessMessage: String? = null,
    val exportedFilePath: String? = null
)

class HistoryViewModel(
    private val repository: QSORepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentSession.collect { session ->
                _uiState.update { it.copy(currentSession = session) }
                if (session != null) {
                    observeQSOs(session.id)
                }
            }
        }
    }

    private fun observeQSOs(sessionId: Long) {
        viewModelScope.launch {
            repository.getQSOsForSession(sessionId).collect { list ->
                _uiState.update {
                    it.copy(
                        allQSOs = list,
                        filteredQSOs = applyFilter(list, it.searchQuery, it.filterBand, it.filterMode, it.filterGridEmptyOnly)
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredQSOs = applyFilter(it.allQSOs, query, it.filterBand, it.filterMode, it.filterGridEmptyOnly)
            )
        }
    }

    fun onFilterBandChange(band: Band?) {
        _uiState.update {
            it.copy(
                filterBand = band,
                filteredQSOs = applyFilter(it.allQSOs, it.searchQuery, band, it.filterMode, it.filterGridEmptyOnly)
            )
        }
    }

    fun onFilterModeChange(mode: Mode?) {
        _uiState.update {
            it.copy(
                filterMode = mode,
                filteredQSOs = applyFilter(it.allQSOs, it.searchQuery, it.filterBand, mode, it.filterGridEmptyOnly)
            )
        }
    }

    fun onToggleFilterGridEmptyOnly() {
        val newEmptyOnly = !_uiState.value.filterGridEmptyOnly
        _uiState.update {
            it.copy(
                filterGridEmptyOnly = newEmptyOnly,
                filteredQSOs = applyFilter(it.allQSOs, it.searchQuery, it.filterBand, it.filterMode, newEmptyOnly)
            )
        }
    }

    private fun applyFilter(
        list: List<QSOEntity>,
        query: String,
        band: Band?,
        mode: Mode?,
        gridEmptyOnly: Boolean
    ): List<QSOEntity> {
        return list.filter { qso ->
            val matchQuery = query.isBlank() ||
                    qso.callsign.contains(query, ignoreCase = true) ||
                    qso.theirGrid.contains(query, ignoreCase = true) ||
                    qso.qth.contains(query, ignoreCase = true) ||
                    qso.comment.contains(query, ignoreCase = true)
            val matchBand = band == null || qso.band == band
            val matchMode = mode == null || qso.mode == mode
            val matchGrid = !gridEmptyOnly || qso.theirGrid.isBlank()
            matchQuery && matchBand && matchMode && matchGrid
        }
    }

    // ── 编辑 QSO 对话框交互 ──────────────────────────────────

    fun openEditDialog(qso: QSOEntity) {
        _uiState.update {
            it.copy(
                editingQSO = qso,
                editCallsign = qso.callsign,
                editRstSent = qso.rstSent,
                editRstRcvd = qso.rstRcvd,
                editBand = qso.band,
                editMode = qso.mode,
                editTheirGrid = qso.theirGrid,
                editTheirName = qso.theirName,
                editQth = qso.qth,
                editAltitudeMeters = qso.altitudeMeters?.toString() ?: "",
                editTheirRig = qso.theirRig,
                editTheirAntenna = qso.theirAntenna,
                editTheirPowerWatts = qso.theirPowerWatts?.toString() ?: "",
                editComment = qso.comment
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(editingQSO = null) }
    }

    fun onEditCallsignChange(value: String) = _uiState.update { it.copy(editCallsign = value.uppercase().trim()) }
    fun onEditRstSentChange(value: String) = _uiState.update { it.copy(editRstSent = value) }
    fun onEditRstRcvdChange(value: String) = _uiState.update { it.copy(editRstRcvd = value) }
    fun onEditBandChange(band: Band) = _uiState.update { it.copy(editBand = band) }
    fun onEditModeChange(mode: Mode) = _uiState.update { it.copy(editMode = mode) }
    fun onEditTheirGridChange(value: String) = _uiState.update { it.copy(editTheirGrid = value.uppercase().trim()) }
    fun onEditTheirNameChange(value: String) = _uiState.update { it.copy(editTheirName = value) }
    fun onEditQthChange(value: String) = _uiState.update { it.copy(editQth = value) }
    fun onEditAltitudeChange(value: String) = _uiState.update { it.copy(editAltitudeMeters = value.filter { c -> c.isDigit() || c == '-' }) }
    fun onEditTheirRigChange(value: String) = _uiState.update { it.copy(editTheirRig = value) }
    fun onEditTheirAntennaChange(value: String) = _uiState.update { it.copy(editTheirAntenna = value) }
    fun onEditTheirPowerChange(value: String) = _uiState.update { it.copy(editTheirPowerWatts = value.filter { c -> c.isDigit() }) }
    fun onEditCommentChange(value: String) = _uiState.update { it.copy(editComment = value) }

    fun saveEditedQSO() {
        val s = _uiState.value
        val original = s.editingQSO ?: return

        val updated = original.copy(
            callsign = s.editCallsign.ifBlank { original.callsign },
            rstSent = s.editRstSent,
            rstRcvd = s.editRstRcvd,
            band = s.editBand,
            mode = s.editMode,
            theirGrid = s.editTheirGrid,
            theirName = s.editTheirName,
            qth = s.editQth,
            altitudeMeters = s.editAltitudeMeters.toIntOrNull(),
            theirRig = s.editTheirRig,
            theirAntenna = s.editTheirAntenna,
            theirPowerWatts = s.editTheirPowerWatts.toIntOrNull(),
            comment = s.editComment
        )

        viewModelScope.launch {
            repository.updateQSO(updated)
            _uiState.update { it.copy(editingQSO = null) }
        }
    }

    fun deleteQSO(qso: QSOEntity) {
        viewModelScope.launch {
            repository.deleteQSO(qso)
        }
    }

    // ── 导出功能 ──────────────────────────────────────────────

    fun exportAdif(context: Context): File? {
        val qsos = _uiState.value.allQSOs
        if (qsos.isEmpty()) return null

        val myCall = _uiState.value.currentSession?.myCallsign ?: "QSO"
        val fileName = "FieldQSO_${myCall}_${System.currentTimeMillis() / 1000}.adi"
        val content = AdifExporter.exportAdif(qsos, myCall)

        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            _uiState.update { it.copy(exportedFilePath = file.absolutePath, exportSuccessMessage = "ADIF 已生成 (${qsos.size} 条)") }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun exportCsv(context: Context): File? {
        val qsos = _uiState.value.allQSOs
        if (qsos.isEmpty()) return null

        val myCall = _uiState.value.currentSession?.myCallsign ?: "QSO"
        val fileName = "FieldQSO_${myCall}_${System.currentTimeMillis() / 1000}.csv"
        val content = AdifExporter.exportCsv(qsos)

        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            _uiState.update { it.copy(exportedFilePath = file.absolutePath, exportSuccessMessage = "CSV 已生成 (${qsos.size} 条)") }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun importAdifFromUri(context: Context, uri: Uri) {
        val sessionId = _uiState.value.currentSession?.id ?: return
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
                val imported = AdifImporter.parseAdif(content, sessionId)
                for (qso in imported) {
                    repository.insertQSO(qso)
                }
                _uiState.update { it.copy(exportSuccessMessage = "成功导入 ${imported.size} 条 QSO 记录") }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportSuccessMessage = "导入失败: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(exportSuccessMessage = null) }
    }
}

class HistoryViewModelFactory(
    private val repository: QSORepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
