package com.marineyachtradar.mayara.ui.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marineyachtradar.mayara.data.api.RadarApiClient
import com.marineyachtradar.mayara.data.api.SignalKStreamClient
import com.marineyachtradar.mayara.data.api.SpokeWebSocketClient
import com.marineyachtradar.mayara.data.model.PowerState
import com.marineyachtradar.mayara.data.model.RadarUiState
import com.marineyachtradar.mayara.domain.RadarRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for [RadarScreen].
 *
 * Creates its own [RadarRepository] using [viewModelScope] so all coroutines are
 * cancelled automatically when the ViewModel is cleared.
 *
 * **Connection modes:**
 * - **Embedded (default)**: The JNI Rust server runs on 127.0.0.1:6502 inside the Android app itself.
 *   This requires Phase 1 (JNI) to be complete and the .so library to load successfully.
 * - **Network (Phase 5)**: User selects a remote mayara-server URL from Settings.
 *
 * **NOTE (Phase 3):** At this stage, the embedded server may not be running yet (Phase 1 JNI
 * integration still in progress). The app will show "Connecting to radar…" and fail with a
 * connection error if the server is unreachable. This is expected until Phase 1 is complete.
 */
class RadarViewModel : ViewModel() {

    private val repository = RadarRepository(
        apiClient = RadarApiClient(EMBEDDED_BASE_URL),
        spokeClient = SpokeWebSocketClient(),
        streamClient = SignalKStreamClient(),
        scope = viewModelScope,
    )

    /** Observed by [RadarScreen]. Never null; starts as [RadarUiState.Loading]. */
    val uiState: StateFlow<RadarUiState> = repository.uiState

    /** Exposed so the GL renderer can subscribe to spoke data. */
    val spokeFlow = repository.spokeFlow

    init {
        // Attempt to connect to the embedded JNI server.
        // If the JNI layer hasn't started the server yet, this will fail with a connection error,
        // which [RadarRepository] will surface in [uiState] as [RadarUiState.Error].
        // This is expected until Phase 1 (JNI integration) is fully verified.
        repository.connect(EMBEDDED_BASE_URL)
    }

    fun onPowerAction(action: PowerState) {
        // TODO Phase 4: map power action to control write
    }

    fun onRangeUp() {
        val state = uiState.value as? RadarUiState.Connected ?: return
        val nextIndex = (state.currentRangeIndex + 1)
            .coerceAtMost(state.capabilities.ranges.lastIndex)
        if (nextIndex != state.currentRangeIndex) {
            repository.setSliderControl("range", state.capabilities.ranges[nextIndex].toFloat())
        }
    }

    fun onRangeDown() {
        val state = uiState.value as? RadarUiState.Connected ?: return
        val nextIndex = (state.currentRangeIndex - 1).coerceAtLeast(0)
        if (nextIndex != state.currentRangeIndex) {
            repository.setSliderControl("range", state.capabilities.ranges[nextIndex].toFloat())
        }
    }

    override fun onCleared() {
        repository.disconnect()
    }

    companion object {
        /**
         * Embedded JNI server address.
         *
         * The Rust server (mayara-jni) runs inside the Android process and listens on
         * 127.0.0.1:6502 (process-local loopback).
         *
         * **On physical device:** 127.0.0.1 = the device itself (not the dev machine).
         * **On emulator:** 10.0.2.2 can be used instead to reach the host machine (not applicable here).
         *
         * This URL is hardcoded for embedded mode. Phase 5 (Settings) will allow users to
         * switch to a remote server URL.
         */
        const val EMBEDDED_BASE_URL = "http://127.0.0.1:6502"
    }
}
