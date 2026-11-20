package io.github.noir017.fleetherd.web

/**
 * Pure Kotlin CSS color parsing and luminance helpers (JVM unit-testable without Robolectric).
 */
object ColorUtils {
    private val HEX_3 = Regex("^#([0-9a-fA-F]{3})$")
    private val HEX_6 = Regex("^#([0-9a-fA-F]{6})$")
    private val HEX_8 = Regex("^#([0-9a-fA-F]{8})$")
    private val RGB_FUNC = Regex(
        "^rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})(?:\\s*,\\s*([0-9.]+))?\\s*\\)$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses a CSS color string (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `rgb(...)`, `rgba(...)`)
     * into an opaque ARGB int. Returns null for transparent (`alpha == 0`) or invalid strings.
     */
    fun parseCssColor(raw: String?): Int? {
        val s = raw?.trim() ?: return null
        if (s.isEmpty() || s.equals("transparent", ignoreCase = true)) return null

        HEX_3.matchEntire(s)?.let { m ->
            val h = m.groupValues[1]
            val r = "${h[0]}${h[0]}".toInt(16)
            val g = "${h[1]}${h[1]}".toInt(16)
            val b = "${h[2]}${h[2]}".toInt(16)
            return argb(255, r, g, b)
        }

        HEX_6.matchEntire(s)?.let { m ->
            val rgb = m.groupValues[1].toInt(16)
            return (0xFF shl 24) or rgb
        }

        HEX_8.matchEntire(s)?.let { m ->
            val v = m.groupValues[1].toLong(16)
            val a = ((v shr 24) and 0xFF).toInt()
            if (a == 0) return null
            return v.toInt()
        }

        RGB_FUNC.matchEntire(s)?.let { m ->
            val r = m.groupValues[1].toIntOrNull() ?: return null
            val g = m.groupValues[2].toIntOrNull() ?: return null
            val b = m.groupValues[3].toIntOrNull() ?: return null
            if (r !in 0..255 || g !in 0..255 || b !in 0..255) return null
            val alphaStr = m.groupValues[4]
            if (alphaStr.isNotEmpty()) {
                val a = alphaStr.toFloatOrNull() ?: return null
                if (a <= 0.01f) return null
            }
            return argb(255, r, g, b)
        }

        return null
    }

    /**
     * Returns true when a color is perceptually light (meaning dark status/nav bar icons should be used).
     */
    fun isLightColor(argb: Int): Boolean {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val luma = 0.299 * r + 0.587 * g + 0.114 * b
        return luma >= 155.0
    }

    fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a and 0xFF) shl 24) or
            ((r and 0xFF) shl 16) or
            ((g and 0xFF) shl 8) or
            (b and 0xFF)
    }
}
