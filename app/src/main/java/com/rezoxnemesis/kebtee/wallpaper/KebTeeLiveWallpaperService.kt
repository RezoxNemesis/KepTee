package com.rezoxnemesis.kebtee.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin

/** Lightweight animated wallpaper. Rendering stops whenever the wallpaper is not visible. */
class KebTeeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = AuroraEngine()

    private inner class AuroraEngine : Engine() {
        private val renderThread = HandlerThread("KebTeeAuroraRenderer").apply { start() }
        private val handler = Handler(renderThread.looper)
        @Volatile private var visible = false
        private val preferences = getSharedPreferences("kebtee_preferences", MODE_PRIVATE)
        @Volatile private var reduceMotion = preferences.getBoolean("reduce_motion", false)
        private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "reduce_motion") {
                reduceMotion = preferences.getBoolean("reduce_motion", false)
                if (visible) { handler.removeCallbacks(drawFrame); handler.post(drawFrame) }
            }
        }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val startedAt = SystemClock.uptimeMillis()

        private val drawFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                drawScene()
                handler.removeCallbacks(this)
                if (visible && !reduceMotion) handler.postDelayed(this, 40L)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) {
                handler.removeCallbacks(drawFrame)
                handler.post(drawFrame)
            } else {
                handler.removeCallbacks(drawFrame)
            }
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
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()
                    if (w <= 0f || h <= 0f) return
                    val t = if (reduceMotion) 0f else (SystemClock.uptimeMillis() - startedAt) / 1000f

                    paint.shader = LinearGradient(
                        0f, 0f, 0f, h,
                        intArrayOf(Color.rgb(3, 7, 24), Color.rgb(10, 13, 45), Color.rgb(17, 8, 45), Color.rgb(4, 12, 29)),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, 0f, w, h, paint)
                    paint.shader = RadialGradient(
                        w * 0.24f, h * 0.54f, w * 0.58f,
                        intArrayOf(Color.argb(75, 28, 83, 220), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(w * 0.24f, h * 0.54f, w * 0.58f, paint)
                    paint.shader = RadialGradient(
                        w * 0.78f, h * 0.48f, w * 0.48f,
                        intArrayOf(Color.argb(65, 103, 37, 190), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(w * 0.78f, h * 0.48f, w * 0.48f, paint)
                    paint.shader = null

                    // Thin, layered ribbons create a more dimensional aurora than broad filled waves.
                    for (band in 0..4) {
                        val path = Path()
                        val baseY = h * (0.39f + band * 0.057f)
                        val amp = h * (0.055f + band * 0.006f)
                        val thickness = h * (0.075f + band * 0.006f)
                        val step = (w / 110f).coerceAtLeast(1f)
                        fun ribbonY(x: Float): Float {
                            val phase = x / w * 5.4f + t * (0.24f + band * 0.045f) + band * 0.82f
                            return baseY + sin(phase.toDouble()).toFloat() * amp +
                                cos((phase * 0.43f).toDouble()).toFloat() * amp * 0.42f
                        }
                        path.moveTo(0f, ribbonY(0f))
                        var x = 0f
                        while (x <= w) {
                            path.lineTo(x, ribbonY(x))
                            x += step
                        }
                        path.lineTo(w, ribbonY(w) + thickness)
                        x = w
                        while (x >= 0f) {
                            path.lineTo(x, ribbonY(x) + thickness)
                            x -= step
                        }
                        path.close()
                        val colors = when (band % 4) {
                            0 -> intArrayOf(Color.argb(4, 34, 211, 238), Color.argb(92, 34, 211, 238), Color.argb(105, 139, 92, 246), Color.argb(4, 236, 72, 153))
                            1 -> intArrayOf(Color.argb(3, 139, 92, 246), Color.argb(98, 92, 64, 220), Color.argb(100, 34, 211, 238), Color.argb(3, 139, 92, 246))
                            2 -> intArrayOf(Color.argb(3, 236, 72, 153), Color.argb(80, 139, 92, 246), Color.argb(82, 75, 110, 245), Color.argb(3, 34, 211, 238))
                            else -> intArrayOf(Color.argb(3, 34, 211, 238), Color.argb(76, 40, 150, 225), Color.argb(80, 139, 92, 246), Color.argb(3, 34, 211, 238))
                        }
                        paint.shader = LinearGradient(0f, baseY - amp, w, baseY + amp + thickness, colors, null, Shader.TileMode.CLAMP)
                        canvas.drawPath(path, paint)
                        paint.shader = null
                    }

                    for (i in 0 until 42) {
                        val phase = i * 12.9898f
                        val sx = ((i * 73.7f) % w + sin((t * 0.12f + phase).toDouble()).toFloat() * 14f + w) % w
                        val sy = (i * 131.3f) % h
                        val twinkle = if (reduceMotion) 0.5f else (0.5f + 0.5f * sin(t * 1.7f + phase)).toFloat()
                        val alpha = (65f + twinkle * 155f).toInt().coerceIn(0, 220)
                        paint.color = Color.argb(alpha, 205, 230, 255)
                        canvas.drawCircle(sx, sy, if (i % 7 == 0) 2.2f else 1.1f, paint)
                    }
                }
            } catch (_: Exception) {
                // The surface can disappear during launcher transitions.
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
        }
    }
}
