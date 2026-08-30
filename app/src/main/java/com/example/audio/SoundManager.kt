package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled: Boolean = true

    fun playClick() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(frequency = 880f, durationMs = 30, decay = true)
        }
    }

    fun playUnscrew() {
        if (!isSoundEnabled) return
        scope.launch {
            // Rapid pitch slide down to simulate metal screw threads spinning
            val sampleRate = 44100
            val durationMs = 120
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val freq = 1200f - (i.toFloat() / numSamples) * 700f
                val env = 1f - (i.toFloat() / numSamples)
                val sample = (sin(2.0 * PI * freq * t) * 32767 * env * 0.5f).toInt().toShort()
                buffer[i] = sample
            }
            writeAndPlay(buffer, sampleRate)
        }
    }

    fun playPlankDrop() {
        if (!isSoundEnabled) return
        scope.launch {
            // Low deep resonant wooden 'thump'
            val sampleRate = 44100
            val durationMs = 180
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val freq = 160f * (1f - (i.toFloat() / numSamples) * 0.4f)
                val env = kotlin.math.exp(-12.0 * i / numSamples)
                val sample = (sin(2.0 * PI * freq * t) * 32767 * env * 0.7f).toInt().toShort()
                buffer[i] = sample
            }
            writeAndPlay(buffer, sampleRate)
        }
    }

    fun playBoxMatch() {
        if (!isSoundEnabled) return
        scope.launch {
            // Bright cheerful chime chord
            playTone(frequency = 587.33f, durationMs = 80, decay = true) // D5
            kotlinx.coroutines.delay(60)
            playTone(frequency = 880.00f, durationMs = 100, decay = true) // A5
            kotlinx.coroutines.delay(70)
            playTone(frequency = 1174.66f, durationMs = 180, decay = true) // D6
        }
    }

    fun playVictory() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f, 1318.51f)
            for (note in notes) {
                playTone(frequency = note, durationMs = 140, decay = true)
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(440f, 415.3f, 392f, 349.2f)
            for (note in notes) {
                playTone(frequency = note, durationMs = 180, decay = true)
                kotlinx.coroutines.delay(150)
            }
        }
    }

    fun playToolUse() {
        if (!isSoundEnabled) return
        scope.launch {
            // Power up buzz
            val sampleRate = 44100
            val durationMs = 220
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val freq = 400f + (i.toFloat() / numSamples) * 800f
                val env = 0.6f
                val sample = (sin(2.0 * PI * freq * t) * 32767 * env).toInt().toShort()
                buffer[i] = sample
            }
            writeAndPlay(buffer, sampleRate)
        }
    }

    private fun playTone(frequency: Float, durationMs: Int, decay: Boolean = false) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = if (decay) 1f - (i.toFloat() / numSamples) else 1f
            val sample = (sin(2.0 * PI * frequency * t) * 32767 * env * 0.4f).toInt().toShort()
            buffer[i] = sample
        }
        writeAndPlay(buffer, sampleRate)
    }

    private fun writeAndPlay(buffer: ShortArray, sampleRate: Int) {
        try {
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                kotlinx.coroutines.delay(buffer.size * 1000L / sampleRate + 50)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            // AudioTrack fallback
        }
    }
}
