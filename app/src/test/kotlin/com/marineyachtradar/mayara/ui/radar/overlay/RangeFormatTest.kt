package com.marineyachtradar.mayara.ui.radar.overlay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [formatRange].
 *
 * The range display must use a stable, predictable format so the monospace
 * font layout doesn't jitter at boundary values.
 */
class RangeFormatTest {

    @Test
    fun `1852 metres formats as exactly 1 NM`() {
        assertEquals("1.0 NM", formatRange(1852))
    }

    @Test
    fun `926 metres (half NM) formats as 0_5 NM`() {
        assertEquals("0.5 NM", formatRange(926))
    }

    @Test
    fun `3704 metres formats as 2_0 NM`() {
        assertEquals("2.0 NM", formatRange(3704))
    }

    @Test
    fun `300 metres (minimum range) formats without throwing`() {
        val result = formatRange(300)
        assertTrue(result.endsWith("NM"), "expected NM suffix but got: $result")
    }

    @Test
    fun `192000 metres (maximum HALO range) formats without throwing`() {
        val result = formatRange(192000)
        assertTrue(result.endsWith("NM"), "expected NM suffix but got: $result")
    }

    @Test
    fun `output always ends in NM suffix`() {
        listOf(300, 600, 1200, 3700, 7400, 18520, 37040).forEach { metres ->
            val result = formatRange(metres)
            assertTrue(result.endsWith("NM"), "formatRange($metres) = '$result' — missing NM")
        }
    }

    @Test
    fun `output always has exactly one decimal place`() {
        listOf(300, 1852, 3704, 18520).forEach { metres ->
            val value = formatRange(metres).removeSuffix(" NM")
            val dotIndex = value.indexOf('.')
            assertTrue(dotIndex >= 0, "No decimal point in '$value'")
            assertEquals(1, value.length - dotIndex - 1, "Expected 1 decimal place in '$value'")
        }
    }
}
