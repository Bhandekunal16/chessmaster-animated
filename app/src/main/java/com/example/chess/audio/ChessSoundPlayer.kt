package com.example.chess.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.chess.model.PieceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class ChessSoundPlayer(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    fun playMove(pieceType: PieceType, soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrateShort(25)
        if (!soundEnabled) return

        audioScope.launch {
            when (pieceType) {
                PieceType.KNIGHT -> generateKnightJumpSound()
                PieceType.ROOK -> generateHeavyMoveSound()
                PieceType.QUEEN -> generateGlideSound()
                else -> generateWoodClick(freq = 320f, durationMs = 45)
            }
        }
    }

    fun playCapture(attackingPiece: PieceType, soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrateMedium(55)
        if (!soundEnabled) return

        audioScope.launch {
            when (attackingPiece) {
                PieceType.KNIGHT -> generateKnightStrikeSound()
                PieceType.ROOK -> generateShockwaveSound()
                PieceType.QUEEN -> generateSlashSound()
                else -> generateCaptureImpact()
            }
        }
    }

    fun playCheck(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibratePattern(longArrayOf(0, 40, 60, 40))
        if (!soundEnabled) return

        audioScope.launch {
            generateChimeSound(freq1 = 587.33f, freq2 = 880.0f, durationMs = 260) // D5 to A5
        }
    }

    fun playCheckmate(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibratePattern(longArrayOf(0, 70, 80, 120))
        if (!soundEnabled) return

        audioScope.launch {
            generateVictoryFanfare()
        }
    }

    fun playPromotion(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrateShort(40)
        if (!soundEnabled) return

        audioScope.launch {
            generatePromotionChime()
        }
    }

    // Sound generation via AudioTrack PCM 16-bit
    private fun playPcm(samples: ShortArray) {
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            // Track auto-releases on garbage collection
        } catch (_: Exception) {
            // AudioTrack unavailable or device restricted
        }
    }

    private fun generateWoodClick(freq: Float = 300f, durationMs: Int = 40) {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val decayCurve = decay * decay * decay
            val wave = sin(2f * Math.PI.toFloat() * freq * t) +
                    0.4f * sin(2f * Math.PI.toFloat() * freq * 2.2f * t)
            samples[i] = (wave * decayCurve * 22000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateCaptureImpact() {
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val sub = sin(2f * Math.PI.toFloat() * 140f * t)
            val punch = sin(2f * Math.PI.toFloat() * 380f * t)
            val noise = (Math.random().toFloat() * 2f - 1f) * 0.2f
            val wave = (sub * 0.6f + punch * 0.4f + noise) * decay * decay
            samples[i] = (wave * 28000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateKnightJumpSound() {
        val durationMs = 80
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / numSamples
            // Rising pitch followed by quick tap
            val pitch = 220f + 160f * sin(progress * Math.PI.toFloat())
            val decay = (1f - progress).coerceIn(0f, 1f)
            val wave = sin(2f * Math.PI.toFloat() * pitch * t) * decay
            samples[i] = (wave * 20000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateKnightStrikeSound() {
        val durationMs = 90
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val wave = (sin(2f * Math.PI.toFloat() * 200f * t) + sin(2f * Math.PI.toFloat() * 480f * t)) * decay * decay
            samples[i] = (wave * 26000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateHeavyMoveSound() {
        val durationMs = 60
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val wave = sin(2f * Math.PI.toFloat() * 180f * t) * decay
            samples[i] = (wave * 24000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateShockwaveSound() {
        val durationMs = 110
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val sub = sin(2f * Math.PI.toFloat() * 90f * t)
            val knock = sin(2f * Math.PI.toFloat() * 260f * t)
            val wave = (sub * 0.7f + knock * 0.3f) * decay
            samples[i] = (wave * 30000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateGlideSound() {
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val wave = sin(2f * Math.PI.toFloat() * 440f * t) * decay
            samples[i] = (wave * 18000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateSlashSound() {
        val durationMs = 85
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val wave = (sin(2f * Math.PI.toFloat() * 520f * t) + sin(2f * Math.PI.toFloat() * 780f * t)) * decay * decay
            samples[i] = (wave * 22000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateChimeSound(freq1: Float, freq2: Float, durationMs: Int) {
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val wave = (0.6f * sin(2f * Math.PI.toFloat() * freq1 * t) + 0.4f * sin(2f * Math.PI.toFloat() * freq2 * t)) * decay
            samples[i] = (wave * 24000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generateVictoryFanfare() {
        val durationMs = 380
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        val freqs = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
        for (i in 0 until numSamples) {
            val noteIdx = (i.toFloat() / numSamples * 4).toInt().coerceIn(0, 3)
            val t = i.toFloat() / sampleRate
            val decay = (1f - (i % (numSamples / 4)).toFloat() / (numSamples / 4)).coerceIn(0f, 1f)
            val wave = sin(2f * Math.PI.toFloat() * freqs[noteIdx] * t) * decay
            samples[i] = (wave * 22000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun generatePromotionChime() {
        val durationMs = 260
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        val freqs = floatArrayOf(440f, 554.37f, 659.25f, 880f)
        for (i in 0 until numSamples) {
            val noteIdx = (i.toFloat() / numSamples * 4).toInt().coerceIn(0, 3)
            val t = i.toFloat() / sampleRate
            val decay = (1f - i.toFloat() / numSamples).coerceIn(0f, 1f)
            val wave = sin(2f * Math.PI.toFloat() * freqs[noteIdx] * t) * decay
            samples[i] = (wave * 20000).toInt().toShort()
        }
        playPcm(samples)
    }

    private fun vibrateShort(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateMedium(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }
}
