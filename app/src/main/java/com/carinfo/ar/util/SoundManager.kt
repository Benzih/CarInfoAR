package com.carinfo.ar.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundManager {
    private var appContext: Context? = null
    var soundEnabled = true

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun playSystemSound(type: Int) {
        if (!soundEnabled) return
        val ctx = appContext ?: return
        try {
            val uri = RingtoneManager.getDefaultUri(type)
            val mp = MediaPlayer.create(ctx, uri)
            if (mp != null) {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mp.setOnCompletionListener { it.release() }
                mp.start()
            } else {
                // Fallback: ToneGenerator beep for devices without default notification sound
                playToneBeep()
            }
        } catch (_: Exception) {
            // Last resort fallback
            try { playToneBeep() } catch (_: Exception) {}
        }
    }

    private fun playToneBeep() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            // Release after tone finishes
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { toneGenerator.release() } catch (_: Exception) {}
            }, 200)
        } catch (_: Exception) {}
    }

    fun playScanDetected() {
        playSystemSound(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun playInfoLoaded() {
        playSystemSound(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun playSaved() {
        playSystemSound(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun playDelete() {
        playSystemSound(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun playDeleteAll() {
        playSystemSound(RingtoneManager.TYPE_NOTIFICATION)
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
        appContext = null
    }
}
