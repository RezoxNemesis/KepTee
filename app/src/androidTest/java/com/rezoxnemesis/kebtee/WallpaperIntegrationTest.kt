package com.rezoxnemesis.kebtee

import android.app.WallpaperInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rezoxnemesis.kebtee.wallpaper.KebTeeLiveWallpaperService
import com.rezoxnemesis.kebtee.wallpaper.WallpaperAssetStore
import com.rezoxnemesis.kebtee.wallpaper.WallpaperPreferences
import com.rezoxnemesis.kebtee.wallpaper.WallpaperScene
import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Validates Android registration and real preferences; rendering still needs wallpaper-host tests. */
@RunWith(AndroidJUnit4::class)
class WallpaperIntegrationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun systemCanDiscoverWallpaperAndReadItsSettingsMetadata() {
        val resolved = context.packageManager.resolveService(
            Intent(WallpaperService.SERVICE_INTERFACE).setPackage(context.packageName),
            PackageManager.GET_META_DATA
        )
        assertNotNull("Android cannot discover the live wallpaper service", resolved)
        val service = requireNotNull(resolved).serviceInfo
        assertEquals(KebTeeLiveWallpaperService::class.java.name, service.name)
        assertEquals("android.permission.BIND_WALLPAPER", service.permission)
        assertTrue(service.exported)
        val info = WallpaperInfo(context, resolved)
        assertEquals(MainActivity::class.java.name, info.settingsActivity)
    }


    @Test fun bundledSilhouetteArtworkDecodesAtExpectedDimensions() {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.kebtee_silhouette)
        assertNotNull("Bundled silhouette artwork failed to decode", bitmap)
        requireNotNull(bitmap)
        assertEquals(1031, bitmap.width)
        assertEquals(1536, bitmap.height)
        var brightSamples = 0
        for (y in 0 until bitmap.height step 16) {
            for (x in 0 until bitmap.width step 16) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.red(pixel) > 180 &&
                    android.graphics.Color.green(pixel) > 180 &&
                    android.graphics.Color.blue(pixel) > 180) {
                    brightSamples++
                }
            }
        }
        assertTrue("Silhouette artwork decoded without visible bright figure detail", brightSamples > 100)
        bitmap.recycle()
    }
    @Test fun suppliedWallpaperAssetsArePackagedAtUsableQuality() {
        val stillFile = WallpaperAssetStore.materialize(context, WallpaperScene.ASCENSION_STILL)
        assertNotNull("Supplied ascension still failed checksum reconstruction", stillFile)
        val still = BitmapFactory.decodeFile(requireNotNull(stillFile).absolutePath)
        assertNotNull("Supplied ascension still failed to decode", still)
        requireNotNull(still)
        assertEquals("Supplied ascension still width changed", 796, still.width)
        assertEquals("Supplied ascension still height changed", 1536, still.height)
        still.recycle()

        assertVideoScene(WallpaperScene.ASCENSION_FLOW, expectedWidth = 1080, expectedHeight = 1920, minDurationMs = 7_500L)
        assertVideoScene(WallpaperScene.AURA_PULSE, expectedWidth = 1440, expectedHeight = 2560, minDurationMs = 13_500L)
    }

    @Test fun restoredWrongPreferenceTypesFallBackWithoutCrashing() {
        val preferences = context.getSharedPreferences("wallpaper_integration_test", Context.MODE_PRIVATE)
        try {
            preferences.edit().clear()
                .putString("wallpaper_speed", "broken")
                .putString("wallpaper_fps", "broken")
                .putInt("wallpaper_touch", 1)
                .putFloat("wallpaper_glow", Float.NaN)
                .putInt("wallpaper_particles", -100)
                .putString("wallpaper_scene", "not-a-real-scene").commit()
            assertEquals(WallpaperSettings(particleDensity = 0), WallpaperPreferences.read(preferences))
            val selected = WallpaperSettings(reduceMotion = true, fps = 60, scene = WallpaperScene.ASCENSION_FLOW)
            WallpaperPreferences.write(preferences, selected)
            assertEquals(selected, WallpaperPreferences.read(preferences))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    private fun assertVideoScene(scene: WallpaperScene, expectedWidth: Int, expectedHeight: Int, minDurationMs: Long) {
        val file = WallpaperAssetStore.materialize(context, scene)
        assertNotNull("Supplied video failed checksum reconstruction: $scene", file)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(requireNotNull(file).absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            assertEquals("Bundled video width changed", expectedWidth, width)
            assertEquals("Bundled video height changed", expectedHeight, height)
            assertTrue("Bundled video duration is unexpectedly short", duration >= minDurationMs)
        } finally {
            retriever.release()
        }

        val player = MediaPlayer()
        try {
            player.setDataSource(requireNotNull(file).absolutePath)
            player.prepare()
            assertTrue("Android MediaPlayer rejected the bundled video duration", player.duration >= minDurationMs)
        } finally {
            player.release()
        }
    }
}
