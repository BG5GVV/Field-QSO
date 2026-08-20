package com.ham.qso.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通联日志实体 (QSO)
 *
 * 需求变更记录：
 * - v2: 新增 timeZoneId, qth, altitudeMeters, theirRig, theirAntenna, theirPowerWatts
 *       支持 timestampUtc 手动设定（非强制取 System.currentTimeMillis()）
 *       theirGrid 允许留空，支持后期编辑换算
 */
@Entity(
    tableName = "qso_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("callsign"),
        Index("timestampUtc")
    ]
)
data class QSOEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,

    // ── 基本通联信息 ──────────────────────────────────────────
    val callsign: String,
    val rstSent: String = "59",
    val rstRcvd: String = "59",

    // ── 时间（UTC毫秒）& 时区 ─────────────────────────────────
    /** 通联时间（UTC毫秒），支持手动设定 */
    val timestampUtc: Long = System.currentTimeMillis(),
    /** 时区ID，例如 "UTC" / "Asia/Shanghai" / "Asia/Tokyo" */
    val timeZoneId: String = "UTC",

    // ── 对方信息 ───────────────────────────────────────────────
    /** 对方梅登黑德网格（允许为空，可后期编辑）*/
    val theirGrid: String = "",
    val theirName: String = "",

    /** 对方 QTH 描述文字（可先在此临时记录，后期换算为 Grid）*/
    val qth: String = "",

    /** 海拔高度（米），可选 */
    val altitudeMeters: Int? = null,

    /** 对方电台设备型号 */
    val theirRig: String = "",

    /** 对方天线类型 */
    val theirAntenna: String = "",

    /** 对方发射功率（W），可选 */
    val theirPowerWatts: Int? = null,

    /** 备注（可先临时记录 QTH 文字）*/
    val comment: String = "",

    // ── 频段与模式 ─────────────────────────────────────────────
    val band: Band = Band.BAND_40M,
    val mode: Mode = Mode.SSB,
    val frequencyMhz: Double = 7.050,

    // ── 我方信息（继承自 Session，可独立覆盖）─────────────────
    val myCallsign: String = "",
    val myGrid: String = "",
    val potaRef: String = "",
    val sotaRef: String = "",
    val txPowerWatts: Int = 100
)
