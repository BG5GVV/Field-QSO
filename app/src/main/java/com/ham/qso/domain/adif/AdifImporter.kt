package com.ham.qso.domain.adif

import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * ADIF 3.1.4 格式导入解析器
 */
object AdifImporter {

    private val fieldPattern = Pattern.compile("<(\\w+):(\\d+)(?::(\\w+))?>([^<]*)", Pattern.CASE_INSENSITIVE)

    fun parseAdif(adifContent: String, sessionId: Long): List<QSOEntity> {
        val qsos = mutableListOf<QSOEntity>()

        // 分割 Header 与 Records (通过 <EOH>)
        val upper = adifContent.uppercase()
        val eohIndex = upper.indexOf("<EOH>")
        val recordsContent = if (eohIndex != -1) {
            adifContent.substring(eohIndex + 5)
        } else {
            adifContent
        }

        // 以 <EOR> 分割每条记录
        val records = recordsContent.split(Regex("(?i)<EOR>"))

        for (record in records) {
            val trimmed = record.trim()
            if (trimmed.isEmpty()) continue

            val fields = mutableMapOf<String, String>()
            val matcher = fieldPattern.matcher(trimmed)
            while (matcher.find()) {
                val name = matcher.group(1).uppercase()
                val length = matcher.group(2).toIntOrNull() ?: 0
                val rawVal = matcher.group(4)
                val value = if (rawVal.length >= length) rawVal.substring(0, length) else rawVal
                fields[name] = value.trim()
            }

            val callsign = fields["CALL"] ?: continue
            if (callsign.isBlank()) continue

            // 解析日期与时间
            val dateStr = fields["QSO_DATE"] ?: "" // YYYYMMDD
            val timeStr = fields["TIME_ON"] ?: fields["TIME_OFF"] ?: "000000" // HHMMSS or HHMM
            val timestampUtc = parseTimestamp(dateStr, timeStr)

            // 解析频段与模式
            val bandStr = fields["BAND"] ?: "40M"
            val band = parseBand(bandStr)
            val modeStr = fields["MODE"] ?: "SSB"
            val mode = parseMode(modeStr)
            val freqMhz = fields["FREQ"]?.toDoubleOrNull() ?: band.frequencyMhz

            val rstSent = fields["RST_SENT"] ?: "59"
            val rstRcvd = fields["RST_RCVD"] ?: "59"
            val theirGrid = fields["GRIDSQUARE"] ?: ""
            val theirName = fields["NAME"] ?: ""
            val qth = fields["QTH"] ?: ""
            val altitude = fields["ALTITUDE"]?.toIntOrNull()
            val theirRig = fields["RIG"] ?: ""
            val theirAntenna = fields["ANTENNA"] ?: ""
            val rxPwr = fields["RX_PWR"]?.toIntOrNull()
            val txPwr = fields["TX_PWR"]?.toIntOrNull() ?: 100
            val comment = fields["COMMENT"] ?: ""
            val myCall = fields["STATION_CALLSIGN"] ?: fields["OPERATOR"] ?: ""
            val myGrid = fields["MY_GRIDSQUARE"] ?: ""
            val potaRef = fields["POTA_REF"] ?: ""
            val sotaRef = fields["SOTA_REF"] ?: ""

            qsos.add(
                QSOEntity(
                    sessionId = sessionId,
                    callsign = callsign.uppercase(),
                    rstSent = rstSent,
                    rstRcvd = rstRcvd,
                    timestampUtc = timestampUtc,
                    timeZoneId = "UTC",
                    theirGrid = theirGrid.uppercase(),
                    theirName = theirName,
                    qth = qth,
                    altitudeMeters = altitude,
                    theirRig = theirRig,
                    theirAntenna = theirAntenna,
                    theirPowerWatts = rxPwr,
                    comment = comment,
                    band = band,
                    mode = mode,
                    frequencyMhz = freqMhz,
                    myCallsign = myCall,
                    myGrid = myGrid,
                    potaRef = potaRef,
                    sotaRef = sotaRef,
                    txPowerWatts = txPwr
                )
            )
        }
        return qsos
    }

    private fun parseTimestamp(dateStr: String, timeStr: String): Long {
        if (dateStr.length < 8) return System.currentTimeMillis()
        val cleanTime = when {
            timeStr.length >= 6 -> timeStr.substring(0, 6)
            timeStr.length >= 4 -> timeStr.substring(0, 4) + "00"
            else -> "000000"
        }
        val fullStr = "${dateStr}${cleanTime}"
        return try {
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(fullStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseBand(bandStr: String): Band {
        val clean = bandStr.uppercase().trim().replace("BAND_", "")
        return Band.values().find {
            it.name.replace("BAND_", "") == clean || it.label.uppercase() == clean
        } ?: Band.BAND_40M
    }

    private fun parseMode(modeStr: String): Mode {
        val clean = modeStr.uppercase().trim()
        return Mode.values().find {
            it.name == clean || it.label.uppercase() == clean
        } ?: Mode.SSB
    }
}
