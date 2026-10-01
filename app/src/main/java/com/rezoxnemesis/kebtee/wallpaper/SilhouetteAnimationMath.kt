package com.rezoxnemesis.kebtee.wallpaper

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Periodic motion curves used by the always-looping monochrome live wallpaper. */
internal object SilhouetteAnimationMath {
    private const val BREATHING_PERIOD_SECONDS = 6.0
    private const val SWEEP_PERIOD_SECONDS = 9.0

    fun breathingPulse(seconds: Float): Float {
        val phase = seconds.toDouble() * (2.0 * PI / BREATHING_PERIOD_SECONDS)
        return (0.82 + 0.18 * (0.5 + 0.5 * sin(phase))).toFloat()
    }

    fun sweepProgress(seconds: Float): Float {
        val phase = seconds.toDouble() * (2.0 * PI / SWEEP_PERIOD_SECONDS)
        return (0.5 - 0.5 * cos(phase)).toFloat()
    }
}
