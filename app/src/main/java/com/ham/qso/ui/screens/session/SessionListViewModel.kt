package com.ham.qso.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ham.qso.data.model.SessionEntity
import com.ham.qso.data.repository.QSORepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionListUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val currentSession: SessionEntity? = null,
    val isCreatingNew: Boolean = false,

    // 创建表单状态
    val formName: String = "",
    val formMyCall: String = "",
    val formMyGrid: String = "",
    val formMyQth: String = "",
    val formTxPower: String = "100",
    val formRig: String = "",
    val formAntenna: String = "",
    val formPota: String = "",
    val formSota: String = ""
)

class SessionListViewModel(
    private val repository: QSORepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSessions.collect { list ->
                _uiState.update { it.copy(sessions = list) }
            }
        }
        viewModelScope.launch {
            repository.currentSession.collect { cur ->
                _uiState.update { it.copy(currentSession = cur) }
            }
        }
    }

    fun openCreateDialog() {
        val cur = _uiState.value.currentSession
        _uiState.update {
            it.copy(
                isCreatingNew = true,
                formName = "",
                formMyCall = cur?.myCallsign ?: "",
                formMyGrid = cur?.myGrid ?: "",
                formMyQth = cur?.myQth ?: "",
                formTxPower = (cur?.txPowerWatts ?: 100).toString(),
                formRig = cur?.rigModel ?: "",
                formAntenna = cur?.antenna ?: "",
                formPota = "",
                formSota = ""
            )
        }
    }

    fun dismissCreateDialog() = _uiState.update { it.copy(isCreatingNew = false) }

    fun onFormNameChange(v: String) = _uiState.update { it.copy(formName = v) }
    fun onFormMyCallChange(v: String) = _uiState.update { it.copy(formMyCall = v.uppercase().trim()) }
    fun onFormMyGridChange(v: String) = _uiState.update { it.copy(formMyGrid = v.uppercase().trim()) }
    fun onFormMyQthChange(v: String) = _uiState.update { it.copy(formMyQth = v) }
    fun onFormTxPowerChange(v: String) = _uiState.update { it.copy(formTxPower = v.filter { c -> c.isDigit() }) }
    fun onFormRigChange(v: String) = _uiState.update { it.copy(formRig = v) }
    fun onFormAntennaChange(v: String) = _uiState.update { it.copy(formAntenna = v) }
    fun onFormPotaChange(v: String) = _uiState.update { it.copy(formPota = v.uppercase().trim()) }
    fun onFormSotaChange(v: String) = _uiState.update { it.copy(formSota = v.uppercase().trim()) }

    fun createSession() {
        val s = _uiState.value
        if (s.formName.isBlank()) return

        viewModelScope.launch {
            val session = SessionEntity(
                name = s.formName.trim(),
                myCallsign = s.formMyCall,
                myGrid = s.formMyGrid,
                myQth = s.formMyQth,
                txPowerWatts = s.formTxPower.toIntOrNull() ?: 100,
                rigModel = s.formRig,
                antenna = s.formAntenna,
                potaRef = s.formPota,
                sotaRef = s.formSota,
                isCurrent = true
            )
            val newId = repository.insertSession(session)
            repository.setCurrentSession(newId)
            _uiState.update { it.copy(isCreatingNew = false) }
        }
    }

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            repository.setCurrentSession(sessionId)
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }
}

class SessionListViewModelFactory(
    private val repository: QSORepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
