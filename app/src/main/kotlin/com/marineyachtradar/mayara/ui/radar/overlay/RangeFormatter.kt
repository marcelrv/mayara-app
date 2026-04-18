package com.marineyachtradar.mayara.ui.radar.overlay

import com.marineyachtradar.mayara.data.model.DistanceUnit
import kotlin.math.abs

/**
 * Formats radar range values (in metres) for display, respecting the user's distance unit
 * preference and using marine-convention fraction notation for sub-1 NM ranges.
 *
 * Fraction notation (e.g. "1/8 NM", "1/4 NM") is the standard on Navico, Furuno,
 * Garmin and Raymarine radar displays for short ranges.
 */
object RangeFormatter {

    /** Common marine radar range fractions (label → metres). */
    private val NM_FRACTIONS = listOf(
        "1/8" to 231.5,   // 1852 / 8
        "1/4" to 463.0,   // 1852 / 4
        "3/8" to 694.5,   // 1852 * 3/8
        "1/2" to 926.0,   // 1852 / 2
        "3/4" to 1389.0,  // 1852 * 3/4
    )

    private const val METRES_PER_NM = 1852.0
    private const val METRES_PER_SM = 1609.344

    /**
     * Format a range in metres to a human-readable string using the given [unit].
     *
     * - **NM**: Uses fraction notation (1/8, 1/4, 3/8, 1/2, 3/4) for sub-1 NM ranges
     *   when within ±5 % tolerance. Falls back to decimal notation otherwise.
     * - **KM**: Metres → kilometres with appropriate decimal places.
     * - **SM**: Metres → statute miles with appropriate decimal places.
     */
    fun format(metres: Int, unit: DistanceUnit): String = when (unit) {
        DistanceUnit.NM -> formatNm(metres)
        DistanceUnit.KM -> formatKm(metres)
        DistanceUnit.SM -> formatSm(metres)
    }

    private fun formatNm(metres: Int): String {
        val nm = metres / METRES_PER_NM

        // Try matching a known fraction (±5 % tolerance) for sub-1 NM ranges.
        if (nm < 1.0) {
            for ((label, fracMetres) in NM_FRACTIONS) {
                if (abs(metres - fracMetres) <= fracMetres * 0.05) {
                    return "$label NM"
                }
            }
            // Sub-1 NM but not a known fraction — use 2 decimal places.
            return "%.2f NM".format(nm)
        }

        // 1 NM – 10 NM: 1 decimal place.
        if (nm < 10.0) return "%.1f NM".format(nm)

        // ≥ 10 NM: no decimals.
        return "%.0f NM".format(nm)
    }

    private fun formatKm(metres: Int): String {
        val km = metres / 1000.0
        return if (km < 10.0) "%.1f km".format(km) else "%.0f km".format(km)
    }

    private fun formatSm(metres: Int): String {
        val sm = metres / METRES_PER_SM
        return when {
            sm < 1.0 -> "%.2f SM".format(sm)
            sm < 10.0 -> "%.1f SM".format(sm)
            else -> "%.0f SM".format(sm)
        }
    }
}
