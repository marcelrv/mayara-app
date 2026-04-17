package com.marineyachtradar.mayara.ui.radar.bottomsheet

import com.marineyachtradar.mayara.data.model.ColorPalette
import com.marineyachtradar.mayara.data.model.RadarOrientation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the display-name helper functions used in [RadarControlSheet].
 *
 * Each palette and orientation entry must have a distinct, non-blank display name
 * so the UI chip labels are readable and unique.
 */
class ControlSheetDisplayNameTest {

    // ------------------------------------------------------------------
    // Palette display names
    // ------------------------------------------------------------------

    @Test
    fun `all ColorPalette entries have a non-blank display name`() {
        ColorPalette.entries.forEach { palette ->
            val name = paletteDisplayName(palette)
            assertFalse(name.isBlank(), "paletteDisplayName($palette) returned blank string")
        }
    }

    @Test
    fun `all ColorPalette display names are unique`() {
        val names = ColorPalette.entries.map { paletteDisplayName(it) }
        assertEquals(names.size, names.toSet().size, "Duplicate palette display names: $names")
    }

    @Test
    fun `GREEN palette displays as Green`() {
        assertEquals("Green", paletteDisplayName(ColorPalette.GREEN))
    }

    @Test
    fun `YELLOW palette displays as Yellow`() {
        assertEquals("Yellow", paletteDisplayName(ColorPalette.YELLOW))
    }

    @Test
    fun `MULTI_COLOR palette displays as Multi`() {
        assertEquals("Multi", paletteDisplayName(ColorPalette.MULTI_COLOR))
    }

    @Test
    fun `NIGHT_RED palette displays as Night`() {
        assertEquals("Night", paletteDisplayName(ColorPalette.NIGHT_RED))
    }

    // ------------------------------------------------------------------
    // Orientation display names
    // ------------------------------------------------------------------

    @Test
    fun `all RadarOrientation entries have a non-blank display name`() {
        RadarOrientation.entries.forEach { orientation ->
            val name = orientationDisplayName(orientation)
            assertFalse(name.isBlank(), "orientationDisplayName($orientation) returned blank string")
        }
    }

    @Test
    fun `all RadarOrientation display names are unique`() {
        val names = RadarOrientation.entries.map { orientationDisplayName(it) }
        assertEquals(names.size, names.toSet().size, "Duplicate orientation display names: $names")
    }

    @Test
    fun `HEAD_UP orientation displays as Head Up`() {
        assertEquals("Head Up", orientationDisplayName(RadarOrientation.HEAD_UP))
    }

    @Test
    fun `NORTH_UP orientation displays as North Up`() {
        assertEquals("North Up", orientationDisplayName(RadarOrientation.NORTH_UP))
    }

    @Test
    fun `COURSE_UP orientation displays as Course Up`() {
        assertEquals("Course Up", orientationDisplayName(RadarOrientation.COURSE_UP))
    }

    // ------------------------------------------------------------------
    // Stability: adding a new enum value should fail loudly
    // ------------------------------------------------------------------

    @Test
    fun `palette display name coverage - count matches enum entries`() {
        // If a new ColorPalette entry is added without updating paletteDisplayName,
        // either the when() will fail to compile (exhaustive) or this count will catch it.
        assertEquals(4, ColorPalette.entries.size, "Expected 4 ColorPalette values; update paletteDisplayName() if you added one")
    }

    @Test
    fun `orientation display name coverage - count matches enum entries`() {
        assertEquals(3, RadarOrientation.entries.size, "Expected 3 RadarOrientation values; update orientationDisplayName() if you added one")
    }

    // ------------------------------------------------------------------
    // Palette and orientation names must not collide with each other
    // ------------------------------------------------------------------

    @Test
    fun `palette and orientation display names do not overlap`() {
        val paletteNames = ColorPalette.entries.map { paletteDisplayName(it) }.toSet()
        val orientationNames = RadarOrientation.entries.map { orientationDisplayName(it) }.toSet()
        val intersection = paletteNames.intersect(orientationNames)
        assertEquals(
            emptySet<String>(),
            intersection,
            "Palette and orientation names overlap: $intersection",
        )
    }
}
