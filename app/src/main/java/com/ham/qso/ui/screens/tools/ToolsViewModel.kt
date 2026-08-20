package com.ham.qso.ui.screens.tools

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ham.qso.data.repository.QSORepository
import com.ham.qso.domain.model.QCodeData
import com.ham.qso.domain.model.QCodeItem
import com.ham.qso.domain.utils.MaidenheadUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolsUiState(
    // GPS & Grid Locator
    val currentLat: Double? = null,
    val currentLon: Double? = null,
    val grid4: String = "",
    val grid6: String = "",
    val isLocating: Boolean = false,
    val locationError: String? = null,

    // Distance & Bearing Calculator
    val calcFromGrid: String = "OL72ab",
    val calcToGrid: String = "PM95",
    val distanceKm: Double? = null,
    val bearingDeg: Double? = null,
    val isCalcValid: Boolean = true,

    // Q-Code search
    val qCodeQuery: String = "",
    val qCodeResults: List<QCodeItem> = QCodeData.qCodes,

    val infoMessage: String? = null
)

class ToolsViewModel(
    private val repository: QSORepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    init {
        // 自动计算初始示例网格
        recalculateDistanceAndBearing("OL72ab", "PM95")
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(context: Context) {
        _uiState.update { it.copy(isLocating = true, locationError = null) }
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val g4 = MaidenheadUtils.latLonToGrid(lat, lon, 4)
                    val g6 = MaidenheadUtils.latLonToGrid(lat, lon, 6)
                    _uiState.update {
                        it.copy(
                            currentLat = lat,
                            currentLon = lon,
                            grid4 = g4,
                            grid6 = g6,
                            isLocating = false,
                            calcFromGrid = g6,
                            infoMessage = "成功获取 GPS 定位: $g6"
                        )
                    }
                    recalculateDistanceAndBearing(g6, _uiState.value.calcToGrid)
                } else {
                    fallbackToSystemLocation(context)
                }
            }.addOnFailureListener {
                fallbackToSystemLocation(context)
            }
        } catch (e: Exception) {
            fallbackToSystemLocation(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToSystemLocation(context: Context) {
        try {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val location = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val g4 = MaidenheadUtils.latLonToGrid(lat, lon, 4)
                val g6 = MaidenheadUtils.latLonToGrid(lat, lon, 6)
                _uiState.update {
                    it.copy(
                        currentLat = lat,
                        currentLon = lon,
                        grid4 = g4,
                        grid6 = g6,
                        isLocating = false,
                        calcFromGrid = g6,
                        infoMessage = "成功通过系统定位更新网格: $g6"
                    )
                }
                recalculateDistanceAndBearing(g6, _uiState.value.calcToGrid)
            } else {
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        locationError = "无法获取 GPS 定位，请确认已开启位置服务及定位权限"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLocating = false,
                    locationError = "定位异常: ${e.localizedMessage ?: "未知错误"}"
                )
            }
        }
    }

    fun applyGpsGridToCurrentSession() {
        val grid = _uiState.value.grid6.ifBlank { _uiState.value.grid4 }
        if (grid.isBlank()) return

        viewModelScope.launch {
            val current = repository.getCurrentSessionDirect()
            if (current != null) {
                repository.updateSession(current.copy(myGrid = grid))
                _uiState.update { it.copy(infoMessage = "已将网格 $grid 应用至当前会话「${current.name}」") }
            } else {
                _uiState.update { it.copy(infoMessage = "暂无活跃会话，请先创建或激活会话") }
            }
        }
    }

    fun onCalcFromGridChanged(grid: String) {
        val g = grid.uppercase().trim()
        _uiState.update { it.copy(calcFromGrid = g) }
        recalculateDistanceAndBearing(g, _uiState.value.calcToGrid)
    }

    fun onCalcToGridChanged(grid: String) {
        val g = grid.uppercase().trim()
        _uiState.update { it.copy(calcToGrid = g) }
        recalculateDistanceAndBearing(_uiState.value.calcFromGrid, g)
    }

    private fun recalculateDistanceAndBearing(from: String, to: String) {
        if (MaidenheadUtils.isValidGrid(from) && MaidenheadUtils.isValidGrid(to)) {
            val dist = MaidenheadUtils.distanceKm(from, to)
            val bearing = MaidenheadUtils.bearingDeg(from, to)
            _uiState.update {
                it.copy(
                    distanceKm = dist,
                    bearingDeg = bearing,
                    isCalcValid = true
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    distanceKm = null,
                    bearingDeg = null,
                    isCalcValid = false
                )
            }
        }
    }

    fun onQCodeQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                qCodeQuery = query,
                qCodeResults = QCodeData.search(query)
            )
        }
    }

    fun dismissInfoMessage() = _uiState.update { it.copy(infoMessage = null) }
}
