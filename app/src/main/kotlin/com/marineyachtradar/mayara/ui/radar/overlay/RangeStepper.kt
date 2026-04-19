package com.marineyachtradar.mayara.ui.radar.overlay

import com.marineyachtradar.mayara.data.model.DistanceUnit

/**
 * Finds the next "round" range index when stepping through the capabilities range list,
 * filtering to ranges that are natural values in the selected distance unit.
 *
 * Marine radars report a combined list of both nautical and metric range steps.
 * When the user has NM selected, stepping should skip metric-only values (100m, 250m, etc.)
 * and land on standard nautical ranges (1/8 NM, 1/4 NM, 1/2 NM, 1 NM, ...).
 */
object RangeStepper {

    /** Standard nautical range steps in metres (1/8 NM through 120 NM). */
    private val NAUTICAL_METRES = listOf(
        57, 115, 231, 463, 926, 1389, 1852, 2778, 3704, 5556,
        7408, 11112, 14816, 22224, 29632, 44448, 59264, 66672, 74080,
        88896, 118528, 133344, 177792, 222240,
    )

    /** Standard metric range steps in metres (50 m through 120 km). */
    private val METRIC_METRES = listOf(
        50, 75, 100, 250, 500, 750, 1000, 1500, 2000, 3000,
        4000, 6000, 8000, 12000, 16000, 24000, 36000, 48000, 64000,
        72000, 96000, 120000,
    )

    /** Standard statute mile range steps in metres. */
    private val STATUTE_METRES = listOf(
        402, 805, 1609, 2414, 3219, 4828, 8047, 16093, 24140,
        32187, 48280, 80467, 120701, 160934,
    )

    private const val TOLERANCE = 0.06 // 6% tolerance for fuzzy matching

    /**
     * Find the next range index in the given [direction] that matches a "round" value
     * for the given [unit].
     *
     * @param ranges  The full sorted list of range values in metres from capabilities.
     * @param currentIndex  The current range index.
     * @param direction  -1 for zoom in (smaller range), +1 for zoom out (larger range).
     * @param unit  The user's selected distance unit.
     * @return The new range index, or [currentIndex] if no step is possible.
     */
    fun findNextRoundRange(
        ranges: List<Int>,
        currentIndex: Int,
        direction: Int,
        unit: DistanceUnit,
    ): Int {
        if (ranges.isEmpty()) return currentIndex

        val preferred = when (unit) {
            DistanceUnit.NM -> NAUTICAL_METRES
            DistanceUnit.KM -> METRIC_METRES
            DistanceUnit.SM -> STATUTE_METRES
        }

        // Build list of indices that match a preferred range value
        val roundIndices = ranges.indices.filter { idx ->
            isPreferred(ranges[idx], preferred)
        }

        if (roundIndices.isEmpty()) {
            // No preferred ranges in the list — fall back to simple index±1
            val next = (currentIndex + direction).coerceIn(0, ranges.lastIndex)
            return next
        }

        // Find the next round index in the given direction
        return if (direction < 0) {
            // Zoom in: find the largest round index that is smaller than currentIndex
            roundIndices.lastOrNull { it < currentIndex } ?: currentIndex
        } else {
            // Zoom out: find the smallest round index that is larger than currentIndex
            roundIndices.firstOrNull { it > currentIndex } ?: currentIndex
        }
    }

    private fun isPreferred(metres: Int, preferred: List<Int>): Boolean {
        return preferred.any { pref ->
            val diff = kotlin.math.abs(metres - pref)
            diff <= maxOf(1, (pref * TOLERANCE).toInt())
        }
    }
}
