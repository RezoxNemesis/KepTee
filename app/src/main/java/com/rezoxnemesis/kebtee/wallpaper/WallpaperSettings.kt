package com.rezoxnemesis.kebtee.wallpaper

/** Values are bounded at the persistence boundary, including values restored from a backup. */
data class WallpaperSettings(
    val speed: Float = 1f,
    val glow: Float = 0.45f,
    val particleDensity: Int = 18,
    val touchEffects: Boolean = true,
    val batterySaver: Boolean = false,
    val fps: Int = 30,
    val timeEffects: Boolean = true,
    val chargingEffects: Boolean = true,
    val dimOnLock: Boolean = true,
    val reduceMotion: Boolean = false
) {
    fun normalized() = copy(
        speed = if (speed.isFinite()) speed.coerceIn(0.25f, 2f) else 1f,
        glow = if (glow.isFinite()) glow.coerceIn(0f, 1f) else 0.45f,
        particleDensity = particleDensity.coerceIn(0, 60),
        fps = boundedFps()
    )

    private fun boundedFps(): Int = when { fps <= 15 -> 15; fps <= 30 -> 30; fps <= 60 -> 60; else -> 120 }

    /** Zero means draw only in response to a state change, with no animation loop. */
    fun frameIntervalMillis(systemPowerSaver: Boolean, locked: Boolean): Long = when {
        reduceMotion -> 0L
        batterySaver || systemPowerSaver || (locked && dimOnLock) -> 100L
        else -> 1000L / boundedFps()
    }

    fun activeParticleCount(systemPowerSaver: Boolean, locked: Boolean): Int = when {
        reduceMotion || batterySaver || systemPowerSaver || (locked && dimOnLock) -> 0
        else -> particleDensity.coerceIn(0, 60)
    }
}
