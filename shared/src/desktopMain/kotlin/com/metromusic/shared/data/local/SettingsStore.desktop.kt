package com.metromusic.shared.data.local

import java.io.File
import java.util.Properties

actual class SettingsStore(private val file: File) {
    private val props = Properties()

    init {
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }

    private fun save() {
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, "Metromusic Settings") }
    }

    actual fun getThemeMode(): String = props.getProperty("theme_mode", "system")
    actual fun setThemeMode(mode: String) { props.setProperty("theme_mode", mode); save() }
    actual fun getPipedInstance(): String = props.getProperty("piped_instance", "")
    actual fun setPipedInstance(url: String) { props.setProperty("piped_instance", url); save() }
    actual fun getVolumeBoost(): Boolean = props.getProperty("volume_boost", "false").toBoolean()
    actual fun setVolumeBoost(enabled: Boolean) { props.setProperty("volume_boost", enabled.toString()); save() }
    actual fun getSpatialAudioPreset(): String = props.getProperty("spatial_preset", "OFF")
    actual fun setSpatialAudioPreset(preset: String) { props.setProperty("spatial_preset", preset); save() }
}
