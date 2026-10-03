package com.rezoxnemesis.kebtee.wallpaper

import android.content.SharedPreferences

object WallpaperPreferences {
    const val FILE_NAME = "kebtee_preferences"
    private val keys = setOf("wallpaper_speed", "wallpaper_glow", "wallpaper_particles", "wallpaper_touch",
        "wallpaper_battery_saver", "wallpaper_fps", "wallpaper_time", "wallpaper_charging", "wallpaper_dim_lock", "reduce_motion",
        "wallpaper_tilt")

    fun isWallpaperKey(key: String?) = key == null || key in keys

    fun read(preferences: SharedPreferences): WallpaperSettings {
        val values = preferences.all
        fun number(key: String, default: Number) = values[key] as? Number ?: default
        fun flag(key: String, default: Boolean) = values[key] as? Boolean ?: default
        return WallpaperSettings(
            speed = number("wallpaper_speed", 1f).toFloat(),
            glow = number("wallpaper_glow", 0.45f).toFloat(),
            particleDensity = number("wallpaper_particles", 18).toInt(),
            touchEffects = flag("wallpaper_touch", true),
            batterySaver = flag("wallpaper_battery_saver", false),
            fps = number("wallpaper_fps", 30).toInt(),
            timeEffects = flag("wallpaper_time", true),
            chargingEffects = flag("wallpaper_charging", true),
            dimOnLock = flag("wallpaper_dim_lock", true),
            reduceMotion = flag("reduce_motion", false),
            tiltMotion = flag("wallpaper_tilt", true)
        ).normalized()
    }

    fun write(preferences: SharedPreferences, settings: WallpaperSettings) {
        val value = settings.normalized()
        preferences.edit().putFloat("wallpaper_speed", value.speed).putFloat("wallpaper_glow", value.glow)
            .putInt("wallpaper_particles", value.particleDensity).putBoolean("wallpaper_touch", value.touchEffects)
            .putBoolean("wallpaper_battery_saver", value.batterySaver).putInt("wallpaper_fps", value.fps)
            .putBoolean("wallpaper_time", value.timeEffects).putBoolean("wallpaper_charging", value.chargingEffects)
            .putBoolean("wallpaper_dim_lock", value.dimOnLock).putBoolean("reduce_motion", value.reduceMotion)
            .putBoolean("wallpaper_tilt", value.tiltMotion).apply()
    }

    fun reset(preferences: SharedPreferences) = write(preferences, WallpaperSettings())
}
