package com.marineyachtradar.mayara.ui.radar

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for pinch-to-zoom behaviour on [RadarGLRenderer].
 *
 * Zoom is applied via [RadarGLRenderer.applyZoom] and clamped to
 * [RadarGLRenderer.MIN_ZOOM]..[RadarGLRenderer.MAX_ZOOM].
 * Zoom resets to 1× on range change via [RadarGLRenderer.resetZoom].
 */
class PinchZoomDisabledTest {

    private val renderer = RadarGLRenderer()

    @Test
    fun `applyZoom scales zoom level incrementally`() {
        renderer.applyZoom(2f)
        assertEquals(2f, renderer.zoomLevel, 0.001f)
        renderer.applyZoom(1.5f)
        assertEquals(3f, renderer.zoomLevel, 0.001f)
    }

    @Test
    fun `applyZoom clamps to maximum zoom`() {
        renderer.applyZoom(20f)
        assertEquals(RadarGLRenderer.MAX_ZOOM, renderer.zoomLevel, 0.001f)
    }

    @Test
    fun `applyZoom clamps to minimum zoom`() {
        renderer.applyZoom(0.1f)
        assertEquals(RadarGLRenderer.MIN_ZOOM, renderer.zoomLevel, 0.001f)
    }
}
