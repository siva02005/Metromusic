package com.metromusic.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.metromusic.app.domain.repository.SettingsRepository
import com.metromusic.app.domain.repository.SpatialPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "metromusic_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val VOLUME_BOOST = floatPreferencesKey("volume_boost_level")
        val SPATIAL_AUDIO_ENABLED = booleanPreferencesKey("spatial_audio_enabled")
        val SPATIAL_PRESET = intPreferencesKey("spatial_preset")
        val EQUALIZER_PRESET = intPreferencesKey("equalizer_preset")
    }

    override suspend fun getVolumeBoostLevel(): Float {
        return context.dataStore.data.map { it[Keys.VOLUME_BOOST] ?: 1.0f }.first()
    }

    override suspend fun setVolumeBoostLevel(level: Float) {
        context.dataStore.edit { it[Keys.VOLUME_BOOST] = level.coerceIn(0f, 2f) }
    }

    override suspend fun isSpatialAudioEnabled(): Boolean {
        return context.dataStore.data.map { it[Keys.SPATIAL_AUDIO_ENABLED] ?: false }.first()
    }

    override suspend fun setSpatialAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SPATIAL_AUDIO_ENABLED] = enabled }
    }

    override suspend fun getSpatialAudioPreset(): SpatialPreset {
        return context.dataStore.data.map {
            when (it[Keys.SPATIAL_PRESET] ?: 0) {
                1 -> SpatialPreset.JBL_STAGE
                2 -> SpatialPreset.DOLBY_ATMOS
                else -> SpatialPreset.OFF
            }
        }.first()
    }

    override suspend fun setSpatialAudioPreset(preset: SpatialPreset) {
        context.dataStore.edit {
            it[Keys.SPATIAL_PRESET] = when (preset) {
                SpatialPreset.OFF -> 0
                SpatialPreset.JBL_STAGE -> 1
                SpatialPreset.DOLBY_ATMOS -> 2
            }
        }
    }

    override suspend fun getEqualizerPreset(): Int {
        return context.dataStore.data.map { it[Keys.EQUALIZER_PRESET] ?: 0 }.first()
    }

    override suspend fun setEqualizerPreset(preset: Int) {
        context.dataStore.edit { it[Keys.EQUALIZER_PRESET] = preset }
    }
}
