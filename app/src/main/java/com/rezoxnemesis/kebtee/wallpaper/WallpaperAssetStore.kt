package com.rezoxnemesis.kebtee.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Reconstructs the user-supplied wallpaper media from small packaged Base64 asset fragments.
 * Fragmenting is a repository transport detail only; runtime media is restored byte-for-byte
 * to an app-private cache file and checksum-verified before use.
 */
object WallpaperAssetStore {
    private data class Spec(
        val directory: String,
        val fileName: String,
        val size: Long,
        val sha256: String
    )

    private val specs = mapOf(
        WallpaperScene.ASCENSION_STILL to Spec(
            "wallpaper/ascension_still", "keptee_ascension_still.jpg", 172_515L,
            "a16ed601f2b31e1df04749636179654b32d67908b66b5e794f3014271018f1d7"
        ),
        WallpaperScene.ASCENSION_FLOW to Spec(
            "wallpaper/ascension_flow", "keptee_ascension_flow.mp4", 3_134_599L,
            "53e9e8c1c1b846cd9c86f6264322c24af508e60a86c2b1b952bbd0aa13d4cf35"
        ),
        WallpaperScene.AURA_PULSE to Spec(
            "wallpaper/aura_pulse", "keptee_aura_pulse.mp4", 1_389_600L,
            "5c88c642f322c8e99a144a441e5a2332613c84f9b4b240e006c9582edbf22138"
        )
    )

    /**
     * Successful checksum validation is remembered for this process. The files live in the
     * app-private cache, so re-reading and hashing multi-megabyte video files on every wallpaper
     * offset/settings callback adds I/O without improving integrity.
     */
    private val verifiedFiles = mutableMapOf<WallpaperScene, File>()

    @Synchronized
    fun materialize(context: Context, scene: WallpaperScene): File? {
        val spec = specs[scene] ?: return null
        verifiedFiles[scene]?.let { cached ->
            if (cached.exists() && cached.length() == spec.size) return cached
            verifiedFiles.remove(scene)
        }

        val root = File(context.cacheDir, "keptee-wallpaper-media")
        if (!root.exists() && !root.mkdirs()) return null
        val target = File(root, spec.fileName)
        if (target.length() == spec.size && sha256(target) == spec.sha256) {
            verifiedFiles[scene] = target
            return target
        }

        val parts = context.assets.list(spec.directory)?.sorted().orEmpty()
        if (parts.isEmpty()) return null

        val temp = File(root, spec.fileName + ".tmp")
        return try {
            FileOutputStream(temp, false).use { output ->
                for (part in parts) {
                    val encoded = context.assets.open("${spec.directory}/$part").bufferedReader().use { it.readText() }
                    output.write(Base64.decode(encoded, Base64.NO_WRAP))
                }
                output.fd.sync()
            }
            if (temp.length() != spec.size || sha256(temp) != spec.sha256) {
                temp.delete()
                null
            } else {
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
                verifiedFiles[scene] = target
                target
            }
        } catch (_: Exception) {
            temp.delete()
            null
        }
    }

    fun decodeStill(context: Context): Bitmap? =
        materialize(context, WallpaperScene.ASCENSION_STILL)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inScaled = false })
        }

    private fun sha256(file: File): String = try {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    } catch (_: Exception) {
        ""
    }
}
