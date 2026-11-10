package com.bumbaiya.lanplay.platform

import android.media.AudioManager
import android.media.ToneGenerator

/** Short beeps for turn changes and draws. Respects the sound setting. */
object Feedback {
    private fun beep(tone: Int, ms: Int) {
        if (!Session.settings.sound.value) return
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60).also { it.startTone(tone, ms) } }
    }
    fun turn() = beep(ToneGenerator.TONE_PROP_ACK, 180)
    /** Direct vibration; the Compose haptic call is silenced by the phone's touch-feedback setting. */
    @Suppress("DEPRECATION")
    fun buzz(context: android.content.Context) {
        if (!Session.settings.haptics.value) return
        runCatching {
            val v = if (android.os.Build.VERSION.SDK_INT >= 31)
                (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            else context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            v.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 220, 90, 220), -1))
        }
    }
    fun draw() = beep(ToneGenerator.TONE_PROP_BEEP, 80)
}
