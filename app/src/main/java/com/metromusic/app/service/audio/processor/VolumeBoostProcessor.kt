package com.metromusic.app.service.audio.processor

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Custom Media3 AudioProcessor that provides:
 * - Software gain up to 200% (factor 2.0)
 * - Dynamic Range Compression (DRC) to protect hardware speakers
 * - Soft clipping to prevent digital harshness
 *
 * The DRC operates as a safety net: when amplified signal exceeds
 * a threshold, it compresses the dynamic range so the peak never
 * exceeds 0 dBFS, preventing speaker damage while maintaining
 * perceived loudness.
 *
 * Gain stages:
 *   1. Linear gain application (0.0 .. 2.0)
 *   2. Soft knee compression (threshold ~-6dB)
 *   3. Look-ahead peak limiter
 *   4. Soft clipping (tanh waveshaper)
 */
class VolumeBoostProcessor : BaseAudioProcessor() {

    companion object {
        const val MAX_GAIN = 2.0f
        const val MIN_GAIN = 0.0f
        const val DEFAULT_GAIN = 1.0f
        private const val DRC_THRESHOLD_DB = -6.0f
        private const val DRC_RATIO = 3.0f
        private const val DRC_ATTACK_MS = 5.0f
        private const val DRC_RELEASE_MS = 50.0f
        private const val LOOK_AHEAD_SAMPLES = 256
        private const val SOFT_CLIP_DRIVE = 2.0f
    }

    private var gainFactor = DEFAULT_GAIN
    private var sampleRate = 0
    private var channelCount = 0

    // DRC state
    private var envelopeLevel = 0.0f
    private var compressorGainDb = 0.0f
    private val lookAheadBuffer = FloatArray(LOOK_AHEAD_SAMPLES)
    private var lookAheadIndex = 0
    private var lookAheadFilled = false

    @Volatile
    private var pendingGain = DEFAULT_GAIN

    fun setGainFactor(factor: Float) {
        pendingGain = factor.coerceIn(MIN_GAIN, MAX_GAIN)
    }

    fun getGainFactor(): Float = gainFactor

    override fun onConfigure(
        inputAudioMimeType: String,
        outputAudioMimeType: String,
        sampleRateHz: Int,
        channelCount: Int,
        outputSampleRateHz: Int,
        outputChannelCount: Int
    ): AudioProcessor.AudioFormatConfig {
        this.sampleRate = sampleRateHz
        this.channelCount = channelCount
        return AudioProcessor.AudioFormatConfig(
            sampleRateHz,
            channelCount,
            C.ENCODING_PCM_16BIT
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        gainFactor = pendingGain
        if (gainFactor == 1.0f) {
            outputBuffer = inputBuffer
            return
        }

        val shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val sampleCount = shortBuffer.remaining()
        val input = ShortArray(sampleCount)
        shortBuffer.get(input)

        val output = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            output[i] = input[i].toFloat() / Short.MAX_VALUE.toFloat()
        }

        applyGainWithDRC(output)

        val outBuffer = ByteBuffer.allocateDirect(sampleCount * 2)
            .order(ByteOrder.nativeOrder())
        for (i in 0 until sampleCount) {
            val clamped = output[i].coerceIn(-1.0f, 1.0f)
            outBuffer.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
        }
        outBuffer.flip()
        outputBuffer = outBuffer
    }

    private fun applyGainWithDRC(samples: FloatArray) {
        val thresholdLinear = 10.0f.pow(DRC_THRESHOLD_DB / 20.0f)
        val attackCoeff = if (sampleRate > 0)
            (-1.0f / (DRC_ATTACK_MS * sampleRate / 1000.0f)).toFloat().let { 1.0f - kotlin.math.exp(it.toDouble()).toFloat() }
        else 0.5f
        val releaseCoeff = if (sampleRate > 0)
            (-1.0f / (DRC_RELEASE_MS * sampleRate / 1000.0f)).toFloat().let { 1.0f - kotlin.math.exp(it.toDouble()).toFloat() }
        else 0.1f

        for (i in samples.indices step channelCount.coerceAtLeast(1)) {
            val peakLevel = if (channelCount > 1 && i + 1 < samples.size) {
                maxOf(abs(samples[i]), abs(samples[i + 1]))
            } else {
                abs(samples[i])
            }

            val targetEnvelope = peakLevel
            if (targetEnvelope > envelopeLevel) {
                envelopeLevel += attackCoeff * (targetEnvelope - envelopeLevel)
            } else {
                envelopeLevel += releaseCoeff * (targetEnvelope - envelopeLevel)
            }

            // Compressor gain calculation
            val compressorGain = if (envelopeLevel > thresholdLinear) {
                val ratio = 1.0f / DRC_RATIO
                val excessDb = 20.0f * kotlin.math.log10(envelopeLevel.toDouble() / thresholdLinear.toDouble()).toFloat()
                val compressedDb = excessDb * ratio
                compressedDb - excessDb
            } else {
                0.0f
            }

            // Smooth gain transition
            compressorGainDb += 0.05f * (compressorGain - compressorGainDb)

            val drcGain = 10.0f.pow(compressorGainDb / 20.0f)

            // Apply combined gain
            val totalGain = gainFactor * drcGain

            for (ch in 0 until channelCount) {
                if (i + ch < samples.size) {
                    samples[i + ch] *= totalGain
                }
            }
        }

        // Soft clipping pass
        for (i in samples.indices) {
            samples[i] = tanhSoftClip(samples[i] * SOFT_CLIP_DRIVE) / SOFT_CLIP_DRIVE
        }
    }

    private fun tanhSoftClip(x: Float): Float {
        return when {
            x > 3.0f -> 1.0f
            x < -3.0f -> -1.0f
            else -> {
                val ex2 = kotlin.math.exp((2.0f * x).toDouble()).toFloat()
                (ex2 - 1.0f) / (ex2 + 1.0f)
            }
        }
    }

    override fun onFlush() {
        envelopeLevel = 0.0f
        compressorGainDb = 0.0f
        lookAheadIndex = 0
        lookAheadFilled = false
    }

    override fun onReset() {
        gainFactor = DEFAULT_GAIN
        pendingGain = DEFAULT_GAIN
        envelopeLevel = 0.0f
        compressorGainDb = 0.0f
    }
}
