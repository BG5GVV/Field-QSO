package com.ham.qso

import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.domain.model.QCodeData
import org.junit.Assert.*
import org.junit.Test

class DupeCheckAndDomainTest {

    @Test
    fun testQCodeSearch() {
        val qthResult = QCodeData.search("QTH")
        assertTrue(qthResult.isNotEmpty())
        assertEquals("QTH", qthResult.first().code)

        val greetingResult = QCodeData.search("73")
        assertTrue(greetingResult.isNotEmpty())
        assertEquals("73", greetingResult.first().code)

        val powerResult = QCodeData.search("功率")
        assertTrue(powerResult.any { it.code == "QRO" || it.code == "QRP" })
    }

    @Test
    fun testQsoEntityDefaults() {
        val qso = QSOEntity(
            sessionId = 1,
            callsign = "BH4XYZ",
            band = Band.BAND_40M,
            mode = Mode.SSB
        )
        assertEquals("59", qso.rstSent)
        assertEquals("59", qso.rstRcvd)
        assertEquals(100, qso.txPowerWatts)
        assertEquals("BH4XYZ", qso.callsign)
    }
}
