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
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Teal
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val glyphs = listOf("+", "−", "×", "÷", "7", "3", "9", "5", "2", "8", "4", "6", "1", "=")

private class Ghost(val x: Float, val phase: Float, val laps: Int, val size: Float, val tilt: Float, val sway: Float, val glyph: String)

/**
 * The animated cloth behind every screen: soft warm glows drifting about, and, on the Combine game's
 * screens only ([cards]), faint number and operator cards floating slowly upwards. One clock drives it all and
 * every motion loops exactly. Cheap: it only redraws a canvas.
 */
@Composable
fun Backdrop(cards: Boolean, modifier: Modifier = Modifier) {
    val clock by rememberInfiniteTransition(label = "backdrop").animateFloat(
        0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val measurer = rememberTextMeasurer()
    val ghosts = remember {
        val r = Random(7)
        List(16) { i -> Ghost(r.nextFloat(), r.nextFloat(), 1 + i % 3, 0.7f + r.nextFloat() * 0.8f, (r.nextFloat() - 0.5f) * 50f, 4f + r.nextFloat() * 10f, glyphs[i % glyphs.size]) }
    }
    val glows = listOf(Amber to 0.28f, Rose to 0.14f, Teal to 0.16f)
    Canvas(modifier.fillMaxSize()) {
        val t = clock
        glows.forEachIndexed { i, (c, a) ->
            val ang = 2 * PI.toFloat() * (t * (i + 1) + i * 0.33f)
            val cx = size.width * (0.5f + 0.38f * sin(ang))
            val cy = size.height * (0.5f + 0.34f * sin(ang * 0.5f + i))
            val r = size.minDimension * (0.75f + 0.1f * i)
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
        }
        if (!cards) return@Canvas
        ghosts.forEach { g ->
            val w = 46.dp.toPx() * g.size
            val h = 64.dp.toPx() * g.size
            val progress = (g.phase + t * g.laps) % 1f
            val y = size.height + h - progress * (size.height + 2 * h)
            val x = g.x * (size.width - w) + sin(2 * PI.toFloat() * (t * g.laps + g.phase)) * g.sway.dp.toPx()
            val fade = (1f - abs(progress - 0.5f) * 2f).coerceIn(0f, 1f).let { (it * 3f).coerceAtMost(1f) }
            rotate(g.tilt + sin(2 * PI.toFloat() * (t * g.laps + g.phase)) * 8f, Offset(x + w / 2, y + h / 2)) {
                drawRoundRect(Ink.copy(alpha = 0.03f * fade), Offset(x, y), Size(w, h), CornerRadius(8.dp.toPx()))
                drawRoundRect(Ink.copy(alpha = 0.10f * fade), Offset(x, y), Size(w, h), CornerRadius(8.dp.toPx()), Stroke(1.5.dp.toPx()))
                val layout = measurer.measure(g.glyph, TextStyle(fontFamily = Baloo, fontSize = (22 * g.size).sp, fontWeight = FontWeight.Bold, color = Ink.copy(alpha = 0.12f * fade)))
                drawText(layout, topLeft = Offset(x + (w - layout.size.width) / 2, y + (h - layout.size.height) / 2))
            }
        }
    }
}
