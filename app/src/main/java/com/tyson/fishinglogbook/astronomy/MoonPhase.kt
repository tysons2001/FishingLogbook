package com.tyson.fishinglogbook.astronomy

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

data class MoonPhaseInfo(
    val phaseName: String,
    val illuminationPct: Int
)

object MoonPhase {

    // Synodic month (new moon to new moon)
    private const val SYNODIC_MONTH = 29.530588853

    // Reference new moon: 2000-01-06 18:14 UTC (Julian day ~ 2451550.1)
    private const val REF_NEW_MOON_JD = 2451550.1

    fun fromMillis(utcMillis: Long): MoonPhaseInfo {
        val jd = unixMillisToJulianDay(utcMillis)

        // Phase as fraction [0,1)
        var phase = (jd - REF_NEW_MOON_JD) / SYNODIC_MONTH
        phase -= floor(phase)
        if (phase < 0) phase += 1.0

        // Illumination percentage: 0..100
        val illum = 0.5 * (1.0 - cos(2.0 * PI * phase))
        val illumPct = (illum * 100.0).toInt().coerceIn(0, 100)

        val name = phaseName(phase)
        return MoonPhaseInfo(name, illumPct)
    }

    private fun phaseName(p: Double): String {
        return when {
            p < 0.03 || p > 0.97 -> "New Moon"
            p < 0.22 -> "Waxing Crescent"
            p < 0.28 -> "First Quarter"
            p < 0.47 -> "Waxing Gibbous"
            p < 0.53 -> "Full Moon"
            p < 0.72 -> "Waning Gibbous"
            p < 0.78 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    private fun unixMillisToJulianDay(ms: Long): Double {
        // Julian Day for Unix epoch (1970-01-01 00:00:00 UTC) is 2440587.5
        return ms / 86400000.0 + 2440587.5
    }
}
