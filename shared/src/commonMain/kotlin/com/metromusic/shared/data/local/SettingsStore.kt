package com.metromusic.shared.data.local

expect class SettingsStore {
    fun getThemeMode(): String
    fun setThemeMode(mode: String)
    fun getPipedInstance(): String
    fun setPipedInstance(url: String)
    fun getVolumeBoost(): Boolean
    fun setVolumeBoost(enabled: Boolean)
    fun getSpatialAudioPreset(): String
    fun setSpatialAudioPreset(preset: String)
}
