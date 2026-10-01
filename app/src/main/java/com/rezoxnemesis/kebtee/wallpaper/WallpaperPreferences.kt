package com.rezoxnemesis.kebtee.wallpaper

import android.content.Context

data class WallpaperSettings(
    val speed: Float = 1f,
    val intensity: Float = 1f,
    val colorVariant: Int = 0,
    val reducedMotion: Boolean = false,
    val batteryMode: Boolean = false
)

class WallpaperPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("kebtee_wallpaper", Context.MODE_PRIVATE)

    fun selectedSceneId(): String = prefs.getString(KEY_SELECTED, WallpaperCatalog.scenes.first().id) ?: WallpaperCatalog.scenes.first().id

    fun setSelectedScene(sceneId: String) {
        prefs.edit().putString(KEY_SELECTED, sceneId).apply()
    }

    fun settingsFor(sceneId: String): WallpaperSettings {
        val safe = WallpaperCatalog.byId(sceneId)?.id ?: WallpaperCatalog.scenes.first().id
        return WallpaperSettings(
            speed = prefs.getFloat("$safe.speed", 1f).coerceIn(0.15f, 2f),
            intensity = prefs.getFloat("$safe.intensity", 1f).coerceIn(0f, 1.25f),
            colorVariant = prefs.getInt("$safe.color", 0),
            reducedMotion = prefs.getBoolean("$safe.reduced", false),
            batteryMode = prefs.getBoolean("$safe.battery", false)
        )
    }

    fun saveSettings(sceneId: String, settings: WallpaperSettings) {
        val safe = WallpaperCatalog.byId(sceneId)?.id ?: WallpaperCatalog.scenes.first().id
        prefs.edit()
            .putFloat("$safe.speed", settings.speed.coerceIn(0.15f, 2f))
            .putFloat("$safe.intensity", settings.intensity.coerceIn(0f, 1.25f))
            .putInt("$safe.color", settings.colorVariant)
            .putBoolean("$safe.reduced", settings.reducedMotion)
            .putBoolean("$safe.battery", settings.batteryMode)
            .apply()
    }

    fun isFavorite(sceneId: String): Boolean = prefs.getStringSet(KEY_FAVORITES, emptySet())?.contains(sceneId) == true

    fun setFavorite(sceneId: String, favorite: Boolean) {
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorite) current.add(sceneId) else current.remove(sceneId)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun markApplied(sceneId: String) {
        val current = prefs.getString(KEY_RECENTS, "")?.split('|')?.filter { it.isNotBlank() }?.toMutableList()
            ?: mutableListOf()
        current.remove(sceneId)
        current.add(0, sceneId)
        prefs.edit().putString(KEY_RECENTS, current.take(8).joinToString("|")).apply()
    }

    fun recentlyUsed(): List<String> = prefs.getString(KEY_RECENTS, "")?.split('|')?.filter { it.isNotBlank() } ?: emptyList()

    private companion object {
        const val KEY_SELECTED = "selected_scene"
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENTS = "recently_used"
    }
}
