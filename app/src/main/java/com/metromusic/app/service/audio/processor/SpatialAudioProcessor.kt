package com.metromusic.app.service.audio.processor

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Advanced spatial audio processor that simulates:
 *
 * 1. JBL Sound Stage: Punchy, bass-forward signature with enhanced
 *    low-mid warmth (200-400Hz) and controlled high-frequency sparkle.
 *    Uses a psychoacoustic bass enhancement algorithm and stereo
 *    width expansion.
 *
 * 2. Dolby Atmos Simulation: Creates virtualized 3D surround from
 *    stereo using:
 *    - Head-Related Transfer Function (HRTF) approximation via
 *      all-pass filtering and comb filtering
 *    - Phase-based spatial expansion (interchannel coherence)
 *    - Frequency-dependent panning (HF sources perceived as elevated)
 *    - Crosstalk cancellation hints for headphone playback
 *
 * The processor operates on stereo (2ch) PCM 16-bit audio.
 * For mono input, it creates artificial stereo width first.
 */
class SpatialAudioProcessor : BaseAudioProcessor() {

    enum class SpatialPreset {
        JBL_STAGE,
        DOLBY_ATMOS,
        OFF
    }

    private var preset = SpatialPreset.OFF
    private var sampleRate = 0
    private var channelCount = 0

    // JBL Stage filter state
    private var jblBassCoeffs = FloatArray(4) { 0.0f }
    private var jblBassState = FloatArray(4) { 0.0f }
    private var jblMidCoeffs = FloatArray(4) { 0.0f }
    private var jblMidState = FloatArray(4) { 0.0f }
    private var jblHighCoeffs = FloatArray(4) { 0.0f }
    private var jblHighState = FloatArray(4) { 0.0f }
    private var stereoWidthFactor = 1.0f

    // Atmos spatial state
    private var atmosAllPass = FloatArray(12) { 0.0f }
    private var atmosCombFilters = Array(6) { FloatArray(256) { 0.0f } }
    private var atmosCombIndices = IntArray(6) { 0 }
    private var atmosPhaseRotators = FloatArray(6) { 0.0f }
    private var atmosDelayLines = Array(4) { FloatArray(512) { 0.0f } }
    private var atmosDelayIndices = IntArray(4) { 0 }

    // Shared
    private var processingEnabled = false
    private var wetDryMix = 0.7f

    fun setPreset(newPreset: SpatialPreset) {
        preset = newPreset
        processingEnabled = preset != SpatialPreset.OFF
    }

    fun getPreset(): SpatialPreset = preset

    override fun onConfigure(
        inputAudioMimeType: String,
        outputAudioMimeType: String,
        sampleRateHz: Int,
        channelCount: Int,
        outputSampleRateHz: Int,
        outputChannelCount: Int
    ): AudioProcessor.AudioFormatConfig {
        this.sampleRate = sampleRateHz
        this.channelCount = channelCount.coerceAtLeast(2)

        initJBLCoefficients()
        initAtmosDelays()

        return AudioProcessor.AudioFormatConfig(
            sampleRateHz,
            this.channelCount,
            C.ENCODING_PCM_16BIT
        )
    }

    private fun initJBLCoefficients() {
        if (sampleRate <= 0) return
        val bassFreq = 150.0f
        val midFreq = 1200.0f
        val highFreq = 6000.0f

        jblBassCoeffs = designPeakingFilter(bassFreq, 2.5f, 6.0f, sampleRate)
        jblMidCoeffs = designPeakingFilter(midFreq, 1.5f, 2.0f, sampleRate)
        jblHighCoeffs = designShelfFilter(highFreq, 0.8f, 1.5f, sampleRate, isHighShelf = true)
        stereoWidthFactor = 1.4f
    }

    private fun initAtmosDelays() {
        if (sampleRate <= 0) return
        // All-pass filter delay times (prime number samples for diffuse decay)
        val allPassDelays = intArrayOf(53, 97, 163, 211, 281, 349, 419, 491, 557, 631, 691, 751)
        allPassDelays.forEachIndexed { i, d ->
            if (i < atmosAllPass.size) atmosAllPass[i] = d.toFloat()
        }

        // Comb filter delays for early reflections (in ms -> samples)
        val combDelaysMs = floatArrayOf(1.2f, 2.8f, 4.5f, 7.1f, 11.3f, 15.7f)
        combDelaysMs.forEachIndexed { i, ms ->
            val delaySamples = (ms * sampleRate / 1000.0f).toInt().coerceIn(1, 255)
            atmosCombFilters[i] = FloatArray(256) { 0.0f }
            atmosCombIndices[i] = 0
        }

        // Phase rotator coefficients (frequency-dependent)
        for (i in 0 until 6) {
            atmosPhaseRotators[i] = (2.0f * PI * (i + 1) * 137.0 / sampleRate).toFloat()
        }

        // Delay lines for elevation simulation
        val elevDelaysMs = floatArrayOf(0.8f, 2.1f, 3.7f, 5.9f)
        elevDelaysMs.forEachIndexed { i, ms ->
            val d = (ms * sampleRate / 1000.0f).toInt().coerceIn(1, 511)
            atmosDelayLines[i] = FloatArray(512) { 0.0f }
            atmosDelayIndices[i] = 0
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!processingEnabled) {
            outputBuffer = inputBuffer
            return
        }

        val shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val sampleCount = shortBuffer.remaining()
        val input = ShortArray(sampleCount)
        shortBuffer.get(input)

        val floatSamples = FloatArray(sampleCount) { input[it].toFloat() / Short.MAX_VALUE.toFloat() }

        when (preset) {
            SpatialPreset.JBL_STAGE -> processJBLStage(floatSamples)
            SpatialPreset.DOLBY_ATMOS -> processDolbyAtmos(floatSamples)
            SpatialPreset.OFF -> {}
        }

        val outBuffer = ByteBuffer.allocateDirect(sampleCount * 2)
            .order(ByteOrder.nativeOrder())
        for (i in 0 until sampleCount) {
            val clamped = floatSamples[i].coerceIn(-1.0f, 1.0f)
            outBuffer.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
        }
        outBuffer.flip()
        outputBuffer = outBuffer
    }

    private fun processJBLStage(samples: FloatArray) {
        val step = channelCount.coerceAtLeast(2)
        for (i in 0 until samples.size - 1 step step) {
            var left = samples[i]
            var right = samples[i + 1]

            // 1. Bass enhancement - punchy low-end
            left = applyBiquad(left, jblBassCoeffs, jblBassState, 0)
            right = applyBiquad(right, jblBassCoeffs, jblBassState, 1)

            // 2. Mid warmth
            left = applyBiquad(left, jblMidCoeffs, jblMidState, 0)
            right = applyBiquad(right, jblMidCoeffs, jblMidState, 1)

            // 3. High sparkle
            left = applyBiquad(left, jblHighCoeffs, jblHighState, 0)
            right = applyBiquad(right, jblHighCoeffs, jblHighState, 1)

            // 4. Stereo width expansion (M/S processing)
            val mid = (left + right) * 0.5f
            val side = (left - right) * 0.5f * stereoWidthFactor
            left = mid + side
            right = mid - side

            // 5. Transient enhancement for punch
            left = transientEnhance(left)
            right = transientEnhance(right)

            samples[i] = left * 0.9f
            samples[i + 1] = right * 0.9f
        }
    }

    private fun processDolbyAtmos(samples: FloatArray) {
        val step = channelCount.coerceAtLeast(2)
        for (i in 0 until samples.size - 1 step step) {
            var left = samples[i]
            var right = samples[i + 1]

            // 1. All-pass filters for diffuse field
            for (ap in 0 until min(6, atmosAllPass.size)) {
                val delayLen = atmosAllPass[ap].toInt().coerceIn(1, 127)
                val idx = (ap * 2) % 12
                val bufIdx = idx % atmosAllPass.size
                val readIdx = ((atmosAllPass[bufIdx].toInt() - delayLen + 128) % 128).toInt()
                val fb = 0.5f

                val tmpL = left - fb * atmosAllPass[bufIdx]
                atmosAllPass[bufIdx] = left + tmpL * 0.3f
                left = tmpL * 0.7f + atmosAllPass[bufIdx] * 0.3f

                val tmpR = right - fb * atmosAllPass[(bufIdx + 1) % atmosAllPass.size]
                atmosAllPass[(bufIdx + 1) % atmosAllPass.size] = right + tmpR * 0.3f
                right = tmpR * 0.7f + atmosAllPass[(bufIdx + 1) % atmosAllPass.size] * 0.3f
            }

            // 2. Comb filters for early reflections
            for (cf in 0 until 6) {
                val combDelay = when (cf) {
                    0 -> 11; 1 -> 23; 2 -> 37; 3 -> 53; 4 -> 71; 5 -> 89
                }
                val readPos = (atmosCombIndices[cf] - combDelay + 256) % 256
                val combGain = 0.35f

                left += atmosCombFilters[cf][readPos] * combGain * 0.3f
                right += atmosCombFilters[cf][readPos] * combGain * 0.3f
                atmosCombFilters[cf][atmosCombIndices[cf]] = left * 0.5f
                atmosCombIndices[cf] = (atmosCombIndices[cf] + 1) % 256
            }

            // 3. Phase rotation for elevation
            val phase1 = atmosPhaseRotators[0]
            val phase2 = atmosPhaseRotators[2]
            val elevLeft = left * cos(phase1.toDouble()).toFloat() - right * sin(phase2.toDouble()).toFloat()
            val elevRight = right * cos(phase2.toDouble()).toFloat() + left * sin(phase1.toDouble()).toFloat()
            left = elevLeft
            right = elevRight

            // 4. Delay-based decorrelation for width
            for (dl in 0 until 4) {
                val delayLen = 30 + dl * 23
                val readPos = (atmosDelayIndices[dl] - delayLen + 512) % 512
                val decorrGain = 0.15f
                if (dl % 2 == 0) left += atmosDelayLines[dl][readPos] * decorrGain
                else right += atmosDelayLines[dl][readPos] * decorrGain

                atmosDelayLines[dl][atmosDelayIndices[dl]] = (left + right) * 0.5f
                atmosDelayIndices[dl] = (atmosDelayIndices[dl] + 1) % 512
            }

            // 5. Crosstalk cancellation hint (for headphone optimization)
            val xtalkL = left - right * 0.15f
            val xtalkR = right - left * 0.15f

            // Wet/dry mix
            samples[i] = left * (1.0f - wetDryMix) + xtalkL * wetDryMix
            samples[i + 1] = right * (1.0f - wetDryMix) + xtalkR * wetDryMix
        }
    }

    private fun transientEnhance(sample: Float): Float {
        val absSample = kotlin.math.abs(sample)
        val envelope = absSample * 0.3f
        return if (absSample > envelope * 1.5f) {
            sample * 1.2f
        } else {
            sample
        }
    }

    private fun applyBiquad(
        input: Float,
        coefficients: FloatArray,
        state: FloatArray,
        channelOffset: Int
    ): Float {
        if (coefficients.size < 5) return input
        val idx = channelOffset * 2
        if (idx + 1 >= state.size) return input

        val output = coefficients[0] * input + coefficients[1] * state[idx] +
                coefficients[2] * state[idx + 1] - coefficients[3] * state[idx] -
                coefficients[4] * state[idx + 1]

        state[idx + 1] = state[idx]
        state[idx] = output

        return output
    }

    private fun designPeakingFilter(
        freq: Float,
        q: Float,
        gainDb: Float,
        sampleRate: Int
    ): FloatArray {
        if (sampleRate <= 0) return FloatArray(5) { 0.0f }
        val w0 = 2.0 * PI * freq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val a = 10.0.pow(gainDb / 40.0)

        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cos(w0)
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cos(w0)
        val a2 = 1.0 - alpha / a

        return floatArrayOf(
            (b0 / a0).toFloat(),
            (b1 / a0).toFloat(),
            (b2 / a0).toFloat(),
            (a1 / a0).toFloat(),
            (a2 / a0).toFloat()
        )
    }

    private fun designShelfFilter(
        freq: Float,
        q: Float,
        gainDb: Float,
        sampleRate: Int,
        isHighShelf: Boolean
    ): FloatArray {
        if (sampleRate <= 0) return FloatArray(5) { 0.0f }
        val w0 = 2.0 * PI * freq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val a = 10.0.pow(gainDb / 40.0)
        val sqAlpha = 2.0 * sqrt(a) * alpha

        val (b0, b1, b2, a0, a1, a2) = if (isHighShelf) {
            listOf(
                a * ((a + 1.0) + (a - 1.0) * cos(w0) + sqAlpha),
                -2.0 * a * ((a - 1.0) + (a + 1.0) * cos(w0)),
                a * ((a + 1.0) + (a - 1.0) * cos(w0) - sqAlpha),
                (a + 1.0) - (a - 1.0) * cos(w0) + sqAlpha,
                2.0 * ((a - 1.0) - (a + 1.0) * cos(w0)),
                (a + 1.0) - (a - 1.0) * cos(w0) - sqAlpha
            )
        } else {
            listOf(
                a * ((a + 1.0) - (a - 1.0) * cos(w0) + sqAlpha),
                2.0 * a * ((a - 1.0) - (a + 1.0) * cos(w0)),
                a * ((a + 1.0) - (a - 1.0) * cos(w0) - sqAlpha),
                (a + 1.0) + (a - 1.0) * cos(w0) + sqAlpha,
                -2.0 * ((a - 1.0) + (a + 1.0) * cos(w0)),
                (a + 1.0) + (a - 1.0) * cos(w0) - sqAlpha
            )
        }

        return floatArrayOf(
            (b0 / a0).toFloat(),
            (b1 / a0).toFloat(),
            (b2 / a0).toFloat(),
            (a1 / a0).toFloat(),
            (a2 / a0).toFloat()
        )
    }

    override fun onFlush() {
        jblBassState.fill(0.0f)
        jblMidState.fill(0.0f)
        jblHighState.fill(0.0f)
        for (i in atmosCombFilters.indices) atmosCombFilters[i].fill(0.0f)
        for (i in atmosDelayLines.indices) atmosDelayLines[i].fill(0.0f)
        atmosCombIndices.fill(0)
        atmosDelayIndices.fill(0)
    }

    override fun onReset() {
        preset = SpatialPreset.OFF
        processingEnabled = false
        onFlush()
    }
}

private fun <T> List<T>.to5(): FiveTuple<T> {
    require(size == 5) { "List must have exactly 5 elements" }
    return FiveTuple(get(0), get(1), get(2), get(3), get(4))
}

data class FiveTuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

private fun <T> destructuredTo5(list: List<T>): FiveTuple<T, T, T, T, T> =
    FiveTuple(list[0], list[1], list[2], list[3], list[4])
