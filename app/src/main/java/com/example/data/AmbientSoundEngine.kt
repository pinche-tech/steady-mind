package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-time procedural acoustic synthesizer for calming focus soundscapes
 * (Warm Brown Noise, Soft Mist & Rain, 432Hz Zen Drone, and Tibetan Singing Bowl Chime).
 */
object AmbientSoundEngine {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var playbackJob: Job? = null
  private var currentTrack: AudioTrack? = null

  fun startMode(mode: AmbientSoundMode) {
    stop()
    if (mode == AmbientSoundMode.OFF) return

    playbackJob = scope.launch {
      try {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val audioTrack = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        currentTrack = audioTrack
        audioTrack.play()

        val buffer = ShortArray(2048)
        var lastBrown = 0.0
        var phase1 = 0.0
        var phase2 = 0.0
        var lfoPhase = 0.0

        while (isActive) {
          for (i in buffer.indices) {
            val sample: Double = when (mode) {
              AmbientSoundMode.BROWN_NOISE -> {
                val white = (Random.nextDouble() * 2.0 - 1.0)
                lastBrown = (lastBrown + (0.025 * white)) / 1.025
                (lastBrown * 3.2).coerceIn(-0.9, 0.9) * 0.25
              }
              AmbientSoundMode.FOREST_RAIN -> {
                val white = (Random.nextDouble() * 2.0 - 1.0)
                lastBrown = 0.88 * lastBrown + 0.12 * white
                val gentleSwell = 0.8 + 0.2 * sin(lfoPhase)
                lfoPhase += (2.0 * PI * 0.15) / sampleRate
                (lastBrown * gentleSwell * 0.18).coerceIn(-0.8, 0.8)
              }
              AmbientSoundMode.ZEN_HUM -> {
                val f1 = 216.0 // Warm 432Hz sub-octave
                val f2 = 324.0 // Perfect fifth
                phase1 += (2.0 * PI * f1) / sampleRate
                phase2 += (2.0 * PI * f2) / sampleRate
                lfoPhase += (2.0 * PI * 0.18) / sampleRate
                val breathMod = 0.75 + 0.25 * sin(lfoPhase)
                ((sin(phase1) * 0.65 + sin(phase2) * 0.35) * breathMod * 0.16)
              }
              AmbientSoundMode.OFF -> 0.0
            }
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
          }
          audioTrack.write(buffer, 0, buffer.size)
        }
      } catch (_: Throwable) {
        // Safely ignore on unit-test JVM or devices without audio output
      }
    }
  }

  fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    try {
      currentTrack?.stop()
      currentTrack?.release()
    } catch (_: Throwable) {
    }
    currentTrack = null
  }

  fun playSingingBowlChime() {
    scope.launch {
      try {
        val sampleRate = 22050
        val durationSeconds = 2.4
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
          val t = i.toDouble() / sampleRate
          val envelope = exp(-2.2 * t)
          val fundamental = sin(2.0 * PI * 432.0 * t)
          val overtone = 0.4 * sin(2.0 * PI * 864.0 * t) * exp(-3.5 * t)
          val value = (fundamental + overtone) * envelope * 0.22
          samples[i] = (value.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
        }

        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
          .setBufferSizeInBytes(samples.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(samples, 0, samples.size)
        track.play()
      } catch (_: Throwable) {
      }
    }
  }
}
