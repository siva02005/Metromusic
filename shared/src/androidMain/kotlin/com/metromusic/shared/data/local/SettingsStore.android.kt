package com.metromusic.shared.data.local

import android.content.Context
import android.content.SharedPreferences

actual class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("metromusic_settings", Context.MODE_PRIVATE)

    actual fun getThemeMode(): String = prefs.getString("theme_mode", "system") ?: "system"
    actual fun setThemeMode(mode: String) { prefs.edit().putString("theme_mode", mode).apply() }
    actual fun getPipedInstance(): String = prefs.getString("piped_instance", "") ?: ""
    actual fun setPipedInstance(url: String) { prefs.edit().putString("piped_instance", url).apply() }
    actual fun getVolumeBoost(): Boolean = prefs.getBoolean("volume_boost", false)
    actual fun setVolumeBoost(enabled: Boolean) { prefs.edit().putBoolean("volume_boost", enabled).apply() }
    actual fun getSpatialAudioPreset(): String = prefs.getString("spatial_preset", "OFF") ?: "OFF"
    actual fun setSpatialAudioPreset(preset: String) { prefs.edit().putString("spatial_preset", preset).apply() }
}
