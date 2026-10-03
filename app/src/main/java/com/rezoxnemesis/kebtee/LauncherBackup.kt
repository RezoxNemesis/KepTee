package com.rezoxnemesis.kebtee

import android.app.AlertDialog
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.rezoxnemesis.kebtee.wallpaper.WallpaperPreferences
import com.rezoxnemesis.kebtee.wallpaper.WallpaperScene
import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Construct during onCreate, before the Activity reaches STARTED. No storage permission is needed. */
class LauncherBackup(
    private val activity: ComponentActivity,
    private val onRestore: (LauncherLayout) -> Unit,
    private val onMessage: (String) -> Unit
) {
    private var pendingExport: String? = null
    private val wallpaperPreferences get() = activity.getSharedPreferences(WallpaperPreferences.FILE_NAME, Context.MODE_PRIVATE)

    private val exportDocument = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val contents = pendingExport
        pendingExport = null
        if (uri != null) {
            if (contents == null) onMessage("Backup interrupted. Please export again.")
            else activity.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val output = activity.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open destination")
                        output.use { it.write(contents.toByteArray(Charsets.UTF_8)) }
                    }
                    onMessage("Home layout and wallpaper settings exported.")
                } catch (cancelled: CancellationException) { throw cancelled }
                  catch (_: Exception) { onMessage("Could not save backup. Choose another destination and try again.") }
            }
        }
    }

    private val importDocument = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) activity.lifecycleScope.launch {
            try {
                val backup = withContext(Dispatchers.IO) {
                    val input = activity.contentResolver.openInputStream(uri) ?: error("Cannot open backup")
                    val bytes = input.use { it.readBytesBounded(LauncherBackupCodec.MAX_BYTES) }
                    LauncherBackupCodec.decode(bytes.toString(Charsets.UTF_8))
                }
                if (!activity.isFinishing && !activity.isDestroyed) {
                    AlertDialog.Builder(activity)
                        .setTitle("Restore KepTee backup?")
                        .setMessage("Replace your home pages, folders, dock, hidden apps and customisation with this backup? Wallpaper controls will also be restored. App installations, the active wallpaper and existing widgets are unchanged; widget IDs are never transferred.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Restore") { _, _ ->
                            // Parsing and validation are complete before either setting store is changed.
                            WallpaperPreferences.write(wallpaperPreferences, backup.wallpaper)
                            onRestore(backup.layout)
                            onMessage("Home layout and wallpaper settings restored.")
                        }.show()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { onMessage("This is not a supported KepTee backup, or the file could not be read. Nothing was changed.") }
        }
    }

    fun backup(layout: LauncherLayout) {
        try {
            pendingExport = LauncherBackupCodec.encode(LauncherBackupData(layout.normalized(), WallpaperPreferences.read(wallpaperPreferences)))
        } catch (_: IllegalArgumentException) {
            onMessage("This layout exceeds the backup size limit.")
            return
        }
        try { exportDocument.launch("KepTee-backup.json") }
        catch (_: android.content.ActivityNotFoundException) { pendingExport = null; onMessage("No document picker is available on this device.") }
    }

    fun restore() {
        try { importDocument.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        catch (_: android.content.ActivityNotFoundException) { onMessage("No document picker is available on this device.") }
    }
}

internal data class LauncherBackupData(val layout: LauncherLayout, val wallpaper: WallpaperSettings)

/** Versioned data-only schema. Device-bound widget IDs and permissions are deliberately excluded. */
internal object LauncherBackupCodec {
    const val MAX_BYTES = 1_000_000

    fun encode(data: LauncherBackupData): String {
        val settings = data.wallpaper.normalized()
        val raw = JSONObject().put("format", "KepTee-backup").put("version", 1)
            .put("layout", JSONObject(LauncherLayoutStore.encode(data.layout.normalized())))
            .put("wallpaper", JSONObject().put("speed", settings.speed.toDouble()).put("glow", settings.glow.toDouble())
                .put("particleDensity", settings.particleDensity).put("touchEffects", settings.touchEffects)
                .put("batterySaver", settings.batterySaver).put("fps", settings.fps)
                .put("timeEffects", settings.timeEffects).put("chargingEffects", settings.chargingEffects)
                .put("dimOnLock", settings.dimOnLock).put("reduceMotion", settings.reduceMotion)
                .put("tiltMotion", settings.tiltMotion).put("scene", settings.scene.id)).toString()
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        return raw
    }

    fun decode(raw: String): LauncherBackupData {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds size limit" }
        val json = JSONObject(raw)
        require(json.getString("format") == "KepTee-backup") { "Not a KepTee backup" }
        require(integer(json, "version") == 1) { "Unsupported backup version" }
        val layoutJson = json.getJSONObject("layout")
        require(integer(layoutJson, "version") == 1) { "Unsupported layout version" }
        validateLayout(layoutJson)
        val layout = LauncherLayoutStore.decode(layoutJson.toString())
        val value = json.getJSONObject("wallpaper")
        val speed = number(value, "speed").toFloat()
        val glow = number(value, "glow").toFloat()
        val particles = integer(value, "particleDensity")
        val fps = integer(value, "fps")
        require(speed in 0.25f..2f && glow in 0f..1f && particles in 0..60 && fps in listOf(15, 30, 60, 120)) { "Invalid wallpaper controls" }
        return LauncherBackupData(layout, WallpaperSettings(
            speed = speed, glow = glow, particleDensity = particles,
            touchEffects = flag(value, "touchEffects"), batterySaver = flag(value, "batterySaver"), fps = fps,
            timeEffects = flag(value, "timeEffects"), chargingEffects = flag(value, "chargingEffects"),
            dimOnLock = flag(value, "dimOnLock"), reduceMotion = flag(value, "reduceMotion"),
            tiltMotion = optionalFlag(value, "tiltMotion", true),
            scene = WallpaperScene.fromId(optionalString(value, "scene"))
        ))
    }

    private fun validateLayout(json: JSONObject) {
        require(integer(json, "columns") in 3..6 && integer(json, "iconSize") in 40..72)
        flag(json, "labels"); flag(json, "locked")
        require(json.getString("drawerStyle") in listOf("Grid", "List"))
        require(json.getString("folderStyle") in listOf("Glass", "Radial"))
        require(json.getString("swipeDown") in LauncherLayout.ACTIONS)
        require(json.getString("doubleTap") in LauncherLayout.ACTIONS)
        fun validateApps(array: org.json.JSONArray, maximum: Int) {
            require(array.length() <= maximum)
            for (i in 0 until array.length()) {
                val value = array.get(i)
                require(value is String && LauncherLayout.validAppKey(value))
            }
        }
        validateApps(json.getJSONArray("dock"), 5)
        validateApps(json.getJSONArray("hidden"), 2000)
        val pages = json.getJSONArray("pages")
        val ids = mutableSetOf<String>()
        val members = mutableSetOf<String>()
        require(pages.length() in 1..12)
        for (i in 0 until pages.length()) {
            val page = pages.getJSONArray(i)
            require(page.length() <= 100)
            for (j in 0 until page.length()) {
                val item = page.getJSONObject(j)
                val id = item.get("id")
                require(id is String && id.length in 1..520)
                require(id.startsWith("folder:") || LauncherLayout.validAppKey(id))
                require(ids.add(id)) { "Duplicate home item" }
                val apps = item.getJSONArray("apps")
                require(apps.length() in 1..100)
                validateApps(apps, 100)
                require(id.startsWith("folder:") || (apps.length() == 1 && apps.getString(0) == id)) { "Invalid app item" }
                for (k in 0 until apps.length()) {
                    require(members.add(apps.getString(k))) { "Duplicate home app" }
                }
                val name = item.get("name")
                require(name is String && name.length <= 48)
                flag(item, "silver")
            }
        }
    }

    private fun number(json: JSONObject, key: String): Double {
        val value = json.get(key)
        require(value is Number) { "Expected numeric setting" }
        return value.toDouble().also { require(it.isFinite()) }
    }

    private fun integer(json: JSONObject, key: String): Int {
        val value = number(json, key)
        require(value % 1 == 0.0 && value >= Int.MIN_VALUE && value <= Int.MAX_VALUE) { "Expected integer setting" }
        return value.toInt()
    }

    private fun optionalFlag(json: JSONObject, key: String, default: Boolean): Boolean {
        if (!json.has(key)) return default
        return flag(json, key)
    }

    private fun optionalString(json: JSONObject, key: String): String? {
        if (!json.has(key)) return null
        val value = json.get(key)
        require(value is String) { "Expected string setting" }
        return value
    }

    private fun flag(json: JSONObject, key: String): Boolean {
        val value = json.get(key)
        require(value is Boolean) { "Expected boolean setting" }
        return value
    }
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= limit) { "Backup exceeds size limit" }
        result.write(buffer, 0, count)
    }
    return result.toByteArray()
}
