package com.rezoxnemesis.kebtee.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperCatalogTest {
    @Test
    fun catalogContainsFortyOneUniqueScenesIncludingPreparedWallpaper() {
        assertEquals(41, WallpaperCatalog.scenes.size)
        assertEquals(41, WallpaperCatalog.scenes.map { it.id }.toSet().size)
        assertNotNull(WallpaperCatalog.byId("keptee-silhouette"))
    }

    @Test
    fun everySceneHasUsableMetadata() {
        assertTrue(WallpaperCatalog.scenes.all { it.id.isNotBlank() })
        assertTrue(WallpaperCatalog.scenes.all { it.title.isNotBlank() })
        assertTrue(WallpaperCatalog.scenes.all { it.category != null })
        assertTrue(WallpaperCatalog.scenes.all { it.batteryProfile != null })
    }

    @Test
    fun catalogCoversTheCoreWallpaperFamilies() {
        val categories = WallpaperCatalog.scenes.map { it.category }.toSet()
        assertTrue(categories.contains(WallpaperCategory.AURORA))
        assertTrue(categories.contains(WallpaperCategory.NEON))
        assertTrue(categories.contains(WallpaperCategory.FLUID))
        assertTrue(categories.contains(WallpaperCategory.GEOMETRY))
        assertTrue(categories.contains(WallpaperCategory.SPACE))
        assertTrue(categories.contains(WallpaperCategory.NATURE))
        assertTrue(categories.contains(WallpaperCategory.AMOLED))
        assertTrue(categories.contains(WallpaperCategory.PARTICLE))
        assertTrue(categories.contains(WallpaperCategory.SILHOUETTE))
    }

    @Test
    fun lookupReturnsTheSameSceneById() {
        val first = WallpaperCatalog.scenes.first()
        assertNotNull(WallpaperCatalog.byId(first.id))
        assertEquals(first, WallpaperCatalog.byId(first.id))
    }
}
