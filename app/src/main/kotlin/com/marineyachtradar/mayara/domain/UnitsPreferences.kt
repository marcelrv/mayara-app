package com.marineyachtradar.mayara.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.marineyachtradar.mayara.data.model.BearingMode
import com.marineyachtradar.mayara.data.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Application-wide DataStore singleton.
 *
 * All settings (connection, units, etc.) share a single DataStore file named "mayara_prefs"
 * to avoid creating multiple backing files for different settings groups.
 *
 * Usage: `context.mayaraDataStore`
 */
val Context.mayaraDataStore: DataStore<Preferences> by preferencesDataStore(name = "mayara_prefs")

/**
 * DataStore-backed repository for user units & formats preferences (spec §3.5).
 *
 * Injected with a [DataStore] so it can be tested with [FakePreferencesDataStore] without
 * requiring an Android Context.
 */
class UnitsPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        internal val KEY_DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        internal val KEY_BEARING_MODE = stringPreferencesKey("bearing_mode")
    }

    /**
     * Emits the current distance unit preference. Defaults to [DistanceUnit.NM] if not set.
     */
    val distanceUnit: Flow<DistanceUnit> = dataStore.data.map { prefs ->
        when (prefs[KEY_DISTANCE_UNIT]) {
            DistanceUnit.KM.name -> DistanceUnit.KM
            DistanceUnit.SM.name -> DistanceUnit.SM
            else -> DistanceUnit.NM
        }
    }

    /**
     * Emits the current bearing mode preference. Defaults to [BearingMode.TRUE] if not set.
     */
    val bearingMode: Flow<BearingMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_BEARING_MODE]) {
            BearingMode.MAGNETIC.name -> BearingMode.MAGNETIC
            else -> BearingMode.TRUE
        }
    }

    /** Persist the chosen distance unit. */
    suspend fun saveDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { prefs ->
            prefs[KEY_DISTANCE_UNIT] = unit.name
        }
    }

    /** Persist the chosen bearing mode. */
    suspend fun saveBearingMode(mode: BearingMode) {
        dataStore.edit { prefs ->
            prefs[KEY_BEARING_MODE] = mode.name
        }
    }

    /** Reset both preferences to factory defaults ([DistanceUnit.NM], [BearingMode.TRUE]). */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DISTANCE_UNIT)
            prefs.remove(KEY_BEARING_MODE)
        }
    }
}
