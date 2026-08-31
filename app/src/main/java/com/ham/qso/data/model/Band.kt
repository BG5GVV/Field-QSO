package com.ham.qso.data.model

/**
 * 波段定义 (Band)
 */
enum class Band(val label: String, val frequencyMhz: Double) {
    BAND_160M("160m", 1.850),
    BAND_80M("80m", 3.700),
    BAND_60M("60m", 5.357),
    BAND_40M("40m", 7.050),
    BAND_30M("30m", 10.115),
    BAND_20M("20m", 14.200),
    BAND_17M("17m", 18.130),
    BAND_15M("15m", 21.200),
    BAND_12M("12m", 24.940),
    BAND_10M("10m", 28.500),
    BAND_6M("6m", 50.200),
    BAND_4M("4m", 70.200),
    BAND_2M("2m", 144.200),
    BAND_70CM("70cm", 432.200)
}
