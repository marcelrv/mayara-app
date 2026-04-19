package com.marineyachtradar.mayara.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-scoped holder for radar info that needs to be accessible
 * from both the main RadarViewModel and the SettingsViewModel.
 *
 * Updated by [RadarRepository] when a connection is established.
 * Read by [SettingsViewModel] to populate the App Info screen.
 */
object RadarInfoHolder {

    private val _radarInfo = MutableStateFlow<RadarInfoSnapshot?>(null)
    val radarInfo: StateFlow<RadarInfoSnapshot?> = _radarInfo.asStateFlow()

    fun update(snapshot: RadarInfoSnapshot) {
        _radarInfo.value = snapshot
    }

    fun clear() {
        _radarInfo.value = null
    }
}

/**
 * Snapshot of radar information for display on the App Info screen.
 */
data class RadarInfoSnapshot(
    val radarName: String,
    val brand: String,
    val modelName: String? = null,
    val serialNumber: String? = null,
    val firmwareVersion: String? = null,
    val spokesPerRevolution: Int,
    val maxSpokeLength: Int,
    val operatingTimeSeconds: Float? = null,
    val transmitTimeSeconds: Float? = null,
)
