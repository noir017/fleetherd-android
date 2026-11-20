package io.github.noir017.fleetherd

import io.github.noir017.fleetherd.web.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorUtilsTest {
    @Test
    fun parsesHexFormats() {
        assertEquals(0xFF131313.toInt(), ColorUtils.parseCssColor("#131313"))
        assertEquals(0xFFFFFFFF.toInt(), ColorUtils.parseCssColor("#fff"))
        assertEquals(0xFF112233.toInt(), ColorUtils.parseCssColor("#123"))
        assertEquals(0x80131313.toInt(), ColorUtils.parseCssColor("#80131313"))
    }

    @Test
    fun parsesRgbAndRgba() {
        assertEquals(0xFF171717.toInt(), ColorUtils.parseCssColor("rgb(23, 23, 23)"))
        assertEquals(0xFF1C222B.toInt(), ColorUtils.parseCssColor("rgba(28, 34, 43, 1)"))
        assertNull(ColorUtils.parseCssColor("rgba(0, 0, 0, 0)"))
        assertNull(ColorUtils.parseCssColor("transparent"))
        assertNull(ColorUtils.parseCssColor("not-a-color"))
    }

    @Test
    fun detectsLightVsDarkLuminance() {
        assertFalse(ColorUtils.isLightColor(0xFF131313.toInt()))
        assertFalse(ColorUtils.isLightColor(0xFF171717.toInt()))
        assertTrue(ColorUtils.isLightColor(0xFFFFFFFF.toInt()))
        assertTrue(ColorUtils.isLightColor(0xFFF3F4F6.toInt()))
    }
}
