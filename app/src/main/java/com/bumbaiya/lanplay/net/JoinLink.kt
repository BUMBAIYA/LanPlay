package com.bumbaiya.lanplay.net

/**
 * What the host's QR code contains. It carries the address as a backup in case the network
 * blocks the automatic room search: scanning gets you in without typing anything.
 */
data class JoinLink(val room: String, val host: String, val port: Int) {
    fun toText() = "lanplay://join?room=$room&host=$host&port=$port"

    companion object {
        fun parse(text: String): JoinLink? {
            val t = text.trim()
            if (!t.startsWith("lanplay://join?")) return null
            val q = t.substringAfter('?').split('&').mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }.toMap()
            val room = q["room"]?.takeIf { RoomIds.isValid(it) } ?: return null
            val host = q["host"]?.takeIf { it.isNotBlank() } ?: return null
            val port = q["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            return JoinLink(room.uppercase(), host, port)
        }
    }
}

/** Room IDs are short and easy to read out loud: no 0/O or 1/I mix-ups. */
object RoomIds {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 4

    fun generate(random: kotlin.random.Random = kotlin.random.Random.Default) =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    fun normalize(input: String) = input.trim().uppercase()

    fun isValid(id: String) = id.length == LENGTH && id.uppercase().all { it in ALPHABET }
}
