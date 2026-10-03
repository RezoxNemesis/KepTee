package com.rezoxnemesis.kebtee

import android.app.WallpaperInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.service.wallpaper.WallpaperService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rezoxnemesis.kebtee.wallpaper.KebTeeLiveWallpaperService
import com.rezoxnemesis.kebtee.wallpaper.WallpaperPreferences
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

    @Suppress("DEPRECATION")
    @Test fun bundledRasterDrawablesResolveAsBitmapDrawables() {
        val logo = context.resources.getDrawable(R.drawable.kebtee_logo_master, context.theme)
        val silhouette = context.resources.getDrawable(R.drawable.kebtee_silhouette, context.theme)
        assertTrue("Logo resource must resolve as BitmapDrawable", logo is BitmapDrawable)
        assertTrue("Silhouette resource must resolve as BitmapDrawable", silhouette is BitmapDrawable)
    }

    @Test fun restoredWrongPreferenceTypesFallBackWithoutCrashing() {
        val preferences = context.getSharedPreferences("wallpaper_integration_test", Context.MODE_PRIVATE)
        try {
            preferences.edit().clear()
                .putString("wallpaper_speed", "broken")
                .putString("wallpaper_fps", "broken")
                .putInt("wallpaper_touch", 1)
                .putFloat("wallpaper_glow", Float.NaN)
                .putInt("wallpaper_particles", -100).commit()
            assertEquals(WallpaperSettings(particleDensity = 0), WallpaperPreferences.read(preferences))
            WallpaperPreferences.write(preferences, WallpaperSettings(reduceMotion = true, fps = 60))
            assertEquals(WallpaperSettings(reduceMotion = true, fps = 60), WallpaperPreferences.read(preferences))
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
