package com.rezoxnemesis.kebtee.wallpaper

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import com.rezoxnemesis.kebtee.R
import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/** Renders the selected built-in procedural scene without network access. */
class KebTeeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SceneEngine()

    private inner class SceneEngine : Engine() {
        private val renderThread = HandlerThread("KebTeeWallpaperRenderer").apply { start() }
        private val handler = Handler(renderThread.looper)
        private val wallpaperPrefs = WallpaperPreferences(this@KebTeeLiveWallpaperService)
        private val wallpaperStore = getSharedPreferences("kebtee_wallpaper", MODE_PRIVATE)
        private val globalPrefs = getSharedPreferences("kebtee_preferences", MODE_PRIVATE)
        private val renderer by lazy { WallpaperRenderer(BitmapFactory.decodeResource(this@KebTeeLiveWallpaperService.resources, R.drawable.kebtee_silhouette)) }
        @Volatile private var visible = false
        @Volatile private var scene = WallpaperCatalog.scenes.first()
        @Volatile private var settings = WallpaperSettings()
        private var startedAt = SystemClock.uptimeMillis()

        private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            reloadState()
            if (visible) {
                handler.removeCallbacks(drawFrame)
                handler.post(drawFrame)
            }
        }

        private val drawFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                drawScene()
                handler.removeCallbacks(this)
                val batteryInterval = if (settings.batteryMode) 66L else 33L
                if (visible && !settings.reducedMotion) handler.postDelayed(this, batteryInterval)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            wallpaperStore.registerOnSharedPreferenceChangeListener(preferenceListener)
            globalPrefs.registerOnSharedPreferenceChangeListener(preferenceListener)
            reloadState()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawFrame)
            if (visible) {
                startedAt = SystemClock.uptimeMillis()
                handler.post(drawFrame)
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
            wallpaperStore.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            globalPrefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            renderThread.quitSafely()
            super.onDestroy()
        }

        private fun reloadState() {
            val selected = WallpaperCatalog.byId(wallpaperPrefs.selectedSceneId()) ?: WallpaperCatalog.scenes.first()
            val saved = wallpaperPrefs.settingsFor(selected.id)
            scene = selected
            settings = saved.copy(reducedMotion = saved.reducedMotion || globalPrefs.getBoolean("reduce_motion", false))
            startedAt = SystemClock.uptimeMillis()
        }

        private fun drawScene() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas() ?: return
                val width = canvas.width
                val height = canvas.height
                if (width <= 0 || height <= 0) return
                val seconds = (SystemClock.uptimeMillis() - startedAt) / 1000f
                renderer.draw(
                    canvas,
                    scene,
                    WallpaperRenderState(
                        width = width,
                        height = height,
                        timeSeconds = seconds,
                        speed = settings.speed,
                        intensity = settings.intensity,
                        reducedMotion = settings.reducedMotion,
                        batteryMode = settings.batteryMode
                    )
                )
            } catch (_: Exception) {
                // Surface lifecycle can race launcher transitions; the next frame recovers.
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // Surface may have been destroyed between lock and post.
                    }
                }
            }
        }
    }
}
