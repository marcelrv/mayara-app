package com.marineyachtradar.mayara.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marineyachtradar.mayara.data.model.ConnectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.URI

/**
 * Manages the active connection mode (Embedded vs Network) and persists the user's
 * "Remember my choice" preference across app restarts.
 *
 * Injected as a singleton; consumers observe [connectionMode] as a [Flow].
 *
 * TODO Phase 2: inject DataStore and implement the full flow.
 */
class ConnectionManager(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_MODE = stringPreferencesKey("connection_mode")
        private val KEY_LAST_HOST = stringPreferencesKey("last_host")
        private val KEY_LAST_PORT = stringPreferencesKey("last_port")
        private val KEY_FORCE_DISCONNECT = longPreferencesKey("force_disconnect_timestamp")
        
        // Deprecated, keeping temporarily just in case.
        private val KEY_REMEMBERED_HOST = stringPreferencesKey("remembered_host")
        private val KEY_REMEMBERED_PORT = stringPreferencesKey("remembered_port")

        const val EMBEDDED_VALUE = "embedded"
        const val NETWORK_VALUE = "network"
    }

    /** The currently persisted connection mode, or null if no choice has been remembered. */
    val rememberedMode: Flow<ConnectionMode?> = dataStore.data.map { prefs ->
        when (prefs[KEY_MODE]) {
            EMBEDDED_VALUE -> ConnectionMode.Embedded()
            NETWORK_VALUE -> {
                val host = prefs[KEY_LAST_HOST] ?: prefs[KEY_REMEMBERED_HOST] ?: return@map null
                val port = prefs[KEY_LAST_PORT] ?: prefs[KEY_REMEMBERED_PORT] ?: return@map null
                ConnectionMode.Network("http://$host:$port")
            }
            else -> null
        }
    }.distinctUntilChanged()

    val lastNetworkHost: Flow<String?> = dataStore.data.map { it[KEY_LAST_HOST] ?: it[KEY_REMEMBERED_HOST] }.distinctUntilChanged()
    val lastNetworkPort: Flow<String?> = dataStore.data.map { it[KEY_LAST_PORT] ?: it[KEY_REMEMBERED_PORT] }.distinctUntilChanged()
    val forceDisconnectEvent: Flow<Long> = dataStore.data.map { it[KEY_FORCE_DISCONNECT] ?: 0L }.distinctUntilChanged()


    /** Save the manual network location so it populates the UI next time, regardless of auto-connect. */
    suspend fun saveLastNetworkLocation(host: String, port: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_HOST] = host
            prefs[KEY_LAST_PORT] = port
        }
    }

    /** Persist the chosen mode. Call when the user ticks "Remember my choice". */
    suspend fun rememberMode(mode: ConnectionMode) {
        dataStore.edit { prefs ->
            when (mode) {
                is ConnectionMode.Embedded -> {
                    prefs[KEY_MODE] = EMBEDDED_VALUE
                }
                is ConnectionMode.Network -> {
                    prefs[KEY_MODE] = NETWORK_VALUE
                    val uri = URI(mode.baseUrl)
                    prefs[KEY_LAST_HOST] = uri.host ?: ""
                    prefs[KEY_LAST_PORT] = (uri.port.takeIf { it > 0 } ?: 6502).toString()
                }
                is ConnectionMode.PcapDemo -> {
                    // PCAP demo is not persisted across restarts (file path may be temporary)
                }
            }
        }
    }

    /** Clear the remembered choice (re-triggers the picker dialog on next launch). */
    suspend fun forgetMode() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MODE)
            prefs[KEY_FORCE_DISCONNECT] = System.currentTimeMillis()
        }
    }
}
