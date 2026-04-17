package com.marineyachtradar.mayara.ui.settings

import com.marineyachtradar.mayara.data.model.BearingMode
import com.marineyachtradar.mayara.data.model.DistanceUnit
import com.marineyachtradar.mayara.ui.settings.screens.isValidPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure JVM tests for settings-related enums and pure helper functions.
 *
 * No Android dependencies — tests run on the JVM directly.
 */
class SettingsModelsTest {

    // ── DistanceUnit labels ──────────────────────────────────────────────

    @Test
    fun `DistanceUnit NM has label NM`() {
        assertEquals("NM", DistanceUnit.NM.label)
    }

    @Test
    fun `DistanceUnit KM has label km`() {
        assertEquals("km", DistanceUnit.KM.label)
    }

    @Test
    fun `DistanceUnit SM has label SM`() {
        assertEquals("SM", DistanceUnit.SM.label)
    }

    // ── BearingMode labels ───────────────────────────────────────────────

    @Test
    fun `BearingMode TRUE has label True`() {
        assertEquals("True", BearingMode.TRUE.label)
    }

    @Test
    fun `BearingMode MAGNETIC has label Magnetic`() {
        assertEquals("Magnetic", BearingMode.MAGNETIC.label)
    }

    // ── isValidPort boundary values ──────────────────────────────────────

    @Test
    fun `isValidPort returns false for 0`() {
        assertFalse(isValidPort("0"))
    }

    @Test
    fun `isValidPort returns false for 65536`() {
        assertFalse(isValidPort("65536"))
    }

    @Test
    fun `isValidPort returns true for 1 (minimum valid)`() {
        assertTrue(isValidPort("1"))
    }

    @Test
    fun `isValidPort returns true for 65535 (maximum valid)`() {
        assertTrue(isValidPort("65535"))
    }

    @Test
    fun `isValidPort returns true for 6502 (default port)`() {
        assertTrue(isValidPort("6502"))
    }

    @Test
    fun `isValidPort returns false for empty string`() {
        assertFalse(isValidPort(""))
    }

    @Test
    fun `isValidPort returns false for non-numeric string`() {
        assertFalse(isValidPort("abc"))
    }

    @Test
    fun `isValidPort returns false for negative number`() {
        assertFalse(isValidPort("-1"))
    }
}
