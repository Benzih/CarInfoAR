package com.carinfo.ar.util

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
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
            mp?.setOnCompletionListener { it.release() }
            mp?.start()
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
