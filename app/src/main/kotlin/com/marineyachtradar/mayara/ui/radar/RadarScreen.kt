package com.marineyachtradar.mayara.ui.radar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marineyachtradar.mayara.data.model.RadarUiState
import com.marineyachtradar.mayara.ui.radar.overlay.HudOverlay
import com.marineyachtradar.mayara.ui.radar.overlay.PowerToggle
import com.marineyachtradar.mayara.ui.radar.overlay.RangeControls

/**
 * Root composable for the radar display.
 *
 * Layout (all layers stacked in a [Box]):
 * - Layer 0 (background): [RadarGLView] — OpenGL ES polar radar sweep
 * - Layer 1 (top-left):   [HudOverlay] — Heading / SOG / COG
 * - Layer 2 (top-right):  [PowerToggle] — OFF/WARMUP/STANDBY/TRANSMIT pill
 * - Layer 3 (bottom-right): [RangeControls] — +/- range FABs
 * - Layer 4 (bottom):     Bottom sheet handle + [RadarControlSheet] (swipe-up)
 *
 * Gesture rules:
 *  - Single-finger drag → pan radar center
 *  - Double-tap → reset pan to center
 *  - Pinch gesture → **discarded** (zoom via range buttons only; see spec §3.2)
 *
 * TODO Phase 3: wire [RadarGLView] and gesture handling.
 * TODO Phase 4: add [RadarControlSheet] bottom sheet.
 */
@Composable
fun RadarScreen(
    viewModel: RadarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 0: OpenGL radar canvas (TODO Phase 3)
        // RadarGLView(modifier = Modifier.fillMaxSize())

        when (uiState) {
            is RadarUiState.Connected -> {
                val connected = uiState as RadarUiState.Connected
                // Layer 1: HUD (top-left)
                HudOverlay(
                    navigationData = connected.navigationData,
                    modifier = Modifier.align(Alignment.TopStart),
                )

                // Layer 2: Power toggle (top-right)
                PowerToggle(
                    powerState = connected.powerState,
                    onPowerAction = { viewModel.onPowerAction(it) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )

                // Layer 3: Range controls (bottom-right)
                RangeControls(
                    ranges = connected.capabilities.ranges,
                    currentIndex = connected.currentRangeIndex,
                    onRangeUp = { viewModel.onRangeUp() },
                    onRangeDown = { viewModel.onRangeDown() },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }

            is RadarUiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to radar…",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            is RadarUiState.Error -> {
                val error = uiState as RadarUiState.Error
                Text(
                    text = "Error: ${error.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
