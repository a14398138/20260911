package com.example.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural low-latency arcade sound synthesizer.
 * Generates dynamic waveforms using Android AudioTrack without requiring any external audio assets.
 */
class SoundManager(context: Context) {

    var isMuted: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    private fun playPcm(samples: ShortArray) {
        if (isMuted) return
        scope.launch {
            try {
                val bufferSize = samples.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                // Wait for playback to finish before release
                val durationMs = (samples.size * 1000L) / sampleRate + 50
                kotlinx.coroutines.delay(durationMs)
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }

    /** Laser fire: frequency sweep from 900Hz to 250Hz */
    fun playLaser() {
        if (isMuted) return
        val duration = 0.08f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 900f * (1f - t * 0.75f)
            val envelope = 1f - t
            val angle = 2.0 * PI * freq * (i.toDouble() / sampleRate)
            samples[i] = (sin(angle) * envelope * Short.MAX_VALUE * 0.4f).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Enemy destruction / explosion: noise burst with exponential decay */
    fun playExplosion(large: Boolean = false) {
        if (isMuted) return
        val duration = if (large) 0.35f else 0.16f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        var lastVal = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val envelope = (1f - t) * (1f - t)
            val whiteNoise = (Math.random() * 2.0 - 1.0)
            // Low-pass filter for deeper explosion rumble
            val rumble = if (large) {
                lastVal = (lastVal * 0.85 + whiteNoise * 0.15)
                lastVal
            } else {
                whiteNoise
            }
            samples[i] = (rumble * envelope * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Power-up collect: bright 3-note ascending arpeggio */
    fun playPowerUp() {
        if (isMuted) return
        val duration = 0.18f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        val freqs = floatArrayOf(587f, 740f, 880f) // D5, F#5, A5
        val segLen = numSamples / 3
        for (i in 0 until numSamples) {
            val seg = (i / segLen).coerceIn(0, 2)
            val freq = freqs[seg]
            val localT = (i % segLen).toFloat() / segLen
            val env = 1f - localT * 0.4f
            val angle = 2.0 * PI * freq * (i.toDouble() / sampleRate)
            samples[i] = (sin(angle) * env * Short.MAX_VALUE * 0.45f).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Shield hit: metallic deflection pulse */
    fun playShieldHit() {
        if (isMuted) return
        val duration = 0.10f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 440f + sin(t * 20) * 150f
            val envelope = (1f - t) * (1f - t)
            val angle = 2.0 * PI * freq * (i.toDouble() / sampleRate)
            samples[i] = (sin(angle) * envelope * Short.MAX_VALUE * 0.4f).toInt().toShort()
        }
        playPcm(samples)
    }

    /** Quantum Bomb detonation: deep subsonic swell & massive shockwave */
    fun playBomb() {
        if (isMuted) return
        val duration = 0.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 120f * (1f - t * 0.6f)
            val env = (1f - t)
            val wave = sin(2.0 * PI * freq * (i.toDouble() / sampleRate))
            val noise = (Math.random() * 2.0 - 1.0) * 0.4
            samples[i] = ((wave * 0.6 + noise * 0.4) * env * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        playPcm(samples)
    }
}
