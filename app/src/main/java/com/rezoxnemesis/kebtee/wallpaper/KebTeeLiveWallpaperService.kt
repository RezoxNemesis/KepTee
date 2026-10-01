package com.rezoxnemesis.kebtee.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin

/** Lightweight animated wallpaper. Rendering stops whenever the wallpaper is not visible. */
class KebTeeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = AuroraEngine()

    private inner class AuroraEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val startedAt = SystemClock.uptimeMillis()

        private val drawFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                drawScene()
                handler.removeCallbacks(this)
                handler.postDelayed(this, 40L)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) drawFrame.run() else handler.removeCallbacks(drawFrame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawFrame.run()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawFrame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(drawFrame)
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
                    val t = (SystemClock.uptimeMillis() - startedAt) / 1000f

                    paint.shader = LinearGradient(
                        0f, 0f, w, h,
                        intArrayOf(Color.rgb(5, 10, 30), Color.rgb(27, 15, 70), Color.rgb(5, 20, 42)),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, 0f, w, h, paint)
                    paint.shader = null

                    for (band in 0..3) {
                        val path = Path()
                        val baseY = h * (0.42f + band * 0.085f)
                        val amp = h * (0.045f + band * 0.008f)
                        path.moveTo(0f, baseY)
                        var x = 0f
                        val step = (w / 90f).coerceAtLeast(1f)
                        while (x <= w) {
                            val phase = x / w * 5.8f + t * (0.35f + band * 0.06f) + band
                            val y = baseY + sin(phase.toDouble()).toFloat() * amp +
                                cos((phase * 0.47f).toDouble()).toFloat() * amp * 0.45f
                            path.lineTo(x, y)
                            x += step
                        }
                        path.lineTo(w, h)
                        path.lineTo(0f, h)
                        path.close()
                        val colors = when (band % 3) {
                            0 -> intArrayOf(Color.argb(15, 34, 211, 238), Color.argb(95, 139, 92, 246), Color.argb(10, 236, 72, 153))
                            1 -> intArrayOf(Color.argb(8, 139, 92, 246), Color.argb(80, 34, 211, 238), Color.argb(8, 139, 92, 246))
                            else -> intArrayOf(Color.argb(10, 236, 72, 153), Color.argb(75, 139, 92, 246), Color.argb(8, 34, 211, 238))
                        }
                        paint.shader = LinearGradient(0f, baseY - amp, w, baseY + amp, colors, null, Shader.TileMode.CLAMP)
                        canvas.drawPath(path, paint)
                        paint.shader = null
                    }

                    for (i in 0 until 42) {
                        val phase = i * 12.9898f
                        val sx = ((i * 73.7f) % w + sin((t * 0.12f + phase).toDouble()).toFloat() * 14f + w) % w
                        val sy = (i * 131.3f) % h
                        paint.color = Color.argb(if (i % 4 == 0) 210 else 110, 205, 230, 255)
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
