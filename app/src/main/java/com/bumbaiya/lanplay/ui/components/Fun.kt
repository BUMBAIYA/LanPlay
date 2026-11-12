package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(Color(0xFFB4F24D), Color(0xFFFF5C7A), Color(0xFF4DA3FF), Color(0xFFFFC83D), Color(0xFF9B6CFF))

private class Bit(val x: Float, val delay: Float, val speed: Float, val sway: Float, val size: Float, val color: Color)

/** A one-off burst of falling confetti for round wins. Redraws only the canvas, never recomposes. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val bits = remember { List(48) { Bit(Random.nextFloat(), Random.nextFloat() * 0.35f, 0.7f + Random.nextFloat() * 0.5f, Random.nextFloat() * 6f, 6f + Random.nextFloat() * 8f, ConfettiColors.random()) } }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2200, easing = LinearEasing)) }
    Canvas(modifier) {
        val p = t.value
        if (p >= 1f) return@Canvas
        bits.forEach { b ->
            val local = ((p - b.delay) / (1f - b.delay)).coerceIn(0f, 1f) * b.speed
            if (local <= 0f) return@forEach
            val y = -20f + local * (size.height + 40f)
            val x = b.x * size.width + sin(local * 6f + b.sway) * 18f
            drawRect(b.color.copy(alpha = (1f - p * 0.6f)), Offset(x, y), Size(b.size, b.size * 1.6f))
        }
    }
}

/** Gently pulses opacity forever, to say "this is happening now". Reads the value only when drawing. */
@Composable
fun Modifier.pulse(): Modifier {
    val a by rememberInfiniteTransition(label = "pulse").animateFloat(0.45f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse-a")
    return graphicsLayer { alpha = a }
}
