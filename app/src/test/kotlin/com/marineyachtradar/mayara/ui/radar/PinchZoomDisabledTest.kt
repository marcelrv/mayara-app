package com.marineyachtradar.mayara.ui.radar

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests confirming that [DisabledPinchZoom] never allows zoom to be applied.
 *
 * This is a safety-critical requirement (spec §3.2): the on-screen radar scale must
 * always reflect the hardware-stepped range value from the radar unit itself.
 * Pinch-to-zoom is permanently disabled at the class level.
 */
class PinchZoomDisabledTest {

    private val subject = DisabledPinchZoom()

    @Test
    fun `shouldApplyScale returns false when factor is greater than 1 (zoom in)`() {
        assertFalse(subject.shouldApplyScale(1.5f))
    }

    @Test
    fun `shouldApplyScale returns false when factor is less than 1 (zoom out)`() {
        assertFalse(subject.shouldApplyScale(0.5f))
    }

    @Test
    fun `shouldApplyScale returns false when factor equals 1 (no change)`() {
        assertFalse(subject.shouldApplyScale(1.0f))
    }
}
