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
 * The embedded server is expected to run on 127.0.0.1:6502 (started by the JNI layer).
 * Network mode will be selectable from Settings in a future phase.
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
        /** Embedded JNI server always binds on this address (see mayara-jni/src/lib.rs). */
        const val EMBEDDED_BASE_URL = "http://127.0.0.1:6502"
    }
}
