package com.rezoxnemesis.kebtee.wallpaper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPreferencesTest {
    @Test
    fun settingsRoundTripKeepsUserValuesPerScene() {
        val prefs = WallpaperPreferences(ApplicationProvider.getApplicationContext<Context>())
        val scene = WallpaperCatalog.scenes.first()
        val expected = WallpaperSettings(speed = 1.35f, intensity = 0.72f, colorVariant = 2, reducedMotion = true, batteryMode = true)

        prefs.saveSettings(scene.id, expected)

        assertEquals(expected, prefs.settingsFor(scene.id))
    }

    @Test
    fun favoritesAndRecentScenesPersist() {
        val prefs = WallpaperPreferences(ApplicationProvider.getApplicationContext<Context>())
        val a = WallpaperCatalog.scenes[0].id
        val b = WallpaperCatalog.scenes[1].id

        prefs.setFavorite(a, true)
        prefs.markApplied(a)
        prefs.markApplied(b)

        assertTrue(prefs.isFavorite(a))
        assertEquals(listOf(b, a), prefs.recentlyUsed().take(2))
    }
}
