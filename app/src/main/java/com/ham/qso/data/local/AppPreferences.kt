package com.ham.qso.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ham.qso.data.model.Band
import com.ham.qso.data.model.Mode

/**
 * 用户本地偏好设置管理 (SharedPreferences)
 *
 * 用于持久化保存极速录入界面最后选择的波段、模式、频率等用户偏好。
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "field_qso_prefs"
        private const val KEY_LAST_BAND = "last_selected_band"
        private const val KEY_LAST_MODE = "last_selected_mode"
        private const val KEY_LAST_FREQUENCY = "last_entered_frequency"
    }

    /**
     * 上次选择的波段，缺省为 40m (BAND_40M)
     */
    var lastBand: Band
        get() {
            val name = prefs.getString(KEY_LAST_BAND, Band.BAND_40M.name) ?: Band.BAND_40M.name
            return try {
                Band.valueOf(name)
            } catch (e: Exception) {
                // 兼容可能存储了 label 的情况
                Band.entries.find { it.name == name || it.label.equals(name, ignoreCase = true) }
                    ?: Band.BAND_40M
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LAST_BAND, value.name).apply()
        }

    /**
     * 上次选择的模式，缺省为 SSB
     */
    var lastMode: Mode
        get() {
            val name = prefs.getString(KEY_LAST_MODE, Mode.SSB.name) ?: Mode.SSB.name
            return try {
                Mode.valueOf(name)
            } catch (e: Exception) {
                Mode.entries.find { it.name == name || it.label.equals(name, ignoreCase = true) }
                    ?: Mode.SSB
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LAST_MODE, value.name).apply()
        }

    /**
     * 上次输入的频率 (MHz 字符串)，缺省为 "7.050"
     */
    var lastFrequencyMhz: String
        get() {
            val freq = prefs.getString(KEY_LAST_FREQUENCY, "7.050")
            return if (freq.isNullOrBlank()) "%.3f".format(lastBand.frequencyMhz) else freq
        }
        set(value) {
            if (value.isNotBlank()) {
                prefs.edit().putString(KEY_LAST_FREQUENCY, value.trim()).apply()
            }
        }
}
