package com.ham.qso

import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.domain.adif.AdifExporter
import com.ham.qso.domain.adif.AdifImporter
import com.ham.qso.domain.utils.MaidenheadUtils
import org.junit.Assert.*
import org.junit.Test

class MaidenheadAndAdifTest {

    @Test
    fun testLatLonToGrid() {
        // 深圳 (22.54°N, 114.05°E) -> OL72
        val grid4 = MaidenheadUtils.latLonToGrid(22.54, 114.05, 4)
        assertEquals("OL72", grid4)

        // 北京 (39.90°N, 116.40°E) -> OM89
        val gridBeijing = MaidenheadUtils.latLonToGrid(39.90, 116.40, 4)
        assertEquals("OM89", gridBeijing)

        // 东京 (35.68°N, 139.76°E) -> PM95
        val gridTokyo = MaidenheadUtils.latLonToGrid(35.68, 139.76, 4)
        assertEquals("PM95", gridTokyo)
    }

    @Test
    fun testGridToLatLon() {
        val latLon = MaidenheadUtils.gridToLatLon("OL72")
        assertNotNull(latLon)
        val (lat, lon) = latLon!!
        assertTrue(lat in 22.0..23.0)
        assertTrue(lon in 114.0..116.0)
    }

    @Test
    fun testDistanceAndBearing() {
        // 深圳(OL72) 到 东京(PM95) 距离大约 2700-3000 公里
        val distance = MaidenheadUtils.distanceKm("OL72", "PM95")
        assertNotNull(distance)
        assertTrue(distance!! in 2700.0..3100.0)

        // 深圳 到 东京 方位角在东北方向 (40° ~ 60°)
        val bearing = MaidenheadUtils.bearingDeg("OL72", "PM95")
        assertNotNull(bearing)
        assertTrue(bearing!! in 40.0..60.0)
    }

    @Test
    fun testGridValidation() {
        assertTrue(MaidenheadUtils.isValidGrid("OL72"))
        assertTrue(MaidenheadUtils.isValidGrid("OL72ab"))
        assertTrue(MaidenheadUtils.isValidGrid("ol72ab"))
        assertFalse(MaidenheadUtils.isValidGrid("OL7"))
        assertFalse(MaidenheadUtils.isValidGrid("OL721"))
        assertFalse(MaidenheadUtils.isValidGrid("ZZ99"))
    }

    @Test
    fun testAdifExportAndImportRoundTrip() {
        val originalQso = QSOEntity(
            id = 1,
            sessionId = 100,
            callsign = "JA1ABC",
            rstSent = "599",
            rstRcvd = "589",
            timestampUtc = 1724000000000L,
            timeZoneId = "Asia/Tokyo",
            theirGrid = "PM95",
            theirName = "Ken",
            qth = "Tokyo Chiyoda",
            altitudeMeters = 50,
            theirRig = "IC-705",
            theirAntenna = "Dipole",
            theirPowerWatts = 10,
            comment = "Nice 40m CW QSO",
            band = Band.BAND_40M,
            mode = Mode.CW,
            frequencyMhz = 7.025,
            myCallsign = "BH4XXX",
            myGrid = "OL72ab",
            potaRef = "CN-0123",
            txPowerWatts = 100
        )

        // 1. 导出为 ADIF 字符串
        val adifString = AdifExporter.exportAdif(listOf(originalQso), "BH4XXX")
        assertTrue(adifString.contains("<CALL:6>JA1ABC"))
        assertTrue(adifString.contains("<BAND:3>40m"))
        assertTrue(adifString.contains("<MODE:2>CW"))
        assertTrue(adifString.contains("<QTH:13>Tokyo Chiyoda"))
        assertTrue(adifString.contains("<RIG:6>IC-705"))
        assertTrue(adifString.contains("<ANTENNA:6>Dipole"))
        assertTrue(adifString.contains("<RX_PWR:2>10"))
        assertTrue(adifString.contains("<ALTITUDE:2>50"))

        // 2. 解析回 QSOEntity 列表
        val parsedList = AdifImporter.parseAdif(adifString, sessionId = 200)
        assertEquals(1, parsedList.size)

        val parsed = parsedList.first()
        assertEquals("JA1ABC", parsed.callsign)
        assertEquals("599", parsed.rstSent)
        assertEquals("589", parsed.rstRcvd)
        assertEquals(Band.BAND_40M, parsed.band)
        assertEquals(Mode.CW, parsed.mode)
        assertEquals("PM95", parsed.theirGrid)
        assertEquals("Ken", parsed.theirName)
        assertEquals("Tokyo Chiyoda", parsed.qth)
        assertEquals(50, parsed.altitudeMeters)
        assertEquals("IC-705", parsed.theirRig)
        assertEquals("Dipole", parsed.theirAntenna)
        assertEquals(10, parsed.theirPowerWatts)
        assertEquals("Nice 40m CW QSO", parsed.comment)
        assertEquals("BH4XXX", parsed.myCallsign)
        assertEquals("OL72ab", parsed.myGrid)
        assertEquals("CN-0123", parsed.potaRef)
    }

    @Test
    fun testCsvExport() {
        val qso = QSOEntity(
            sessionId = 1,
            callsign = "BA4ABC",
            rstSent = "59",
            rstRcvd = "59",
            band = Band.BAND_20M,
            mode = Mode.SSB,
            frequencyMhz = 14.200,
            theirGrid = "OM89",
            qth = "Beijing Haidian",
            altitudeMeters = 80,
            theirRig = "FT-891",
            theirAntenna = "PAC-12",
            theirPowerWatts = 50,
            comment = "Field test, 73!"
        )

        val csv = AdifExporter.exportCsv(listOf(qso))
        assertTrue(csv.contains("Date(UTC),Time(UTC)"))
        assertTrue(csv.contains("BA4ABC"))
        assertTrue(csv.contains("20m"))
        assertTrue(csv.contains("SSB"))
        assertTrue(csv.contains("Beijing Haidian"))
        assertTrue(csv.contains("FT-891"))
        assertTrue(csv.contains("PAC-12"))
        assertTrue(csv.contains("50"))
        assertTrue(csv.contains("\"Field test, 73!\""))
    }
}
