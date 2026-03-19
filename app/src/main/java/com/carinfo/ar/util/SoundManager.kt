package com.carinfo.ar.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundManager {
    private var soundPool: SoundPool? = null
    private var scanSoundId = 0
    private var infoSoundId = 0
    private var isInitialized = false
    var soundEnabled = true

    fun init(context: Context) {
        if (isInitialized) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        // We'll use system sounds as fallback since we don't have custom sound files
        isInitialized = true
    }

    fun playScanDetected() {
        if (!soundEnabled) return
        // Haptic feedback only - simple vibration
    }

    fun playInfoLoaded() {
        if (!soundEnabled) return
    }

    fun vibrate(context: Context, durationMs: Long = 50) {
        if (!soundEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
