package com.rezoxnemesis.kebtee

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherPersistenceTest {
    private val a = "com.example.a/Main"
    private val b = "com.example.b/Main"
    private val data get() = LauncherBackupData(
        LauncherLayout(pages = listOf(listOf(HomeItem(a, listOf(a)), HomeItem(b, listOf(b))))),
        WallpaperSettings()
    )

    @Test fun backupRoundTripPreservesLayoutAndWallpaperControls() {
        assertEquals(data, LauncherBackupCodec.decode(LauncherBackupCodec.encode(data)))
    }

    @Test fun backupRejectsDuplicateAppsRatherThanSilentlyDroppingThem() {
        val json = JSONObject(LauncherBackupCodec.encode(data))
        val items = json.getJSONObject("layout").getJSONArray("pages").getJSONArray(0)
        items.put(items.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) { LauncherBackupCodec.decode(json.toString()) }
    }

    @Test fun backupRejectsStandaloneItemWhoseIdDoesNotMatchItsApp() {
        val json = JSONObject(LauncherBackupCodec.encode(data))
        json.getJSONObject("layout").getJSONArray("pages").getJSONArray(0)
            .getJSONObject(0).put("id", "com.example.wrong/Main")
        assertThrows(IllegalArgumentException::class.java) { LauncherBackupCodec.decode(json.toString()) }
    }

    @Test fun migrationPreservesPreviousReleaseSettingsAndAllFavouritePages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("migration-test", Context.MODE_PRIVATE)
        val favorites = (0 until 101).map { "com.example.app$it/Main" }.toSet()
        preferences.edit().clear().putStringSet("favorites", favorites).putStringSet("hidden_apps", setOf(a))
            .putInt("grid_columns", 5).putInt("icon_size_dp", 64)
            .putBoolean("show_labels", false).putBoolean("layout_locked", true).commit()
        try {
            val migrated = LauncherLayoutStore.read(preferences)
            assertEquals(2, migrated.pages.size)
            assertEquals(favorites, migrated.pages.flatten().flatMap { it.apps }.toSet())
            assertEquals(setOf(a), migrated.hidden)
            assertEquals(5, migrated.columns)
            assertEquals(64, migrated.iconSize)
            assertFalse(migrated.labels)
            assertTrue(migrated.locked)
            LauncherLayoutStore.write(preferences, migrated)
            assertEquals(migrated, LauncherLayoutStore.read(preferences))
        } finally { preferences.edit().clear().commit() }
    }
}
