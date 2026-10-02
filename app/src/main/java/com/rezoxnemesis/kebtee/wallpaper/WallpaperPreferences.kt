package com.rezoxnemesis.kebtee.wallpaper

import android.content.Context
import android.content.SharedPreferences

data class WallpaperSettings(
    val speed: Float = 1f,
    val intensity: Float = 1f,
    val colorVariant: Int = 0,
    val reducedMotion: Boolean = false,
    val batteryMode: Boolean = false
)

internal interface WallpaperPreferenceStore {
    fun getString(key: String, default: String?): String?
    fun getFloat(key: String, default: Float): Float
    fun getInt(key: String, default: Int): Int
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getStringSet(key: String, default: Set<String>): Set<String>?
    fun putString(key: String, value: String)
    fun putFloat(key: String, value: Float)
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
    fun putStringSet(key: String, value: Set<String>)
}

private class SharedPreferencesStore(
    private val prefs: SharedPreferences
) : WallpaperPreferenceStore {
    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)
    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun getStringSet(key: String, default: Set<String>): Set<String>? = prefs.getStringSet(key, default)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun putFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun putStringSet(key: String, value: Set<String>) { prefs.edit().putStringSet(key, value).apply() }
}

class WallpaperPreferences internal constructor(
    private val store: WallpaperPreferenceStore
) {
    constructor(context: Context) : this(
        SharedPreferencesStore(context.getSharedPreferences("kebtee_wallpaper", Context.MODE_PRIVATE))
    )

    fun selectedSceneId(): String =
        store.getString(KEY_SELECTED, WallpaperCatalog.scenes.first().id) ?: WallpaperCatalog.scenes.first().id

    fun setSelectedScene(sceneId: String) {
        store.putString(KEY_SELECTED, sceneId)
    }

    fun settingsFor(sceneId: String): WallpaperSettings {
        val safe = WallpaperCatalog.byId(sceneId)?.id ?: WallpaperCatalog.scenes.first().id
        return WallpaperSettings(
            speed = store.getFloat("$safe.speed", 1f).coerceIn(0.15f, 2f),
            intensity = store.getFloat("$safe.intensity", 1f).coerceIn(0f, 1.25f),
            colorVariant = store.getInt("$safe.color", 0).coerceIn(0, 3),
            reducedMotion = store.getBoolean("$safe.reduced", false),
            batteryMode = store.getBoolean("$safe.battery", false)
        )
    }

    fun saveSettings(sceneId: String, settings: WallpaperSettings) {
        val safe = WallpaperCatalog.byId(sceneId)?.id ?: WallpaperCatalog.scenes.first().id
        val normalized = sanitizeWallpaperSettings(settings)
        store.putFloat("$safe.speed", normalized.speed)
        store.putFloat("$safe.intensity", normalized.intensity)
        store.putInt("$safe.color", normalized.colorVariant)
        store.putBoolean("$safe.reduced", normalized.reducedMotion)
        store.putBoolean("$safe.battery", normalized.batteryMode)
    }

    fun isFavorite(sceneId: String): Boolean =
        store.getStringSet(KEY_FAVORITES, emptySet())?.contains(sceneId) == true

    fun setFavorite(sceneId: String, favorite: Boolean) {
        val current = store.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorite) current.add(sceneId) else current.remove(sceneId)
        store.putStringSet(KEY_FAVORITES, current)
    }

    fun markApplied(sceneId: String) {
        val current = store.getString(KEY_RECENTS, "")
            ?.split('|')
            ?.filter { it.isNotBlank() }
            ?.toMutableList()
            ?: mutableListOf()
        current.remove(sceneId)
        current.add(0, sceneId)
        store.putString(KEY_RECENTS, current.take(8).joinToString("|"))
    }

    fun recentlyUsed(): List<String> =
        store.getString(KEY_RECENTS, "")?.split('|')?.filter { it.isNotBlank() } ?: emptyList()

    private companion object {
        const val KEY_SELECTED = "selected_scene"
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENTS = "recently_used"
    }
}

internal fun sanitizeWallpaperSettings(settings: WallpaperSettings): WallpaperSettings =
    settings.copy(
        speed = settings.speed.coerceIn(0.15f, 2f),
        intensity = settings.intensity.coerceIn(0f, 1.25f),
        colorVariant = settings.colorVariant.coerceIn(0, 3)
    )
