package com.rezoxnemesis.kebtee.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPreferencesTest {
    @Test
    fun settingsNormalizationClampsRuntimeValues() {
        val input = WallpaperSettings(speed = 9f, intensity = -2f, colorVariant = 9, reducedMotion = true, batteryMode = true)
        val normalized = sanitizeWallpaperSettings(input)

        assertEquals(2f, normalized.speed, 0.0001f)
        assertEquals(0f, normalized.intensity, 0.0001f)
        assertEquals(3, normalized.colorVariant)
        assertTrue(normalized.reducedMotion)
        assertTrue(normalized.batteryMode)
    }

    @Test
    fun catalogScenesProvideStablePreferenceKeys() {
        val ids = WallpaperCatalog.scenes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
    }
}
