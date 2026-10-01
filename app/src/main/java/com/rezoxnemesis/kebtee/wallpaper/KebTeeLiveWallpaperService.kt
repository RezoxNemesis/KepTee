package com.rezoxnemesis.kebtee.wallpaper

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.rezoxnemesis.kebtee.R

/** Uses the bundled reference artwork directly and adds only a gentle, seamless breathing animation. */
class KebTeeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SilhouetteEngine()

    private inner class SilhouetteEngine : Engine() {
        private val renderThread = HandlerThread("KebTeeSilhouetteRenderer").apply { start() }
        private val handler = Handler(renderThread.looper)
        @Volatile private var visible = false
        private val preferences = getSharedPreferences("kebtee_preferences", MODE_PRIVATE)
        @Volatile private var reduceMotion = preferences.getBoolean("reduce_motion", false)
        private val figureBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.kebtee_silhouette)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val startedAt = SystemClock.uptimeMillis()
        private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "reduce_motion") {
                reduceMotion = preferences.getBoolean("reduce_motion", false)
                if (visible) {
                    handler.removeCallbacks(drawFrame)
                    handler.post(drawFrame)
                }
            }
        }

        private val drawFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                drawScene()
                handler.removeCallbacks(this)
                if (visible && !reduceMotion) handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawFrame)
            if (visible) handler.post(drawFrame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) {
                handler.removeCallbacks(drawFrame)
                handler.post(drawFrame)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawFrame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(drawFrame)
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            renderThread.quitSafely()
            super.onDestroy()
        }

        private fun drawScene() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas() ?: return
                val width = canvas.width.toFloat()
                val height = canvas.height.toFloat()
                if (width <= 0f || height <= 0f) return
                canvas.drawColor(Color.BLACK)

                val seconds = if (reduceMotion) 0f else (SystemClock.uptimeMillis() - startedAt) / 1000f
                val pulse = if (reduceMotion) 1f else SilhouetteAnimationMath.breathingPulse(seconds)
                val pulseAmount = ((pulse - 0.82f) / 0.18f).coerceIn(0f, 1f)
                val breathingScale = if (reduceMotion) 1f else 0.86f + pulseAmount * 0.012f
                val fitScale = maxOf(width / figureBitmap.width, height / figureBitmap.height) * breathingScale
                val destWidth = figureBitmap.width * fitScale
                val destHeight = figureBitmap.height * fitScale
                val destination = RectF(
                    (width - destWidth) / 2f,
                    (height - destHeight) / 2f,
                    (width + destWidth) / 2f,
                    (height + destHeight) / 2f
                )

                // The original image remains the source. Only its brightness and scale breathe gently.
                val brightness = if (reduceMotion) 1f else 0.72f + pulseAmount * 0.28f
                paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                    brightness, 0f, 0f, 0f, 0f,
                    0f, brightness, 0f, 0f, 0f,
                    0f, 0f, brightness, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
                canvas.drawBitmap(figureBitmap, null, destination, paint)
                paint.colorFilter = null
            } catch (_: Exception) {
                // Android can destroy the wallpaper surface during launcher transitions.
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
        }
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 33L
    }
}
