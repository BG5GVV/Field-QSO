package com.ham.qso.ui.screens.tools

import androidx.lifecycle.ViewModel
import com.ham.qso.domain.utils.MaidenheadUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GridCalculatorUiState(
    // 经纬度 -> 网格
    val inputLat: String = "22.5431",
    val inputLon: String = "114.0579",
    val resultGrid4: String = "OL72",
    val resultGrid6: String = "OL72ab",

    // 网格 -> 距离与天线方位角
    val inputMyGrid: String = "OL72ab",
    val inputTargetGrid: String = "PM95",
    val calculatedDistanceKm: Double? = null,
    val calculatedBearingDeg: Double? = null
)

class GridCalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GridCalculatorUiState())
    val uiState: StateFlow<GridCalculatorUiState> = _uiState.asStateFlow()

    init {
        recalculateLatLonToGrid()
        recalculateDistanceAndBearing()
    }

    fun onLatChange(lat: String) {
        _uiState.update { it.copy(inputLat = lat) }
        recalculateLatLonToGrid()
    }

    fun onLonChange(lon: String) {
        _uiState.update { it.copy(inputLon = lon) }
        recalculateLatLonToGrid()
    }

    fun setCoordinates(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                inputLat = "%.4f".format(lat),
                inputLon = "%.4f".format(lon)
            )
        }
        recalculateLatLonToGrid()
    }

    private fun recalculateLatLonToGrid() {
        val s = _uiState.value
        val lat = s.inputLat.toDoubleOrNull()
        val lon = s.inputLon.toDoubleOrNull()
        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
            val g4 = MaidenheadUtils.latLonToGrid(lat, lon, 4)
            val g6 = MaidenheadUtils.latLonToGrid(lat, lon, 6)
            _uiState.update { it.copy(resultGrid4 = g4, resultGrid6 = g6) }
        }
    }

    fun onMyGridChange(g: String) {
        _uiState.update { it.copy(inputMyGrid = g.uppercase().trim()) }
        recalculateDistanceAndBearing()
    }

    fun onTargetGridChange(g: String) {
        _uiState.update { it.copy(inputTargetGrid = g.uppercase().trim()) }
        recalculateDistanceAndBearing()
    }

    private fun recalculateDistanceAndBearing() {
        val s = _uiState.value
        val dist = MaidenheadUtils.distanceKm(s.inputMyGrid, s.inputTargetGrid)
        val bear = MaidenheadUtils.bearingDeg(s.inputMyGrid, s.inputTargetGrid)
        _uiState.update {
            it.copy(
                calculatedDistanceKm = dist,
                calculatedBearingDeg = bear
            )
        }
    }
}
