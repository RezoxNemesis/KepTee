package com.rezoxnemesis.kebtee.wallpaper

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.rezoxnemesis.kebtee.R
import java.util.Calendar
import kotlin.math.sin

/** Original bundled artwork with restrained monochrome lighting; all drawing owns one render thread. */
class KebTeeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SilhouetteEngine()

    private inner class SilhouetteEngine : Engine() {
        private val renderThread = HandlerThread("KepTeeWallpaperRenderer").apply { start() }
        private val handler = Handler(renderThread.looper)
        private val preferences = getSharedPreferences(WallpaperPreferences.FILE_NAME, MODE_PRIVATE)
        private val power = getSystemService(PowerManager::class.java)
        private val keyguard = getSystemService(KeyguardManager::class.java)
        @Volatile private var visible = false
        @Volatile private var surfaceReady = false
        @Volatile private var destroyed = false
        @Volatile private var settings = WallpaperPreferences.read(preferences)
        @Volatile private var powerSaver = power.isPowerSaveMode
        @Volatile private var interactive = power.isInteractive
        @Volatile private var charging = false
        @Volatile private var deviceLocked = keyguard.isKeyguardLocked
        @Volatile private var hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        private var receiverRegistered = false
        // Accessed only by the render thread, including disposal.
        private var figureBitmap: Bitmap? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val effectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val destination = RectF()
        private val brightnessFilters = Array(65) { index ->
            val brightness = 0.35f + index / 64f * 0.65f
            ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                brightness, 0f, 0f, 0f, 0f, 0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f, 0f, 0f, 0f, 1f, 0f
            )))
        }
        private var glowShader: Shader? = null
        private var shaderWidth = 0
        private var shaderHeight = 0
        private var animationSeconds = 0f
        private var lastFrameTime = 0L
        private var offset = 0f
        private var touchX = 0f
        private var touchY = 0f
        private var touchAt = -10_000L

        private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (WallpaperPreferences.isWallpaperKey(key)) {
                settings = WallpaperPreferences.read(preferences)
                requestFrame()
            }
        }
        private val stateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                powerSaver = power.isPowerSaveMode
                interactive = power.isInteractive
                deviceLocked = keyguard.isKeyguardLocked
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                }
                requestFrame()
            }
        }
        private val drawFrame = object : Runnable {
            override fun run() {
                if (!canDraw()) { lastFrameTime = 0L; return }
                val start = SystemClock.uptimeMillis()
                val config = settings
                val locked = deviceLocked && !isPreview
                if (lastFrameTime != 0L && !config.reduceMotion) {
                    animationSeconds += (start - lastFrameTime).coerceAtMost(100L) / 1000f * config.speed
                }
                lastFrameTime = start
                drawScene(config, locked, start)
                handler.removeCallbacks(this)
                val interval = config.frameIntervalMillis(powerSaver, locked)
                if (canDraw() && interval > 0L) {
                    handler.postDelayed(this, (interval - (SystemClock.uptimeMillis() - start)).coerceAtLeast(1L))
                }
            }
        }

        private fun canDraw() = visible && surfaceReady && interactive && !destroyed

        // Serialize scheduling with drawing. A request arriving during a frame must survive
        // that frame's callback cleanup, especially when reduced motion has no next tick.
        private val scheduleFrame = Runnable {
            handler.removeCallbacks(drawFrame)
            if (canDraw()) handler.post(drawFrame) else lastFrameTime = 0L
        }

        private fun requestFrame() {
            if (destroyed) return
            handler.removeCallbacks(scheduleFrame)
            handler.post(scheduleFrame)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
            val filter = IntentFilter().apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(this@KebTeeLiveWallpaperService, stateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            interactive = power.isInteractive
            deviceLocked = keyguard.isKeyguardLocked
            handler.post { lastFrameTime = 0L }
            requestFrame()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            requestFrame()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceReady = true
            requestFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            requestFrame()
            super.onSurfaceDestroyed(holder)
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            handler.post { offset = (xOffset - 0.5f).coerceIn(-0.5f, 0.5f) }
            requestFrame()
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN && settings.touchEffects && !settings.reduceMotion) {
                val x = event.x
                val y = event.y
                handler.post { touchX = x; touchY = y; touchAt = SystemClock.uptimeMillis() }
                requestFrame()
            }
            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            destroyed = true
            visible = false
            handler.removeCallbacksAndMessages(null)
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            if (receiverRegistered) unregisterReceiver(stateReceiver)
            // Recycling is queued behind an in-flight frame, never racing drawBitmap.
            handler.post { figureBitmap?.recycle(); figureBitmap = null; glowShader = null }
            renderThread.quitSafely()
            super.onDestroy()
        }

        private fun drawScene(config: WallpaperSettings, locked: Boolean, now: Long) {
            if (!surfaceHolder.surface.isValid) return
            if (figureBitmap == null) {
                figureBitmap = BitmapFactory.decodeResource(resources, R.drawable.kebtee_silhouette,
                    BitmapFactory.Options().apply { inScaled = false }) ?: return
            }
            val bitmap = figureBitmap ?: return
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas() ?: return
                val width = canvas.width.toFloat()
                val height = canvas.height.toFloat()
                if (width <= 0f || height <= 0f) return
                canvas.drawColor(Color.BLACK)
                val seconds = if (config.reduceMotion) 0f else animationSeconds
                val pulse = if (config.reduceMotion) 1f else SilhouetteAnimationMath.breathingPulse(seconds)
                // Fit, rather than fill: the source composition is never cropped.
                val fit = minOf(width / bitmap.width, height / bitmap.height) * (0.97f + pulse * 0.025f)
                val imageWidth = bitmap.width * fit
                val imageHeight = bitmap.height * fit
                val shift = if (config.reduceMotion) 0f else offset * width * 0.01f
                destination.set((width - imageWidth) / 2f + shift, (height - imageHeight) / 2f,
                    (width + imageWidth) / 2f + shift, (height + imageHeight) / 2f)
                val night = config.timeEffects && (hour < 6 || hour >= 21)
                val dim = if (locked && config.dimOnLock) 0.65f else if (night) 0.86f else 1f
                val brightness = ((0.78f + pulse * 0.22f) * dim).coerceIn(0.35f, 1f)
                paint.colorFilter = brightnessFilters[((brightness - 0.35f) / 0.65f * 64).toInt().coerceIn(0, 64)]
                canvas.drawBitmap(bitmap, null, destination, paint)
                paint.colorFilter = null

                if (config.glow > 0f) {
                    if (shaderWidth != canvas.width || shaderHeight != canvas.height || glowShader == null) {
                        shaderWidth = canvas.width; shaderHeight = canvas.height
                        glowShader = RadialGradient(width * 0.5f, height * 0.45f, minOf(width, height) * 0.48f,
                            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    }
                    effectPaint.shader = glowShader
                    val chargingBoost = if (charging && config.chargingEffects) 1.7f else 1f
                    effectPaint.alpha = (config.glow * 22f * pulse * chargingBoost * dim).toInt().coerceIn(0, 70)
                    canvas.drawRect(0f, 0f, width, height, effectPaint)
                    effectPaint.shader = null
                }
                effectPaint.color = Color.WHITE
                val particles = config.activeParticleCount(powerSaver, locked)
                for (index in 0 until particles) {
                    val x = ((index * 0.618034f) % 1f) * width + sin(seconds * 0.16f + index) * 9f
                    val y = (1f - ((index * 0.381966f + seconds * 0.015f) % 1f)) * height
                    effectPaint.alpha = (24f + 30f * (0.5f + 0.5f * sin(seconds + index))).toInt()
                    canvas.drawCircle(x, y, 1f + index % 3 * 0.55f, effectPaint)
                }
                val touchAge = (now - touchAt) / 1000f
                if (config.touchEffects && !config.reduceMotion && touchAge in 0f..1f) {
                    effectPaint.style = Paint.Style.STROKE
                    effectPaint.strokeWidth = resources.displayMetrics.density * 1.2f
                    effectPaint.alpha = ((1f - touchAge) * 95).toInt()
                    canvas.drawCircle(touchX, touchY, 10f + touchAge * width * 0.22f, effectPaint)
                    effectPaint.style = Paint.Style.FILL
                }
                effectPaint.alpha = 255
            } catch (_: IllegalArgumentException) {
                // The system may replace the Surface between validity checking and locking.
            } catch (_: IllegalStateException) {
                // A disappearing wallpaper host must not crash the launcher process.
            } finally {
                canvas?.let { try { surfaceHolder.unlockCanvasAndPost(it) } catch (_: IllegalArgumentException) { } catch (_: IllegalStateException) { } }
            }
        }
    }
}
