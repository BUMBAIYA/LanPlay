package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val glyphs = listOf("+", "−", "×", "÷", "7", "3", "9", "5", "2", "8", "4", "6", "1", "=")

private class Ghost(val x: Float, val phase: Float, val laps: Int, val size: Float, val tilt: Float, val sway: Float, val glyph: String)

/**
 * The animated table cloth behind every game screen: soft colour glows drifting about and faint
 * number and operator cards floating slowly upwards. Pure canvas drawing driven by one clock, so it
 * costs almost nothing. Every motion loops exactly, so there is never a visible jump.
 */
@Composable
fun GameBackdrop(modifier: Modifier = Modifier) {
    val clock by rememberInfiniteTransition(label = "backdrop").animateFloat(
        0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val measurer = rememberTextMeasurer()
    val ghosts = remember {
        val r = Random(7)
        List(16) { i -> Ghost(r.nextFloat(), r.nextFloat(), 1 + i % 3, 0.7f + r.nextFloat() * 0.8f, (r.nextFloat() - 0.5f) * 50f, 4f + r.nextFloat() * 10f, glyphs[i % glyphs.size]) }
    }
    val glows = listOf(Color(0xFFB6F24A), Color(0xFF4A90E2), Color(0xFFE2603C))
    Canvas(modifier.fillMaxSize()) {
        val t = clock
        // Glows: wide, very soft, slowly circling.
        glows.forEachIndexed { i, c ->
            val a = 2 * PI.toFloat() * (t * (i + 1) + i * 0.33f)
            val cx = size.width * (0.5f + 0.38f * sin(a))
            val cy = size.height * (0.5f + 0.34f * sin(a * 0.5f + i))
            val r = size.minDimension * (0.65f + 0.1f * i)
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.13f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
        }
        // Ghost cards drift from the bottom to the top and sway a little.
        ghosts.forEach { g ->
            val w = 46.dp.toPx() * g.size
            val h = 64.dp.toPx() * g.size
            val progress = (g.phase + t * g.laps) % 1f
            val y = size.height + h - progress * (size.height + 2 * h)
            val x = g.x * (size.width - w) + sin(2 * PI.toFloat() * (t * g.laps + g.phase)) * g.sway.dp.toPx()
            val fade = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f).let { (it * 3f).coerceAtMost(1f) }
            rotate(g.tilt + sin(2 * PI.toFloat() * (t * g.laps + g.phase)) * 8f, Offset(x + w / 2, y + h / 2)) {
                drawRoundRect(Color.White.copy(alpha = 0.025f * fade), Offset(x, y), Size(w, h), CornerRadius(8.dp.toPx()))
                drawRoundRect(Color.White.copy(alpha = 0.07f * fade), Offset(x, y), Size(w, h), CornerRadius(8.dp.toPx()), Stroke(1.2.dp.toPx()))
                val layout = measurer.measure(g.glyph, TextStyle(fontSize = (26 * g.size).sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.11f * fade)))
                drawText(layout, topLeft = Offset(x + (w - layout.size.width) / 2, y + (h - layout.size.height) / 2))
            }
        }
    }
}
