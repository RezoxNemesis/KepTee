package com.rezoxnemesis.kebtee.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.rezoxnemesis.kebtee.R

/**
 * Loads the authored wallpaper bitmap when it can be decoded. Some Android resource
 * configurations can return a null bitmap for the WebP drawable, so use the bundled
 * Compose-safe vector artwork as a real bitmap fallback instead of crashing or
 * rendering a blank silhouette scene.
 */
internal fun loadKebTeeSilhouette(context: Context): Bitmap {
    BitmapFactory.decodeResource(context.resources, R.drawable.kebtee_silhouette)?.let {
        return it
    }
    return ContextCompat.getDrawable(context, R.drawable.kebtee_silhouette_art)
        ?.toBitmap(600, 260, Bitmap.Config.ARGB_8888)
        ?: Bitmap.createBitmap(600, 260, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
}
