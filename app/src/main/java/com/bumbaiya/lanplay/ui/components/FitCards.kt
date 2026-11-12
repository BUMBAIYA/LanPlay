package com.bumbaiya.lanplay.ui.components

import androidx.compose.ui.unit.Dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * How much to shrink cards so [count] of them fit in [availW] x [availH], wrapping onto more rows
 * when one row is too narrow. Returns [maxScale] (1 = normal size) when there is room to spare, and never goes below
 * [minScale] (the cards stay readable). [extraW]/[extraH] are per-card space that does not shrink.
 * This is what lets the table handle any screen size or any number of cards without scrolling.
 */
fun fitScale(
    count: Int, cardW: Dp, cardH: Dp, gap: Dp, availW: Dp, availH: Dp,
    extraW: Dp = Dp(0f), extraH: Dp = Dp(0f), minScale: Float = 0.4f, maxScale: Float = 1f,
): Float {
    if (count <= 0 || availW.value <= 0f || availH.value <= 0f) return 1f
    var s = maxScale
    while (s > minScale) {
        val itemW = cardW.value * s + extraW.value
        val perRow = max(1, floor((availW.value + gap.value) / (itemW + gap.value)).toInt())
        val rows = ceil(count.toFloat() / perRow).toInt()
        val height = rows * (cardH.value * s + extraH.value) + (rows - 1) * gap.value
        if (height <= availH.value) return s
        s -= 0.05f
    }
    return minScale
}
