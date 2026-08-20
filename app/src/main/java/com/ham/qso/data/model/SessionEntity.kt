package com.ham.qso.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 架台/活动会话 (Session)
 * 每次出站操作对应一个 Session，QSO 记录归属于 Session。
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 会话名称，例如 "莲花山 POTA CN-0123" */
    val name: String,

    /** 我的呼号 */
    val myCallsign: String = "",

    /** 我的梅登黑德网格定位符（4位或6位）*/
    val myGrid: String = "",

    /** 我的 QTH 名称描述 */
    val myQth: String = "",

    /** 发射功率（W）*/
    val txPowerWatts: Int = 100,

    /** 电台设备型号 */
    val rigModel: String = "",

    /** 天线描述 */
    val antenna: String = "",

    /** POTA 参考编号（可选）*/
    val potaRef: String = "",

    /** SOTA 参考编号（可选）*/
    val sotaRef: String = "",

    /** WWFF/BOTA 参考编号（可选）*/
    val wwffRef: String = "",

    /** 当前是否为活动会话 */
    val isCurrent: Boolean = false,

    /** 会话创建时间（UTC 毫秒）*/
    val createdAt: Long = System.currentTimeMillis()
)
