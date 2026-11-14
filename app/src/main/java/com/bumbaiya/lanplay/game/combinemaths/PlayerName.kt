package com.bumbaiya.lanplay.game.combinemaths

/** Player names are letters only: no digits, spaces or symbols, and at most [MAX] characters. */
object PlayerName {
    const val MAX = 15
    /** How much a field lets people type before the validation message takes over. */
    const val TYPE_LIMIT = 30

    fun clean(raw: String) = raw.filter { it.isLetter() }.take(MAX)

    /** Why [raw] can't be used as a name, or null if it can (or is still empty). */
    fun problem(raw: String): String? {
        if (raw.isEmpty()) return null
        val bad = raw.filterNot { it.isLetter() }.toList().distinct()
        if (bad.isNotEmpty()) return "Can't use ${bad.take(4).joinToString(", ") { if (it == ' ') "space" else "\"$it\"" }}. Letters only."
        if (raw.length > MAX) return "Up to $MAX letters."
        return null
    }

    fun isValid(raw: String) = raw.isNotEmpty() && problem(raw) == null
}
