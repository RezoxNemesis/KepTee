package com.rezoxnemesis.kebtee.wallpaper

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin

internal object WallpaperRenderMath {
    fun loopPhase(timeSeconds: Float, periodSeconds: Float): Float {
        if (periodSeconds <= 0f) return 0f
        val wrapped = timeSeconds % periodSeconds
        return if (wrapped < 0f) (wrapped + periodSeconds) / periodSeconds else wrapped / periodSeconds
    }

    fun periodicWave(timeSeconds: Float, periodSeconds: Float): Float {
        val phase = loopPhase(timeSeconds, periodSeconds)
        return (0.5f + 0.5f * sin((phase * 2.0 * PI).toFloat()))
    }

    fun motionAmount(reducedMotion: Boolean, batteryMode: Boolean): Float = when {
        reducedMotion -> 0f
        batteryMode -> 0.35f
        else -> 1f
    }

    fun seedUnit(seed: Int): Float {
        var value = seed
        value = value xor (value shl 13)
        value = value xor (value ushr 17)
        value = value xor (value shl 5)
        return (abs(value.toLong()) % 10_000L).toFloat() / 10_000f
    }

    fun oscillation(timeSeconds: Float, speed: Float, periodSeconds: Float): Float {
        return periodicWave(timeSeconds * speed, periodSeconds)
    }

    fun rotationRadians(timeSeconds: Float, speed: Float, periodSeconds: Float): Float {
        return loopPhase(timeSeconds * speed, periodSeconds) * (2f * PI.toFloat())
    }

    fun blend(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    fun cos01(timeSeconds: Float, speed: Float, periodSeconds: Float): Float {
        val phase = loopPhase(timeSeconds * speed, periodSeconds)
        return (0.5f + 0.5f * cos((phase * 2f * PI).toFloat()))
    }
}
