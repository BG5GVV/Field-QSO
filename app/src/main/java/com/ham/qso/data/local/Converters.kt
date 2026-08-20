package com.ham.qso.data.local

import androidx.room.TypeConverter
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode

/**
 * Room TypeConverters for enum types
 */
class Converters {

    @TypeConverter
    fun fromBand(band: Band): String = band.name

    @TypeConverter
    fun toBand(value: String): Band = try {
        Band.valueOf(value)
    } catch (e: IllegalArgumentException) {
        Band.BAND_40M
    }

    @TypeConverter
    fun fromMode(mode: Mode): String = mode.name

    @TypeConverter
    fun toMode(value: String): Mode = try {
        Mode.valueOf(value)
    } catch (e: IllegalArgumentException) {
        Mode.SSB
    }
}
