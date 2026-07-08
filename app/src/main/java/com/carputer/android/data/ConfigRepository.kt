package com.carputer.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("carputer_config", Context.MODE_PRIVATE)

    var videoPort: Int
        get() = prefs.getInt("video_port", 5600)
        set(v) = prefs.edit().putInt("video_port", v.coerceIn(1, 65535)).apply()

    var telemetryPort: Int
        get() = prefs.getInt("telemetry_port", 14551)
        set(v) = prefs.edit().putInt("telemetry_port", v.coerceIn(1, 65535)).apply()

    var dvrDirectory: String
        get() = prefs.getString("dvr_directory", "/storage/emulated/0/dvr") ?: "/storage/emulated/0/dvr"
        set(v) = prefs.edit().putString("dvr_directory", v).apply()

    var lastPage: Int
        get() = prefs.getInt("last_page", 1)
        set(v) = prefs.edit().putInt("last_page", v.coerceIn(1, 5)).apply()

    var targetTemp: Int
        get() = prefs.getInt("target_temp", 72)
        set(v) = prefs.edit().putInt("target_temp", v.coerceIn(60, 85)).apply()

    var fanSpeed: Int
        get() = prefs.getInt("fan_speed", 0)
        set(v) = prefs.edit().putInt("fan_speed", v.coerceIn(0, 5)).apply()

    var hvacEnabled: Boolean
        get() = prefs.getBoolean("hvac_enabled", false)
        set(v) = prefs.edit().putBoolean("hvac_enabled", v).apply()

    var acEnabled: Boolean
        get() = prefs.getBoolean("ac_enabled", false)
        set(v) = prefs.edit().putBoolean("ac_enabled", v).apply()

    var autoMode: Boolean
        get() = prefs.getBoolean("auto_mode", true)
        set(v) = prefs.edit().putBoolean("auto_mode", v).apply()

    var recirculate: Boolean
        get() = prefs.getBoolean("recirculate", false)
        set(v) = prefs.edit().putBoolean("recirculate", v).apply()

    var backgroundImage: String
        get() = prefs.getString("background_image", "") ?: ""
        set(v) = prefs.edit().putString("background_image", v).apply()

    var backgroundOpacity: Float
        get() = prefs.getFloat("background_opacity", 0.3f)
        set(v) = prefs.edit().putFloat("background_opacity", v.coerceIn(0f, 1f)).apply()

    var audioVolume: Int
        get() = prefs.getInt("audio_volume", 80)
        set(v) = prefs.edit().putInt("audio_volume", v.coerceIn(0, 100)).apply()

    var repeatMode: Int
        get() = prefs.getInt("repeat_mode", 0)
        set(v) = prefs.edit().putInt("repeat_mode", v.coerceIn(0, 2)).apply()

    var shuffleOn: Boolean
        get() = prefs.getBoolean("shuffle_on", false)
        set(v) = prefs.edit().putBoolean("shuffle_on", v).apply()

    fun saveTheme(name: String, accentHex: String, gaugeBorderMode: String) {
        prefs.edit()
            .putString("theme", name)
            .putString("accent_color", accentHex)
            .putString("gauge_border_mode", gaugeBorderMode)
            .apply()
    }

    fun loadTheme(onLoaded: (name: String, accentHex: String, borderMode: String) -> Unit) {
        val name = prefs.getString("theme", "Dark") ?: "Dark"
        val accent = prefs.getString("accent_color", "#00A8E8") ?: "#00A8E8"
        val border = prefs.getString("gauge_border_mode", "gauge") ?: "gauge"
        onLoaded(name, accent, border)
    }
}
