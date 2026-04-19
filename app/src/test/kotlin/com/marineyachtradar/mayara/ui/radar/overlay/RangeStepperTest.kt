package com.marineyachtradar.mayara.ui.radar.overlay

import com.marineyachtradar.mayara.data.model.DistanceUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RangeStepperTest {

    // Typical Navico 4G-style ranges (mix of nautical + metric).
    private val typicalRanges = listOf(
        50, 75, 100, 250, 500, 750,
        926, 1389, 1852, 2778, 3704,
        5556, 7408, 11112, 14816, 22224,
        29632, 44448, 59264, 66672,
    )

    // ------------------------------------------------------------------
    // Nautical-mile stepping
    // ------------------------------------------------------------------

    @Test
    fun `NM stepping skips metric-only values`() {
        // Starting at 100m (metric-only), step up should jump to 926m (0.5 NM)
        val idx100 = typicalRanges.indexOf(100)
        val result = RangeStepper.findNextRoundRange(typicalRanges, idx100, 1, DistanceUnit.NM)
        assertEquals(typicalRanges.indexOf(926), result)
    }

    @Test
    fun `NM stepping up from 1852m goes to 2778m`() {
        val idx1852 = typicalRanges.indexOf(1852)
        val result = RangeStepper.findNextRoundRange(typicalRanges, idx1852, 1, DistanceUnit.NM)
        assertEquals(typicalRanges.indexOf(2778), result)
    }

    @Test
    fun `NM stepping down from 2778m goes to 1852m`() {
        val idx2778 = typicalRanges.indexOf(2778)
        val result = RangeStepper.findNextRoundRange(typicalRanges, idx2778, -1, DistanceUnit.NM)
        assertEquals(typicalRanges.indexOf(1852), result)
    }

    @Test
    fun `NM stepping at lower bound stays at first NM range`() {
        val idx926 = typicalRanges.indexOf(926)
        val result = RangeStepper.findNextRoundRange(typicalRanges, idx926, -1, DistanceUnit.NM)
        // No NM range below 926; should stay
        assertEquals(idx926, result)
    }

    @Test
    fun `NM stepping at upper bound stays`() {
        val last = typicalRanges.lastIndex
        val result = RangeStepper.findNextRoundRange(typicalRanges, last, 1, DistanceUnit.NM)
        assertEquals(last, result)
    }

    // ------------------------------------------------------------------
    // Metric (KM) stepping
    // ------------------------------------------------------------------

    @Test
    fun `KM stepping includes metric values and skips non-round`() {
        val idx50 = typicalRanges.indexOf(50)
        val result = RangeStepper.findNextRoundRange(typicalRanges, idx50, 1, DistanceUnit.KM)
        assertEquals(typicalRanges.indexOf(75), result)
    }

    @Test
    fun `KM stepping down from 250m goes to 100m`() {
        val idx250 = typicalRanges.indexOf(250)
        val result = RangeStepper.findNextRoundRange(typicalRanges, idx250, -1, DistanceUnit.KM)
        assertEquals(typicalRanges.indexOf(100), result)
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `empty range list returns current index`() {
        val result = RangeStepper.findNextRoundRange(emptyList(), 0, 1, DistanceUnit.NM)
        assertEquals(0, result)
    }

    @Test
    fun `single range list stays at 0`() {
        val result = RangeStepper.findNextRoundRange(listOf(1852), 0, 1, DistanceUnit.NM)
        assertEquals(0, result)
    }
}
