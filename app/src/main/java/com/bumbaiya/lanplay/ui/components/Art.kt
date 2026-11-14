package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Rose
import com.bumbaiya.lanplay.ui.theme.Teal
import com.bumbaiya.lanplay.ui.theme.TealDark
import com.bumbaiya.lanplay.ui.theme.Terra
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The illustrations: flat, drawn on a 200 x 150 board and scaled to fit, each with its own small loop
 * (Wi-Fi arcs pinging, portal rings pulsing, the clock filling, cards swaying). One clock in seconds drives them all.
 */

private val Pale1 = Color(0xFFFBE3B0)
private val Pale2 = Color(0xFFF6D79A)

private class Scene(val d: DrawScope, val m: TextMeasurer, val t: Float)

@Composable
private fun Art(modifier: Modifier, draw: Scene.() -> Unit) {
    val clock by rememberInfiniteTransition(label = "art").animateFloat(0f, 360f, infiniteRepeatable(tween(360_000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val m = rememberTextMeasurer()
    Canvas(modifier) {
        val s = min(size.width / 200f, size.height / 150f)
        withTransform({ translate((size.width - 200f * s) / 2f, (size.height - 150f * s) / 2f); scale(s, s, Offset.Zero) }) { Scene(this, m, clock).draw() }
    }
}

private fun Scene.disc(cx: Float, cy: Float, r: Float, c: Color) = d.drawCircle(c, r, Offset(cx, cy))
private fun Scene.circleLine(cx: Float, cy: Float, r: Float, c: Color, w: Float, a: Float = 1f) = d.drawCircle(c.copy(alpha = a), r, Offset(cx, cy), style = Stroke(w))
private fun Scene.shadowDisc(cx: Float, cy: Float, r: Float) = d.drawCircle(Ink.copy(alpha = 0.35f), r, Offset(cx + 3f, cy + 4f))

private fun Scene.text(s: String, size: Float, c: Color, cx: Float, cy: Float) {
    val l = m.measure(s, TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = with(d) { size.toSp() }, color = c))
    d.drawText(l, topLeft = Offset(cx - l.size.width / 2f, cy - l.size.height / 2f))
}

private fun Scene.plus(x: Float, y: Float, c: Color = Ink, w: Float = 3f, h: Float = 5f) {
    d.drawLine(c, Offset(x - h, y), Offset(x + h, y), w, StrokeCap.Round)
    d.drawLine(c, Offset(x, y - h), Offset(x, y + h), w, StrokeCap.Round)
}

private fun Scene.dot(x: Float, y: Float, r: Float, c: Color, a: Float = 0.5f) = d.drawCircle(c.copy(alpha = a), r, Offset(x, y))

private fun Scene.miniCard(x: Float, y: Float, rot: Float, sc: Float, glyph: String, col: Color, bob: Float = 0f) {
    // Every small card drifts a little: a slow float and sway, each out of step with its neighbours.
    val ph = x * 0.011f + y * 0.007f
    val by = y + bob + wave(t, 3.4f, ph) * 2.2f
    val r = rot + wave(t, 4.6f, ph + 0.25f) * 2.5f
    d.withTransform({ translate(x, by); rotate(r, Offset.Zero); scale(sc, sc, Offset.Zero) }) {
        drawRoundRect(Ink.copy(alpha = 0.35f), Offset(-19f, -24f), Size(40f, 56f), CornerRadius(8f))
        drawRoundRect(Color.White, Offset(-20f, -28f), Size(40f, 56f), CornerRadius(8f))
        drawRoundRect(Ink, Offset(-20f, -28f), Size(40f, 56f), CornerRadius(8f), Stroke(2.6f))
    }
    d.withTransform({ translate(x, by); rotate(r, Offset.Zero); scale(sc, sc, Offset.Zero) }) { Scene(this, m, t).text(glyph, 30f, col, 0f, 0f) }
}

private fun wave(t: Float, period: Float, phase: Float = 0f) = sin(2f * PI.toFloat() * (t / period + phase))

/** A Wi-Fi arc with chord half-width [h] and radius [r], whose lower end sits at y = [y0]. */
private fun Scene.arc(r: Float, h: Float, y0: Float, c: Color, w: Float, a: Float = 1f) {
    val cy = y0 + sqrt(r * r - h * h)
    val half = Math.toDegrees(asin(h / r).toDouble()).toFloat()
    d.drawArc(c.copy(alpha = a), -90f - half, 2f * half, false, Offset(100f - r, cy - r), Size(2f * r, 2f * r), style = Stroke(w, cap = StrokeCap.Round))
}

private fun Scene.trophy(cx: Float, cy: Float, s: Float, fill: Color = Color.White) {
    val cup = Path().also { PathParser().parsePathString("M8 4h8v5a4 4 0 0 1-8 0z").toPath(it) }
    val rest = Path().also { PathParser().parsePathString("M8 5H4v2a3 3 0 0 0 4 3M16 5h4v2a3 3 0 0 1-4 3M12 13v4M8 20h8").toPath(it) }
    d.withTransform({ translate(cx - 12f * s, cy - 12f * s); scale(s, s, Offset.Zero) }) {
        drawPath(cup, fill)
        drawPath(cup, Ink, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(rest, Ink, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The Combine card fan: four cards swaying a little over two tinted discs. */
@Composable
fun CombineArt(modifier: Modifier = Modifier, glyphs: List<String> = listOf("6", "×", "7", "+")) = Art(modifier) {
    disc(100f, 78f, 68f, Pale1); disc(100f, 78f, 52f, Pale2)
    val cols = listOf(TealDark, Terra, TealDark, Terra)
    val xs = listOf(58f, 88f, 120f, 150f); val ys = listOf(92f, 82f, 82f, 92f); val rs = listOf(-16f, -6f, 6f, 16f)
    for (i in 0..3) miniCard(xs[i], ys[i], rs[i] + wave(t, 5f, i * 0.2f) * 2.5f, 1.1f, glyphs[i], cols[i], wave(t, 4f, i * 0.25f) * 1.5f)
    plus(100f, 14f, Ink, 3f, 5f); plus(186f, 134f, Ink, 3f, 5f)
    dot(24f, 52f, 3.5f, Ink); dot(14f, 128f, 4f, Color.White, 1f)
}

/** Host tile: the Wi-Fi arcs ping one after another. */
@Composable
fun HostArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 86f, 70f, Color(0xFFCFF0EA)); disc(100f, 86f, 56f, Color(0xFF9ED9CF))
    val specs = listOf(Triple(60f, 42f, 78f), Triple(40f, 28f, 94f), Triple(20f, 14f, 110f))
    specs.forEachIndexed { i, (r, h, y) ->
        d.withTransform({ translate(0f, 3f) }) { Scene(this, m, t).arc(r, h, y, Ink, 9f, 0.3f) }
        val p = (t / 1.8f - (2 - i) * 0.1667f) % 1f
        val a = 0.25f + 0.75f * (1f - kotlin.math.abs(((p + 1f) % 1f) - 0.45f).coerceIn(0f, 0.45f) / 0.45f)
        arc(r, h, y, Ink, 9f, a)
    }
    disc(100f, 123f, 7.5f, Ink)
    miniCard(32f, 38f, -16f, 0.72f, "7", TealDark); miniCard(174f, 52f, 14f, 0.55f, "+", Terra)
    miniCard(164f, 116f, 10f, 0.68f, "3", TealDark)
    dot(22f, 86f, 3f, Ink); dot(180f, 80f, 3f, Ink); plus(100f, 16f, Color.White)
}

/** Join tile: a portal whose rings pulse outward while the arrow nudges in. */
@Composable
fun JoinArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 86f, 70f, Color(0xFFF7C9D3)); disc(100f, 86f, 56f, Color(0xFFF2A9B9))
    for (k in 0..1) {
        val p = ((t / 2f) + k * 0.5f) % 1f
        circleLine(100f, 86f, 45f * (1f + 0.7f * p), Color.White, 3f, 0.7f * (1f - p))
    }
    shadowDisc(100f, 86f, 45f); disc(100f, 86f, 45f, Ink); disc(100f, 86f, 35f, Color(0xFFF2C778)); disc(100f, 86f, 24f, Pale1)
    val nudge = (wave(t, 1.2f) + 1f) * 2.5f
    d.withTransform({ translate(nudge, 0f) }) {
        drawLine(Ink, Offset(82f, 86f), Offset(114f, 86f), 7f, StrokeCap.Round)
        drawLine(Ink, Offset(102f, 72f), Offset(116f, 86f), 7f, StrokeCap.Round)
        drawLine(Ink, Offset(116f, 86f), Offset(102f, 100f), 7f, StrokeCap.Round)
    }
    miniCard(164f, 36f, -12f, 0.7f, "×", Terra); miniCard(172f, 108f, 18f, 0.55f, "9", TealDark)
    miniCard(36f, 114f, 12f, 0.7f, "7", TealDark)
    dot(22f, 86f, 3f, Color.White); dot(180f, 80f, 3f, Color.White); plus(100f, 16f, Color.White)
}

/** Solo tile: a stopwatch whose amber pie fills clockwise exactly as the needle sweeps. */
@Composable
fun SoloArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 86f, 70f, Color(0xFFF7D3C0)); disc(100f, 86f, 56f, Color(0xFFEFB596))
    d.drawRoundRect(Ink, Offset(94f, 30f), Size(12f, 9f), CornerRadius(3f)); d.drawRoundRect(Ink, Offset(88f, 26f), Size(24f, 6f), CornerRadius(3f))
    shadowDisc(100f, 86f, 45f); disc(100f, 86f, 45f, Color.White); circleLine(100f, 86f, 44f, Ink, 3.4f)
    val frac = (t % 6f) / 6f
    d.drawArc(Amber, -90f, 360f * frac, true, Offset(67f, 53f), Size(66f, 66f))
    for (k in 0 until 12) {
        val a = k * 30f * PI.toFloat() / 180f
        d.drawLine(Ink, Offset(100f + 34f * sin(a), 86f - 34f * cos(a)), Offset(100f + 29f * sin(a), 86f - 29f * cos(a)), 2.6f, StrokeCap.Round)
    }
    val a = frac * 2f * PI.toFloat()
    d.drawLine(Ink, Offset(100f, 86f), Offset(100f + 24f * sin(a), 86f - 24f * cos(a)), 3.6f, StrokeCap.Round)
    val h = (t % 72f) / 72f * 2f * PI.toFloat() + 2.6f
    d.drawLine(Terra, Offset(100f, 86f), Offset(100f + 12f * sin(h), 86f - 12f * cos(h)), 3.2f, StrokeCap.Round)
    disc(100f, 86f, 5f, Ink)
    miniCard(36f, 46f, 10f, 0.7f, "5", TealDark); miniCard(30f, 110f, -16f, 0.58f, "+", Terra)
    miniCard(170f, 42f, -9f, 0.75f, "9", TealDark)
    dot(22f, 86f, 3f, Ink); dot(180f, 80f, 3f, Ink); plus(100f, 16f, Ink)
}

/** A question card on a tint: the "coming soon" placeholder. */
@Composable
fun SoonArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 76f, 62f, Color(0xFFEBDCC2)); disc(100f, 76f, 48f, Color(0xFFF4E4CB))
    miniCard(100f, 76f, -6f + wave(t, 4f) * 3f, 1.5f, "?", TealDark, wave(t, 3f) * 2f)
}

/** The LanPlay mark: Wi-Fi waves over a play button, on an amber rounded square. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, bg: Color = Amber, animate: Boolean = true, shadow: Boolean = true) {
    val clock by rememberInfiniteTransition(label = "logo").animateFloat(0f, 360f, infiniteRepeatable(tween(360_000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) {
        val s = min(size.width, size.height) / 120f
        withTransform({ scale(s, s, Offset.Zero) }) {
            if (shadow) drawRoundRect(Ink, Offset(8f, 12f), Size(104f, 104f), CornerRadius(28f))
            drawRoundRect(bg, Offset(6f, 6f), Size(104f, 104f), CornerRadius(28f))
            drawRoundRect(Ink, Offset(6f, 6f), Size(104f, 104f), CornerRadius(28f), Stroke(4f))
            val specs = listOf(Triple(50f, 36f, 44f), Triple(34f, 25f, 56f), Triple(18f, 14f, 68f))
            specs.forEachIndexed { i, (r, h, y) ->
                val p = if (animate) ((clock / 1.8f - (2 - i) * 0.1667f) % 1f + 1f) % 1f else 0.45f
                val a = 0.25f + 0.75f * (1f - kotlin.math.abs(p - 0.45f).coerceIn(0f, 0.45f) / 0.45f)
                val cy = y + sqrt(r * r - h * h)
                val half = Math.toDegrees(asin(h / r).toDouble()).toFloat()
                drawArc(Ink.copy(alpha = a), -90f - half, 2f * half, false, Offset(60f - r, cy - r), Size(2f * r, 2f * r), style = Stroke(6f, cap = StrokeCap.Round))
            }
            val tri = Path().apply { moveTo(51f, 76f); lineTo(73f, 88f); lineTo(51f, 100f); close() }
            drawPath(tri, Teal); drawPath(tri, Ink, style = Stroke(4f, join = StrokeJoin.Round))
        }
    }
}

/** The round winner card: bobbing trophy, corner cards and confetti falling through. */
@Composable
fun WinnerArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 82f, 68f, Pale1); disc(100f, 82f, 52f, Pale2)
    miniCard(34f, 44f, -14f, 0.7f, "6", TealDark); miniCard(168f, 40f, 12f, 0.7f, "×", Terra)
    miniCard(160f, 116f, -10f, 0.6f, "4", TealDark); miniCard(38f, 114f, 12f, 0.6f, "=", Terra)
    val bob = wave(t, 2.4f) * -3f - 3f
    d.withTransform({ translate(0f, bob) }) {
        Scene(this, m, t).apply {
            shadowDisc(100f, 82f, 42f); disc(100f, 82f, 42f, Teal); circleLine(100f, 82f, 42f, Ink, 3.4f)
            trophy(100f, 80f, 2.6f)
        }
    }
    val cols = listOf(Teal, Rose, Color.White, Terra, Teal, Rose, Color.White)
    val xs = listOf(20f, 48f, 78f, 122f, 150f, 178f, 100f); val per = listOf(3.2f, 2.8f, 3.6f, 3f, 3.4f, 2.9f, 3.1f); val del = listOf(0f, .6f, 1.1f, .3f, 1.6f, .9f, 2f)
    for (i in xs.indices) {
        val p = (((t - del[i]) / per[i]) % 1f + 1f) % 1f
        val y = -20f + p * 190f
        d.withTransform({ translate(xs[i], y); rotate(p * 360f, Offset(3f, 5f)) }) {
            drawRect(cols[i].copy(alpha = if (p < 0.15f) p / 0.15f else if (p > 0.85f) (1f - p) / 0.15f else 1f), Offset.Zero, Size(6f, 11f))
            drawRect(Ink, Offset.Zero, Size(6f, 11f), style = Stroke(1.6f))
        }
    }
    plus(100f, 12f, Ink, 3f, 5f)
}

/** The solo loss card: an alarm clock with a sad face and a tear, corner cards drifting. */
@Composable
fun LoserArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 82f, 68f, Color(0xFFF7BFCB)); disc(100f, 82f, 52f, Color(0xFFF2A3B4))
    miniCard(34f, 44f, -14f, 0.7f, "6", TealDark); miniCard(168f, 40f, 12f, 0.7f, "×", Terra)
    miniCard(160f, 116f, -10f, 0.6f, "4", TealDark); miniCard(38f, 114f, 12f, 0.6f, "=", Terra)
    d.withTransform({ translate(0f, wave(t, 3f) * -2f) }) {
        Scene(this, m, t).apply {
            for (bx in listOf(76f, 124f)) { disc(bx, 46f, 10f, Amber); circleLine(bx, 46f, 10f, Ink, 3f) }
            d.drawLine(Ink, Offset(100f, 36f), Offset(100f, 30f), 3.4f, StrokeCap.Round)
            shadowDisc(100f, 82f, 42f); disc(100f, 82f, 42f, Color.White); circleLine(100f, 82f, 42f, Ink, 3.4f)
            for ((x1, y1, x2, y2) in listOf(listOf(100f, 46f, 100f, 51f), listOf(100f, 113f, 100f, 118f), listOf(64f, 82f, 69f, 82f), listOf(131f, 82f, 136f, 82f)))
                d.drawLine(Ink, Offset(x1, y1), Offset(x2, y2), 2.6f, StrokeCap.Round)
            disc(87f, 76f, 4.6f, Ink); disc(113f, 76f, 4.6f, Ink)
            val frown = Path().apply { moveTo(85f, 101f); quadraticBezierTo(100f, 88f, 115f, 101f) }
            d.drawPath(frown, Ink, style = Stroke(3.6f, cap = StrokeCap.Round))
            val tear = Path().also { PathParser().parsePathString("M121 84c4 6 6 9 6 12a6 6 0 0 1-12 0c0-3 2-6 6-12z").toPath(it) }
            d.drawPath(tear, Teal); d.drawPath(tear, Ink, style = Stroke(2.4f, join = StrokeJoin.Round))
        }
    }
}

/** A big round state picture (joining, failed, removed, disconnected): a disc, a white face and one icon. */
enum class StateArtKind { Joining, Failed, Kicked, Disconnected }

@Composable
fun StateArt(kind: StateArtKind, modifier: Modifier = Modifier) = Art(modifier) {
    val (c1, c2) = when (kind) {
        StateArtKind.Joining -> Color(0xFFF7C9D3) to Color(0xFFF2A9B9)
        StateArtKind.Failed -> Color(0xFFF7D3C0) to Color(0xFFEFB596)
        StateArtKind.Kicked -> Pale1 to Pale2
        StateArtKind.Disconnected -> Color(0xFFCFF0EA) to Color(0xFF9ED9CF)
    }
    disc(100f, 76f, 66f, c1); disc(100f, 76f, 52f, c2)
    if (kind == StateArtKind.Joining) for (k in 0..1) {
        val p = ((t / 2f) + k * 0.5f) % 1f
        circleLine(100f, 76f, 40f * (1f + 0.7f * p), Rose, 3f, 0.8f * (1f - p))
    }
    shadowDisc(100f, 76f, 40f); disc(100f, 76f, 40f, Color.White); circleLine(100f, 76f, 40f, Ink, 3.4f)
    val w = 6f
    when (kind) {
        StateArtKind.Joining -> {
            d.drawArc(Rose, (t % 1.2f) / 1.2f * 360f - 90f, 270f, false, Offset(78f, 54f), Size(44f, 44f), style = Stroke(w, cap = StrokeCap.Round))
            disc(100f, 76f, 5f, Ink)
        }
        StateArtKind.Failed, StateArtKind.Disconnected -> {
            val a = if (kind == StateArtKind.Failed) 1f else 0.35f + 0.65f * (wave(t, 2f) + 1f) / 2f
            arc(36f, 26f, 66f, Ink, w, a); arc(21f, 16f, 78f, Ink, w, a); disc(100f, 90f, 4.5f, Ink.copy(alpha = a))
            if (kind == StateArtKind.Failed) {
                val sh = wave(t, 0.6f) * 3f
                d.withTransform({ translate(sh, 0f) }) {
                    drawCircle(Rose, 16f, Offset(128f, 100f)); drawCircle(Ink, 16f, Offset(128f, 100f), style = Stroke(3f))
                    drawLine(Ink, Offset(121f, 93f), Offset(135f, 107f), 4f, StrokeCap.Round); drawLine(Ink, Offset(135f, 93f), Offset(121f, 107f), 4f, StrokeCap.Round)
                }
            } else d.drawLine(Rose, Offset(72f, 108f), Offset(128f, 48f), 7f, StrokeCap.Round)
        }
        StateArtKind.Kicked -> {
            val n = (wave(t, 1.4f) + 1f) * 3f
            d.withTransform({ translate(n, 0f) }) {
                drawRoundRect(Ink, Offset(78f, 56f), Size(22f, 40f), CornerRadius(3f), style = Stroke(w))
                drawLine(Ink, Offset(100f, 76f), Offset(126f, 76f), w, StrokeCap.Round)
                drawLine(Ink, Offset(116f, 64f), Offset(128f, 76f), w, StrokeCap.Round); drawLine(Ink, Offset(128f, 76f), Offset(116f, 88f), w, StrokeCap.Round)
            }
        }
    }
    plus(100f, 12f, Ink, 3f, 5f); dot(22f, 78f, 3f, Ink); dot(180f, 72f, 3f, Ink)
}

/** A round gear for the Settings badge. */
@Composable
fun GearArt(modifier: Modifier = Modifier) = Art(modifier) {
    val c = Offset(100f, 75f)
    for (k in 0 until 8) d.withTransform({ rotate(k * 45f, c) }) { drawRoundRect(Ink, Offset(94f, 22f), Size(12f, 18f), CornerRadius(3f)) }
    d.drawCircle(Ink, 38f, c); d.drawCircle(Color(0xFFEFB596), 16f, c)
}

/** Floating sparkles and dots around a hero: used by the Credits card. */
@Composable
fun SparkleArt(modifier: Modifier = Modifier) = Art(modifier) {
    disc(100f, 80f, 74f, Pale1); disc(100f, 80f, 58f, Pale2)
    val sp = listOf(Triple(60f, 22f, 0f), Triple(150f, 60f, .6f), Triple(48f, 100f, 1.1f))
    sp.forEach { (x, y, ph) ->
        val a = 0.1f + 0.9f * (wave(t, 1.8f, ph / 1.8f) + 1f) / 2f
        plus(x, y, Ink.copy(alpha = a), 2.6f, 5f)
    }
    val dots = listOf(listOf(26f, 40f, 7f), listOf(176f, 34f, 9f), listOf(168f, 128f, 6f), listOf(32f, 124f, 8f))
    val cs = listOf(Rose, Teal, Color.White, Amber)
    dots.forEachIndexed { i, (x, y, r) ->
        val by = wave(t, 3f + i * 0.4f) * 3f
        d.drawCircle(cs[i], r, Offset(x, y + by)); d.drawCircle(Ink, r, Offset(x, y + by), style = Stroke(2f))
    }
}
