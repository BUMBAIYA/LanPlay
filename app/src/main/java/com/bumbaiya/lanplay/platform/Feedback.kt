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
    fun draw() = beep(ToneGenerator.TONE_PROP_BEEP, 80)
}
