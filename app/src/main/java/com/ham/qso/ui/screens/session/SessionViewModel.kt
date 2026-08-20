package com.ham.qso.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ham.qso.data.model.SessionEntity
import com.ham.qso.data.repository.QSORepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionWithStats(
    val session: SessionEntity,
    val qsoCount: Int = 0,
    val uniqueCallCount: Int = 0
)

data class SessionUiState(
    val sessions: List<SessionWithStats> = emptyList(),
    val activeSession: SessionEntity? = null,
    val editingSession: SessionEntity? = null,
    val showCreateDialog: Boolean = false,
    val infoMessage: String? = null
)

class SessionViewModel(
    private val repository: QSORepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentSession.collect { active ->
                _uiState.update { it.copy(activeSession = active) }
            }
        }

        viewModelScope.launch {
            repository.allSessions.collect { sessionList ->
                loadSessionsWithStats(sessionList)
            }
        }
    }

    private suspend fun loadSessionsWithStats(sessionList: List<SessionEntity>) {
        val statsList = sessionList.map { session ->
            val qsos = repository.getQSOsForSessionDirect(session.id)
            SessionWithStats(
                session = session,
                qsoCount = qsos.size,
                uniqueCallCount = qsos.map { it.callsign.uppercase() }.distinct().size
            )
        }
        _uiState.update { it.copy(sessions = statsList) }
    }

    fun openCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun closeCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }

    fun openEditDialog(session: SessionEntity) = _uiState.update { it.copy(editingSession = session) }
    fun closeEditDialog() = _uiState.update { it.copy(editingSession = null) }

    fun saveNewSession(
        name: String,
        myCallsign: String,
        myGrid: String,
        myQth: String,
        txPowerWatts: Int,
        rigModel: String,
        antenna: String,
        potaRef: String,
        sotaRef: String,
        wwffRef: String,
        setAsCurrent: Boolean
    ) {
        viewModelScope.launch {
            val newSession = SessionEntity(
                name = name.ifBlank { "野台架台会话" },
                myCallsign = myCallsign.uppercase().trim(),
                myGrid = myGrid.uppercase().trim(),
                myQth = myQth.trim(),
                txPowerWatts = txPowerWatts,
                rigModel = rigModel.trim(),
                antenna = antenna.trim(),
                potaRef = potaRef.uppercase().trim(),
                sotaRef = sotaRef.uppercase().trim(),
                wwffRef = wwffRef.uppercase().trim(),
                isCurrent = setAsCurrent
            )
            val newId = repository.insertSession(newSession)
            if (setAsCurrent) {
                repository.setCurrentSession(newId)
            }
            _uiState.update {
                it.copy(
                    showCreateDialog = false,
                    infoMessage = "成功创建架台会话: $name"
                )
            }
        }
    }

    fun updateSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.updateSession(session)
            _uiState.update {
                it.copy(
                    editingSession = null,
                    infoMessage = "已更新会话: ${session.name}"
                )
            }
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            _uiState.update {
                it.copy(
                    editingSession = null,
                    infoMessage = "已删除会话: ${session.name}"
                )
            }
        }
    }

    fun activateSession(sessionId: Long) {
        viewModelScope.launch {
            repository.setCurrentSession(sessionId)
            _uiState.update { it.copy(infoMessage = "已切换当前活动架台") }
        }
    }

    fun dismissInfoMessage() = _uiState.update { it.copy(infoMessage = null) }
}
