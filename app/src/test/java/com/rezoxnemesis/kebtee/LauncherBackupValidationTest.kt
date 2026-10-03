package com.rezoxnemesis.kebtee

import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test

class LauncherBackupValidationTest {
    private val homeApp = "com.example.home/Main"
    private val otherApp = "com.example.other/Main"

    private fun validBackup(): JSONObject {
        val layout = LauncherLayout(
            pages = listOf(listOf(HomeItem(homeApp, listOf(homeApp)))),
            dock = listOf(otherApp)
        )
        return JSONObject(LauncherBackupCodec.encode(LauncherBackupData(layout, WallpaperSettings())))
    }

    @Test fun rejectsDuplicateDockEntries() {
        val root = validBackup()
        root.getJSONObject("layout").put("dock", JSONArray().put(otherApp).put(otherApp))

        assertThrows(IllegalArgumentException::class.java) {
            LauncherBackupCodec.decode(root.toString())
        }
    }

    @Test fun rejectsAnAppPlacedInBothDockAndHomePage() {
        val root = validBackup()
        root.getJSONObject("layout").put("dock", JSONArray().put(homeApp))

        assertThrows(IllegalArgumentException::class.java) {
            LauncherBackupCodec.decode(root.toString())
        }
    }
}
