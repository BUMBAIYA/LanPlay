package com.bumbaiya.lanplay.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

// ---------- icons: the design's own line icons, drawn from the same SVG paths ----------

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r $r" + "v${h - 2 * r}a$r $r 0 0 1 ${-r} $r" + "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}" + "v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun icon(name: String, stroke: Float, vararg paths: String, size: Float = 24f, filled: Boolean = false) =
    ImageVector.Builder(name, 24.dp, 24.dp, size, size).apply {
        paths.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(), fill = if (filled) SolidColor(Color.Black) else null, stroke = SolidColor(Color.Black), strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

object DI {
    val Right = icon("right", 2.5f, "M5 12h14M13 6l6 6-6 6")
    val Back = icon("back", 2.5f, "M19 12H5M11 6l-6 6 6 6")
    val Host = icon("host", 2.25f, "M3 9a13 13 0 0 1 18 0M6.5 12.5a8 8 0 0 1 11 0M10 16a3 3 0 0 1 4 0", circle(12f, 19.5f, 1f))
    val Join = icon("join", 2.25f, "M14 4h5v16h-5M10 8l4 4-4 4M14 12H4")
    val Solo = icon("solo", 2.25f, circle(12f, 8f, 4f), "M4 20a8 8 0 0 1 16 0")
    val Sliders = icon("sliders", 2.25f, "M4 6h8M18 6h2M4 12h2M12 12h8M4 18h10", circle(15f, 6f, 2.5f), circle(9f, 12f, 2.5f), circle(17f, 18f, 2.5f))
    val Scan = icon("scan", 2.25f, rect(3f, 3f, 7f, 7f, 1f), rect(14f, 3f, 7f, 7f, 1f), rect(3f, 14f, 7f, 7f, 1f), "M14 14h3v3M21 14v1M14 21h1M18 21h3v-4")
    val Check = icon("check", 2.75f, "M5 12l5 5 9-9")
    val Close = icon("close", 2.5f, "M6 6l12 12M18 6L6 18")
    val Play = icon("play", 2.5f, "M7 5l12 7-12 7z")
    val Pass = icon("pass", 2.5f, "M5 5l9 7-9 7zM18 5v14")
    val DrawCard = icon("draw", 2.5f, rect(6f, 3f, 12f, 18f, 2f), "M12 9v6M9 12h6")
    val Speaker = icon("speaker", 2.25f, "M4 9v6h4l5 4V5L8 9H4z", "M16.5 9a4 4 0 0 1 0 6M19 6.5a8 8 0 0 1 0 11")
    val SpeakerOff = icon("speakerOff", 2.25f, "M4 9v6h4l5 4V5L8 9H4z", "M17 9l5 6M22 9l-5 6")
    val Leave = icon("leave", 2.5f, "M10 4H5v16h5M14 8l4 4-4 4M18 12H9")
    val Torch = icon("torch", 2.5f, "M13 3L5 14h6l-1 7 8-11h-6z")
    /** A die showing five: reads as "random". */
    val Spinner = icon("dice", 2.25f, rect(4f, 4f, 16f, 16f, 3f), circle(8.5f, 8.5f, 1f), circle(15.5f, 8.5f, 1f), circle(12f, 12f, 1f), circle(8.5f, 15.5f, 1f), circle(15.5f, 15.5f, 1f))
    val Book = icon("book", 2.25f, "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z", "M4 19V5M9 8h6")
    val Tour = icon("tour", 2.25f, "M7 5l12 7-12 7z", circle(12f, 12f, 10f))
    /** The LanPlay mark: radio waves, on a 32 grid. */
    val Trophy = icon("trophy", 2.25f, "M8 4h8v5a4 4 0 0 1-8 0z", "M8 5H4v2a3 3 0 0 0 4 3M16 5h4v2a3 3 0 0 1-4 3M12 13v4M8 20h8")
    val Heart = icon("heart", 2.25f, "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.4A4 4 0 0 1 19 10c0 5.6-7 10-7 10z")
    val HeartFilled = icon("heartFilled", 2f, "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.4A4 4 0 0 1 19 10c0 5.6-7 10-7 10z", filled = true)
    val Eye = icon("eye", 2.25f, "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z", circle(12f, 12f, 3f))
    val Minus = icon("minus", 3f, "M6 12h12")
    val Plus = icon("plus", 3f, "M6 12h12M12 6v12")
    val Logo = icon("logo", 2.5f, "M5 12a16 16 0 0 1 22 0", "M9 17a10 10 0 0 1 14 0", circle(16f, 23f, 2f), size = 32f)
}

@Composable
fun DIcon(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onBackground, size: Dp = 24.dp) =
    Icon(icon, null, modifier.size(size), tint = tint)

