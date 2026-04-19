package com.marineyachtradar.mayara.data.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SignalKStreamClientTest {

    private val client = SignalKStreamClient()

    // ------------------------------------------------------------------
    // parseUpdates — nested BareControlValue format (server default)
    // ------------------------------------------------------------------

    @Test
    fun `parseUpdates handles nested BareControlValue objects`() {
        val json = """
        {
          "updates": [{
            "values": [{
              "path": "radars.halo24.controls.gain",
              "value": { "value": 75, "auto": false }
            }]
          }]
        }
        """.trimIndent()

        val results = client.parseUpdates(json)
        assertEquals(1, results.size)
        assertEquals("halo24", results[0].radarId)
        assertEquals("gain", results[0].controlId)
        assertEquals(75f, results[0].value)
        assertFalse(results[0].auto)
    }

    @Test
    fun `parseUpdates handles nested value with auto true`() {
        val json = """
        {
          "updates": [{
            "values": [{
              "path": "radars.radar1.controls.sea",
              "value": { "value": 42, "auto": true }
            }]
          }]
        }
        """.trimIndent()

        val results = client.parseUpdates(json)
        assertEquals(1, results.size)
        assertEquals(42f, results[0].value)
        assertTrue(results[0].auto)
    }

    // ------------------------------------------------------------------
    // parseUpdates — flat format (legacy/fallback)
    // ------------------------------------------------------------------

    @Test
    fun `parseUpdates handles flat value format`() {
        val json = """
        {
          "updates": [{
            "values": [{
              "path": "radars.br24.controls.power",
              "value": 2
            }]
          }]
        }
        """.trimIndent()

        val results = client.parseUpdates(json)
        assertEquals(1, results.size)
        assertEquals("br24", results[0].radarId)
        assertEquals("power", results[0].controlId)
        assertEquals(2f, results[0].value)
        assertFalse(results[0].auto)
    }

    // ------------------------------------------------------------------
    // parseUpdates — multiple values
    // ------------------------------------------------------------------

    @Test
    fun `parseUpdates extracts multiple values from single update`() {
        val json = """
        {
          "updates": [{
            "values": [
              { "path": "radars.r1.controls.gain", "value": { "value": 50, "auto": false } },
              { "path": "radars.r1.controls.sea",  "value": { "value": 30, "auto": true  } }
            ]
          }]
        }
        """.trimIndent()

        val results = client.parseUpdates(json)
        assertEquals(2, results.size)
        assertEquals("gain", results[0].controlId)
        assertEquals("sea", results[1].controlId)
    }

    // ------------------------------------------------------------------
    // parseUpdates — path filtering
    // ------------------------------------------------------------------

    @Test
    fun `parseUpdates skips non-radar paths`() {
        val json = """
        {
          "updates": [{
            "values": [{
              "path": "navigation.position",
              "value": { "longitude": 5.0, "latitude": 52.0 }
            }]
          }]
        }
        """.trimIndent()

        val results = client.parseUpdates(json)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `parseUpdates returns empty for missing updates array`() {
        val results = client.parseUpdates("""{ "context": "self" }""")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `parseUpdates returns empty for empty updates array`() {
        val results = client.parseUpdates("""{ "updates": [] }""")
        assertTrue(results.isEmpty())
    }
}
