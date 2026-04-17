package com.marineyachtradar.mayara.ui.radar

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.marineyachtradar.mayara.data.model.ColorPalette
import com.marineyachtradar.mayara.data.model.SpokeData

/**
 * Compose wrapper for the OpenGL ES radar canvas.
 *
 * Renders the radar sweep using [RadarGLRenderer] inside a [GLSurfaceView].
 *
 * ## Gesture handling
 * - **One-finger drag** → pans the radar centre (look-ahead mode)
 * - **Double-tap**      → resets pan to centre
 * - **Pinch gesture**   → **discarded** — see spec §3.2 and [DisabledPinchZoom]
 *
 * @param latestSpoke      Most recent [SpokeData] decoded from the spoke WebSocket.
 *                         When non-null, the renderer updates the corresponding texture column.
 * @param spokesPerRevolution  Number of spokes per 360° from the capabilities handshake.
 * @param palette          Active colour palette applied by the fragment shader LUT.
 * @param modifier         Standard Compose [Modifier].
 */
@Composable
fun RadarGLView(
    latestSpoke: SpokeData?,
    spokesPerRevolution: Int,
    palette: ColorPalette,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { RadarGLRenderer() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            createGLSurfaceView(context, renderer)
        },
        update = { _ ->
            // Feed new spoke data into the renderer whenever Compose recomposes.
            latestSpoke?.let { renderer.updateSpoke(it, spokesPerRevolution) }
            renderer.setPalette(palette)
        }
    )
}

// ---------------------------------------------------------------------------
// GLSurfaceView factory (extracted for testability)
// ---------------------------------------------------------------------------

/**
 * Creates and configures a [GLSurfaceView] with GLES 2.0, attaches [renderer],
 * and wires gesture detectors to the view's touch handler.
 */
internal fun createGLSurfaceView(context: Context, renderer: RadarGLRenderer): GLSurfaceView {
    val glView = GLSurfaceView(context)
    glView.setEGLContextClientVersion(2)
    glView.setRenderer(renderer)
    glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            // Convert pixel delta to normalised display units [-0.5..0.5].
            val normX = distanceX / glView.width * -1f
            val normY = distanceY / glView.height
            renderer.setCenterOffset(normX, normY)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            renderer.resetCenter()
            return true
        }
    })

    // Pinch zoom is DISCARDED per spec §3.2. The DisabledPinchZoom listener
    // consumes the scale events but never applies the scale factor to the renderer.
    val scaleDetector = ScaleGestureDetector(context, DisabledPinchZoom())

    glView.setOnTouchListener { _, event ->
        // Feed both detectors; both consume the event.
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        true
    }

    return glView
}

// ---------------------------------------------------------------------------
// Pinch zoom policy (named class — directly unit-testable)
// ---------------------------------------------------------------------------

/**
 * Scale gesture listener that **discards all pinch events**.
 *
 * This is a safety-critical requirement (spec §3.2): zooming must only be
 * possible via the hardware-stepped [+/-] buttons to ensure the on-screen scale
 * exactly reflects the radar's active range.
 *
 * The listener is registered so that pinch events are properly consumed
 * (returning `true` from [onScale]) and do not leak through to other handlers.
 * The scale factor is intentionally never read or applied.
 */
class DisabledPinchZoom : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    /**
     * Returns `true` to consume the scale event. The [detector]'s scale factor
     * is never applied — pinch-to-zoom is unconditionally disabled.
     */
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        // Intentionally do nothing with detector.scaleFactor.
        return true  // consumed — must not propagate to other handlers
    }

    /**
     * Returns `false` to confirm we do NOT want zoom to be applied.
     * Exposed for unit testing via [shouldApplyScale].
     */
    fun shouldApplyScale(@Suppress("UNUSED_PARAMETER") factor: Float): Boolean = false
}
