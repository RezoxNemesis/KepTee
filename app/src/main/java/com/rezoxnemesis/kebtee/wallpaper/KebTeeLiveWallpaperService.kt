package com.rezoxnemesis.kebtee.wallpaper

import android.content.SharedPreferences
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/** Black-and-white live wallpaper with a continuously breathing, luminous T-pose figure. */
class KebTeeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SilhouetteEngine()

    private inner class SilhouetteEngine : Engine() {
        private val renderThread = HandlerThread("KebTeeSilhouetteRenderer").apply { start() }
        private val handler = Handler(renderThread.looper)
        @Volatile private var visible = false
        private val preferences = getSharedPreferences("kebtee_preferences", MODE_PRIVATE)
        @Volatile private var reduceMotion = preferences.getBoolean("reduce_motion", false)
        private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "reduce_motion") {
                reduceMotion = preferences.getBoolean("reduce_motion", false)
                if (visible) {
                    handler.removeCallbacks(drawFrame)
                    handler.post(drawFrame)
                }
            }
        }

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val startedAt = SystemClock.uptimeMillis()
        private val figurePath = buildFigurePath()
        private val designBounds = RectF(0f, 0f, DESIGN_WIDTH, DESIGN_HEIGHT)

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
                canvas = surfaceHolder.lockCanvas()
                if (canvas == null) return
                val width = canvas.width.toFloat()
                val height = canvas.height.toFloat()
                if (width <= 0f || height <= 0f) return

                canvas.drawColor(Color.BLACK)
                val scale = minOf(width / DESIGN_WIDTH, height / DESIGN_HEIGHT)
                val left = (width - DESIGN_WIDTH * scale) / 2f
                val top = (height - DESIGN_HEIGHT * scale) / 2f
                val seconds = if (reduceMotion) 0f else
                    (SystemClock.uptimeMillis() - startedAt) / 1000f
                val pulse = if (reduceMotion) 0.92f else
                    SilhouetteAnimationMath.breathingPulse(seconds)
                val sweep = if (reduceMotion) 0.5f else
                    SilhouetteAnimationMath.sweepProgress(seconds)
                val pulseAmount = ((pulse - 0.82f) / 0.18f).coerceIn(0f, 1f)

                canvas.save()
                canvas.translate(left, top)
                canvas.scale(scale, scale)

                // A restrained aura gives the white figure depth without tinting the black field.
                paint.shader = null
                paint.xfermode = null
                paint.color = Color.WHITE
                paint.alpha = (22f + pulseAmount * 48f).toInt()
                paint.maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
                drawFigure(canvas, paint)
                paint.maskFilter = null

                // Keep the figure's lower body intentionally dim, matching the reference image.
                paint.alpha = (215f + pulseAmount * 40f).toInt().coerceIn(0, 255)
                paint.shader = figureGradient()
                drawFigure(canvas, paint)
                paint.shader = null
                paint.alpha = 255

                // A soft highlight travels fingertip-to-fingertip, clipped to the silhouette.
                val layer = canvas.saveLayer(designBounds, null)
                paint.shader = figureGradient()
                paint.xfermode = null
                paint.alpha = (215f + pulseAmount * 40f).toInt().coerceIn(0, 255)
                drawFigure(canvas, paint)
                paint.alpha = 255
                val sweepX = 130f + 740f * sweep
                paint.shader = RadialGradient(
                    sweepX, 585f, 230f,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.argb(108, 255, 255, 255),
                        Color.argb(30, 255, 255, 255),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.22f, 0.58f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                canvas.drawRect(designBounds, paint)
                paint.xfermode = null
                paint.shader = null
                canvas.restoreToCount(layer)

                canvas.restore()
            } catch (_: Exception) {
                // Android may destroy the surface during a launcher transition.
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
        }

        private fun figureGradient() = LinearGradient(
            0f, 430f, 0f, 1225f,
            intArrayOf(
                Color.rgb(250, 250, 250),
                Color.rgb(242, 243, 245),
                Color.rgb(175, 177, 181),
                Color.rgb(44, 45, 49),
                Color.rgb(3, 3, 4)
            ),
            floatArrayOf(0f, 0.28f, 0.52f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )

        private fun drawFigure(canvas: Canvas, targetPaint: Paint) {
            canvas.drawPath(figurePath, targetPaint)
            canvas.drawOval(458f, 414f, 542f, 526f, targetPaint)
            canvas.drawOval(115f, 558f, 166f, 600f, targetPaint)
            canvas.drawOval(834f, 558f, 885f, 600f, targetPaint)
        }
    }

    private fun buildFigurePath(): Path = Path().apply {
        // Arms and shoulders: a relaxed, straight T-pose with rounded edges.
        moveTo(466f, 520f)
        cubicTo(432f, 525f, 414f, 542f, 384f, 556f)
        cubicTo(353f, 571f, 314f, 578f, 270f, 580f)
        lineTo(174f, 580f)
        lineTo(150f, 571f)
        cubicTo(139f, 567f, 136f, 572f, 145f, 579f)
        lineTo(163f, 588f)
        lineTo(141f, 584f)
        cubicTo(128f, 582f, 127f, 590f, 142f, 594f)
        lineTo(164f, 599f)
        lineTo(145f, 599f)
        cubicTo(133f, 599f, 134f, 606f, 150f, 608f)
        lineTo(191f, 607f)
        lineTo(274f, 612f)
        cubicTo(324f, 614f, 375f, 610f, 416f, 621f)
        lineTo(438f, 646f)
        lineTo(562f, 646f)
        lineTo(584f, 621f)
        cubicTo(625f, 610f, 676f, 614f, 726f, 612f)
        lineTo(809f, 607f)
        lineTo(850f, 608f)
        cubicTo(866f, 606f, 867f, 599f, 855f, 599f)
        lineTo(836f, 599f)
        lineTo(858f, 594f)
        cubicTo(873f, 590f, 872f, 582f, 859f, 584f)
        lineTo(837f, 588f)
        lineTo(855f, 579f)
        cubicTo(864f, 572f, 861f, 567f, 850f, 571f)
        lineTo(826f, 580f)
        lineTo(730f, 580f)
        cubicTo(686f, 578f, 647f, 571f, 616f, 556f)
        cubicTo(586f, 542f, 568f, 525f, 534f, 520f)
        close()

        // Neck and torso.
        moveTo(472f, 493f)
        lineTo(528f, 493f)
        lineTo(536f, 535f)
        cubicTo(554f, 543f, 570f, 558f, 574f, 584f)
        lineTo(581f, 657f)
        cubicTo(582f, 705f, 566f, 757f, 555f, 814f)
        lineTo(445f, 814f)
        cubicTo(434f, 757f, 418f, 705f, 419f, 657f)
        lineTo(426f, 584f)
        cubicTo(430f, 558f, 446f, 543f, 464f, 535f)
        close()

        // Two legs with a narrow, natural gap and softly fading feet.
        moveTo(447f, 786f)
        cubicTo(437f, 842f, 444f, 906f, 431f, 966f)
        cubicTo(424f, 1005f, 427f, 1064f, 426f, 1125f)
        lineTo(420f, 1170f)
        cubicTo(413f, 1184f, 393f, 1192f, 391f, 1204f)
        cubicTo(391f, 1214f, 402f, 1217f, 425f, 1215f)
        lineTo(451f, 1212f)
        cubicTo(460f, 1208f, 461f, 1195f, 455f, 1179f)
        lineTo(459f, 1122f)
        cubicTo(466f, 1073f, 473f, 1021f, 480f, 976f)
        lineTo(500f, 851f)
        lineTo(520f, 976f)
        cubicTo(527f, 1021f, 534f, 1073f, 541f, 1122f)
        lineTo(545f, 1179f)
        cubicTo(539f, 1195f, 540f, 1208f, 549f, 1212f)
        lineTo(575f, 1215f)
        cubicTo(598f, 1217f, 609f, 1214f, 609f, 1204f)
        cubicTo(607f, 1192f, 587f, 1184f, 580f, 1170f)
        lineTo(574f, 1125f)
        cubicTo(573f, 1064f, 576f, 1005f, 569f, 966f)
        cubicTo(556f, 906f, 563f, 842f, 553f, 786f)
        close()
    }

    private companion object {
        const val DESIGN_WIDTH = 1000f
        const val DESIGN_HEIGHT = 1500f
        const val FRAME_INTERVAL_MS = 33L
    }
}
