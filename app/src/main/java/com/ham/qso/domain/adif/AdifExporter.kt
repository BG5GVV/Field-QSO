package com.ham.qso.domain.adif

import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode
import com.ham.qso.data.model.QSOEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ADIF 3.1.4 标准格式导出器
 *
 * 参考标准：http://adif.org/314/ADIF_314.htm
 * 支持导出为 .adi 文件（ADIF）和 .csv 文件。
 */
object AdifExporter {

    private val utcFormat = SimpleDateFormat("HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** 生成 ADIF 3.1.4 格式字符串 */
    fun exportAdif(qsos: List<QSOEntity>, myCallsign: String = ""): String {
        val sb = StringBuilder()
        // Header
        sb.appendLine("Field QSO Log — exported by Field QSO App (Android)")
        sb.appendLine("Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())} UTC")
        sb.appendLine("<ADIF_VER:5>3.1.4")
        sb.appendLine("<PROGRAMID:9>FieldQSO")
        sb.appendLine("<EOH>")
        sb.appendLine()

        for (qso in qsos) {
            val ts = Date(qso.timestampUtc)
            sb.append(adifField("CALL", qso.callsign))
            sb.append(adifField("QSO_DATE", dateFormat.format(ts)))
            sb.append(adifField("TIME_ON", utcFormat.format(ts)))
            sb.append(adifField("BAND", qso.band.label))
            sb.append(adifField("MODE", qso.mode.label))
            sb.append(adifField("FREQ", "%.4f".format(qso.frequencyMhz)))
            sb.append(adifField("RST_SENT", qso.rstSent))
            sb.append(adifField("RST_RCVD", qso.rstRcvd))
            if (qso.myCallsign.isNotBlank()) sb.append(adifField("STATION_CALLSIGN", qso.myCallsign))
            if (qso.myGrid.isNotBlank()) sb.append(adifField("MY_GRIDSQUARE", qso.myGrid))
            if (qso.theirGrid.isNotBlank()) sb.append(adifField("GRIDSQUARE", qso.theirGrid))
            if (qso.theirName.isNotBlank()) sb.append(adifField("NAME", qso.theirName))
            if (qso.qth.isNotBlank()) sb.append(adifField("QTH", qso.qth))
            if (qso.altitudeMeters != null) sb.append(adifField("ALTITUDE", qso.altitudeMeters.toString()))
            if (qso.theirRig.isNotBlank()) sb.append(adifField("RIG", qso.theirRig))
            if (qso.theirAntenna.isNotBlank()) sb.append(adifField("ANTENNA", qso.theirAntenna))
            if (qso.theirPowerWatts != null) sb.append(adifField("RX_PWR", qso.theirPowerWatts.toString()))
            if (qso.txPowerWatts > 0) sb.append(adifField("TX_PWR", qso.txPowerWatts.toString()))
            if (qso.potaRef.isNotBlank()) sb.append(adifField("POTA_REF", qso.potaRef))
            if (qso.sotaRef.isNotBlank()) sb.append(adifField("SOTA_REF", qso.sotaRef))
            if (qso.comment.isNotBlank()) sb.append(adifField("COMMENT", qso.comment))
            sb.appendLine("<EOR>")
            sb.appendLine()
        }
        return sb.toString()
    }

    /** 生成 CSV 格式字符串 */
    fun exportCsv(qsos: List<QSOEntity>): String {
        val sb = StringBuilder()
        sb.appendLine(
            "Date(UTC),Time(UTC),Call,Band,Mode,Freq(MHz),RSTSent,RSTRcvd," +
            "TheirGrid,TheirName,QTH,Altitude(m),TheirRig,TheirAntenna,TheirPower(W)," +
            "MyCall,MyGrid,TxPower(W),POTA,SOTA,Comment"
        )
        for (qso in qsos) {
            val ts = Date(qso.timestampUtc)
            sb.appendLine(
                "${dateFormat.format(ts)},${utcFormat.format(ts)}," +
                "${csv(qso.callsign)},${qso.band.label},${qso.mode.label}," +
                "${"%.4f".format(qso.frequencyMhz)},${qso.rstSent},${qso.rstRcvd}," +
                "${csv(qso.theirGrid)},${csv(qso.theirName)},${csv(qso.qth)}," +
                "${qso.altitudeMeters ?: ""},${csv(qso.theirRig)},${csv(qso.theirAntenna)}," +
                "${qso.theirPowerWatts ?: ""}," +
                "${csv(qso.myCallsign)},${csv(qso.myGrid)},${qso.txPowerWatts}," +
                "${csv(qso.potaRef)},${csv(qso.sotaRef)},${csv(qso.comment)}"
            )
        }
        return sb.toString()
    }

    // ── helpers ───────────────────────────────────────────────────

    private fun adifField(tag: String, value: String): String {
        if (value.isBlank()) return ""
        return "<$tag:${value.length}>$value "
    }

    private fun csv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
