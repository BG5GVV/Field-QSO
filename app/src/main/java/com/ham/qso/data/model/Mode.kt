package com.ham.qso.data.model

/**
 * 通联模式定义 (Mode)
 */
enum class Mode(val label: String) {
    SSB("SSB"),
    CW("CW"),
    FT8("FT8"),
    FT4("FT4"),
    FM("FM"),
    AM("AM"),
    RTTY("RTTY"),
    PSK31("PSK31"),
    DSTAR("D-STAR"),
    DMR("DMR"),
    C4FM("C4FM")
}
