package com.metromusic.app.service.audio.processor

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the entire Metromusic audio pipeline.
 * Manages the order and configuration of all custom AudioProcessors.
 *
 * Pipeline order (critical for correct signal flow):
 *   1. Input format conversion (Media3 internal)
 *   2. VolumeBoostProcessor (gain + DRC)
 *   3. SpatialAudioProcessor (JBL Stage / Dolby Atmos)
 *   4. Output format conversion (Media3 internal)
 *
 * This ordering ensures that spatial processing acts on the already-gained
 * signal, and the DRC in VolumeBoostProcessor prevents spatial processing
 * from introducing clipping artifacts.
 */
@Singleton
class MetromusicAudioPipeline @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val volumeBoost = VolumeBoostProcessor()
    val spatialAudio = SpatialAudioProcessor()

    fun getProcessors(): List<AudioProcessor> = listOf(
        volumeBoost,
        spatialAudio
    )

    fun setVolumeBoost(factor: Float) {
        volumeBoost.setGainFactor(factor)
    }

    fun setSpatialPreset(preset: SpatialAudioProcessor.SpatialPreset) {
        spatialAudio.setPreset(preset)
    }

    fun release() {
        volumeBoost.reset()
        spatialAudio.reset()
    }
}
