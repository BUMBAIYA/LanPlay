package com.bumbaiya.lanplay.game.combinemaths

/** Player names are letters only: no digits, spaces or symbols, and at most [MAX] characters. */
object PlayerName {
    const val MAX = 15
    fun clean(raw: String) = raw.filter { it.isLetter() }.take(MAX)
}
