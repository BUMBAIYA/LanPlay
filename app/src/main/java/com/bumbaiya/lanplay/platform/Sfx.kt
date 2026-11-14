package com.bumbaiya.lanplay.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Button sounds, synthesised at start-up so the app needs no audio files: a soft "tick" when a button
 * goes down and a bright "pop" when it springs back. Respects the sound setting.
 */
object Sfx {
    private var pool: SoundPool? = null
    private var down = 0
    private var up = 0
    private var ready = 0

    fun init(context: Context) {
        if (pool != null) return
        val sp = SoundPool.Builder().setMaxStreams(4)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        sp.setOnLoadCompleteListener { _, _, status -> if (status == 0) ready++ }
        pool = sp
        runCatching {
            val dir = context.cacheDir
            down = sp.load(wav(File(dir, "sfx_down.wav"), tone(0.045, 560.0, 420.0, 0.55, 38.0)).path, 1)
            up = sp.load(wav(File(dir, "sfx_up.wav"), tone(0.09, 980.0, 560.0, 0.6, 24.0)).path, 1)
        }
    }

    private fun enabled() = runCatching { Session.settings.sound.value }.getOrDefault(true)

    fun down() { if (enabled() && ready >= 2) pool?.play(down, 0.7f, 0.7f, 1, 0, 1f) }
    fun up() { if (enabled() && ready >= 2) pool?.play(up, 0.8f, 0.8f, 1, 0, 1f) }

    private const val RATE = 22050

    /** A sine that slides from [f0] to [f1] while it decays. */
    private fun tone(seconds: Double, f0: Double, f1: Double, gain: Double, decay: Double): ShortArray {
        val n = (seconds * RATE).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i / RATE.toDouble()
            val f = f0 + (f1 - f0) * (t / seconds)
            phase += 2 * PI * f / RATE
            val env = exp(-decay * t) * (if (i < 80) i / 80.0 else 1.0)
            (sin(phase) * env * gain * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun wav(file: File, pcm: ShortArray): File {
        RandomAccessFile(file, "rw").use { f ->
            f.setLength(0)
            val bytes = pcm.size * 2
            fun i32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
            fun i16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
            f.writeBytes("RIFF"); f.write(i32(36 + bytes)); f.writeBytes("WAVEfmt ")
            f.write(i32(16)); f.write(i16(1)); f.write(i16(1)); f.write(i32(RATE)); f.write(i32(RATE * 2)); f.write(i16(2)); f.write(i16(16))
            f.writeBytes("data"); f.write(i32(bytes))
            val buf = ByteArray(bytes)
            pcm.forEachIndexed { i, s -> buf[2 * i] = s.toByte(); buf[2 * i + 1] = (s.toInt() shr 8).toByte() }
            f.write(buf)
        }
        return file
    }
}
