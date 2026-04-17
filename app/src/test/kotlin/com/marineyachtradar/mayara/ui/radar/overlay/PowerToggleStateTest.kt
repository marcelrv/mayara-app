package com.marineyachtradar.mayara.ui.radar.overlay

import com.marineyachtradar.mayara.data.model.PowerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [nextPowerTarget].
 *
 * This pure function drives the pill-shaped [PowerToggle] state machine.
 * Correctness is safety-relevant: an incorrect transition could leave the
 * radar in transmit when the user intends standby.
 */
class PowerToggleStateTest {

    @Test
    fun `OFF transitions to STANDBY on tap`() {
        assertEquals(PowerState.STANDBY, nextPowerTarget(PowerState.OFF))
    }

    @Test
    fun `STANDBY transitions to TRANSMIT on tap`() {
        assertEquals(PowerState.TRANSMIT, nextPowerTarget(PowerState.STANDBY))
    }

    @Test
    fun `TRANSMIT transitions back to STANDBY on tap`() {
        assertEquals(PowerState.STANDBY, nextPowerTarget(PowerState.TRANSMIT))
    }

    @Test
    fun `WARMUP returns null (button is disabled, no transition allowed)`() {
        assertNull(nextPowerTarget(PowerState.WARMUP))
    }

    @Test
    fun `all PowerState values are handled without throwing`() {
        PowerState.entries.forEach { state ->
            // must not throw — null is the valid "no-op" result
            nextPowerTarget(state)
        }
    }
}
