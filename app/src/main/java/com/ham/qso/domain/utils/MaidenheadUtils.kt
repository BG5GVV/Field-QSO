package com.ham.qso.domain.utils

import kotlin.math.abs
import kotlin.math.floor

/**
 * 梅登黑德网格坐标换算工具 (Maidenhead Locator)
 *
 * 支持 4 位精度（如 OL72）和 6 位精度（如 OL72ab）
 */
object MaidenheadUtils {

    /**
     * 将经纬度转换为梅登黑德网格定位符
     * @param lat 纬度（-90 ~ +90）
     * @param lon 经度（-180 ~ +180）
     * @param precision 精度：4 或 6（默认6）
     */
    fun latLonToGrid(lat: Double, lon: Double, precision: Int = 6): String {
        require(lat in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Longitude must be between -180 and 180" }

        val adjLon = lon + 180.0
        val adjLat = lat + 90.0

        val field1 = ('A' + floor(adjLon / 20).toInt())
        val field2 = ('A' + floor(adjLat / 10).toInt())

        val square1 = ('0' + floor((adjLon % 20) / 2).toInt())
        val square2 = ('0' + floor((adjLat % 10) / 1).toInt())

        return if (precision >= 6) {
            val sub1 = ('A' + floor((adjLon % 2) / (2.0 / 24)).toInt())
            val sub2 = ('A' + floor((adjLat % 1) / (1.0 / 24)).toInt())
            "$field1$field2$square1$square2${sub1.lowercaseChar()}${sub2.lowercaseChar()}"
        } else {
            "$field1$field2$square1$square2"
        }
    }

    /**
     * 将梅登黑德网格换算为中心经纬度
     * @return Pair(lat, lon) or null if invalid
     */
    fun gridToLatLon(grid: String): Pair<Double, Double>? {
        val g = grid.uppercase().trim()
        if (g.length < 4) return null

        return try {
            val lon = (g[0] - 'A') * 20.0 - 180 +
                    (g[2] - '0') * 2.0 +
                    (if (g.length >= 6) (g[4] - 'A') * (2.0 / 24) + (1.0 / 24) else 1.0)
            val lat = (g[1] - 'A') * 10.0 - 90 +
                    (g[3] - '0') * 1.0 +
                    (if (g.length >= 6) (g[5] - 'A') * (1.0 / 24) + (0.5 / 24) else 0.5)
            Pair(lat, lon)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 计算两个网格之间的大圆距离（公里）
     */
    fun distanceKm(grid1: String, grid2: String): Double? {
        val (lat1, lon1) = gridToLatLon(grid1) ?: return null
        val (lat2, lon2) = gridToLatLon(grid2) ?: return null
        return haversineKm(lat1, lon1, lat2, lon2)
    }

    /**
     * 计算从 grid1 到 grid2 的方位角（度，0–360，北为0）
     */
    fun bearingDeg(grid1: String, grid2: String): Double? {
        val (lat1, lon1) = gridToLatLon(grid1) ?: return null
        val (lat2, lon2) = gridToLatLon(grid2) ?: return null
        return bearing(lat1, lon1, lat2, lon2)
    }

    // ── 内部计算工具 ─────────────────────────────────────────────

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.asin(Math.sqrt(a))
    }

    private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2))
        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon)
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360
    }

    /**
     * 校验梅登黑德网格格式是否有效（4位或6位）
     */
    fun isValidGrid(grid: String): Boolean {
        val g = grid.trim()
        if (g.length != 4 && g.length != 6) return false
        val gu = g.uppercase()
        if (gu[0] !in 'A'..'R') return false
        if (gu[1] !in 'A'..'R') return false
        if (gu[2] !in '0'..'9') return false
        if (gu[3] !in '0'..'9') return false
        if (g.length == 6) {
            if (gu[4] !in 'A'..'X') return false
            if (gu[5] !in 'A'..'X') return false
        }
        return true
    }
}
