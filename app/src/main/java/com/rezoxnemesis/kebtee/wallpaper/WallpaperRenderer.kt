package com.rezoxnemesis.kebtee.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class WallpaperRenderState(
    val width: Int,
    val height: Int,
    val timeSeconds: Float,
    val speed: Float = 1f,
    val intensity: Float = 1f,
    val reducedMotion: Boolean = false,
    val batteryMode: Boolean = false
)

class WallpaperRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()

    fun draw(canvas: Canvas, scene: WallpaperScene, state: WallpaperRenderState) {
        if (state.width <= 0 || state.height <= 0) return
        val motion = WallpaperRenderMath.motionAmount(state.reducedMotion, state.batteryMode)
        val time = state.timeSeconds * state.speed
        val intensity = state.intensity.coerceIn(0f, 1.25f)

        when (scene.rendererType) {
            WallpaperRendererType.AURORA -> drawAurora(canvas, scene, time, intensity, motion)
            WallpaperRendererType.NEON_GRID -> drawNeonGrid(canvas, scene, time, intensity, motion)
            WallpaperRendererType.FLUID_WAVES -> drawFluid(canvas, scene, time, intensity, motion)
            WallpaperRendererType.GEOMETRY -> drawGeometry(canvas, scene, time, intensity, motion)
            WallpaperRendererType.SPACE -> drawSpace(canvas, scene, time, intensity, motion)
            WallpaperRendererType.NATURE -> drawNature(canvas, scene, time, intensity, motion)
            WallpaperRendererType.AMOLED_GLOW -> drawAmoled(canvas, scene, time, intensity, motion)
            WallpaperRendererType.PARTICLE_FLOW -> drawParticleFlow(canvas, scene, time, intensity, motion)
        }
    }

    private fun drawAurora(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        fillGradient(canvas, scene.palette.primary, scene.palette.secondary, vertical = true)
        val base = min(canvas.width, canvas.height).toFloat()
        repeat(5) { index ->
            val phase = index * 0.72f
            val x = canvas.width * (0.12f + index * 0.19f) + sin((time * 0.28f + phase) * 2f * PI).toFloat() * base * 0.12f * motion
            val y = canvas.height * (0.28f + index * 0.12f) + cos((time * 0.22f + phase) * 2f * PI).toFloat() * base * 0.08f * motion
            val radius = base * (0.26f + intensity * 0.05f)
            paint.shader = RadialGradient(
                x,
                y,
                radius,
                scene.palette.accent,
                withAlpha(scene.palette.accent, (0.02f + intensity * 0.12f).coerceIn(0f, 0.35f)),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.shader = null
    }

    private fun drawNeonGrid(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        canvas.drawColor(scene.palette.primary)
        val spacing = min(canvas.width, canvas.height) * 0.08f
        val sweep = WallpaperRenderMath.loopPhase(time, 6f) * spacing * 2f * motion
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, spacing * 0.018f)
        paint.color = withAlpha(scene.palette.secondary, 0.42f + intensity * 0.2f)
        var x = -spacing + sweep
        while (x < canvas.width + spacing) {
            canvas.drawLine(x, 0f, x, canvas.height.toFloat(), paint)
            x += spacing
        }
        var y = -spacing + sweep
        while (y < canvas.height + spacing) {
            canvas.drawLine(0f, y, canvas.width.toFloat(), y, paint)
            y += spacing
        }
        paint.color = scene.palette.accent
        paint.strokeWidth = maxOf(2f, spacing * 0.035f)
        val radius = min(canvas.width, canvas.height) * (0.18f + intensity * 0.03f)
        val cx = canvas.width * (0.5f + 0.18f * sin(time * 0.5f * 2f * PI).toFloat() * motion)
        val cy = canvas.height * 0.48f
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawFluid(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        canvas.drawColor(scene.palette.primary)
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        repeat(3) { wave ->
            val amplitude = height * (0.08f + 0.03f * wave) * (0.75f + intensity * 0.5f)
            val baseline = height * (0.40f + wave * 0.18f)
            path.reset()
            path.moveTo(0f, height)
            path.lineTo(0f, baseline)
            var x = 0f
            while (x <= width) {
                val normalized = x / width
                val y = baseline +
                    sin((normalized * 2.2f + time * (0.08f + wave * 0.025f)) * 2f * PI).toFloat() * amplitude * motion +
                    cos((normalized * 1.3f - time * 0.06f) * 2f * PI).toFloat() * amplitude * 0.38f * motion
                path.lineTo(x, y)
                x += 18f
            }
            path.lineTo(width, height)
            path.close()
            paint.color = withAlpha(
                when (wave) {
                    0 -> scene.palette.secondary
                    1 -> scene.palette.accent
                    else -> Color.WHITE
                },
                0.18f + 0.08f * (2 - wave)
            )
            canvas.drawPath(path, paint)
        }
    }

    private fun drawGeometry(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        fillGradient(canvas, scene.palette.primary, scene.palette.secondary, vertical = false)
        val cx = canvas.width / 2f
        val cy = canvas.height / 2f
        val base = min(canvas.width, canvas.height).toFloat()
        val rotation = WallpaperRenderMath.rotationRadians(time, 0.35f + intensity * 0.12f, 10f) * motion
        paint.style = Paint.Style.FILL
        repeat(4) { index ->
            val scale = 0.22f + index * 0.10f
            canvas.save()
            canvas.rotate(rotation * (1f + index * 0.2f) * if (index % 2 == 0) 1f else -1f, cx, cy)
            val size = base * scale
            paint.color = withAlpha(
                when (index) {
                    0 -> scene.palette.accent
                    1 -> Color.WHITE
                    2 -> scene.palette.secondary
                    else -> scene.palette.accent
                },
                0.10f + index * 0.05f
            )
            canvas.drawRoundRect(
                RectF(cx - size, cy - size, cx + size, cy + size),
                size * 0.14f,
                size * 0.14f,
                paint
            )
            canvas.restore()
        }
    }

    private fun drawSpace(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        canvas.drawColor(scene.palette.primary)
        val base = min(canvas.width, canvas.height).toFloat()
        val seed = scene.id.hashCode()
        paint.style = Paint.Style.FILL
        val stars = if (scene.batteryProfile == BatteryProfile.ACTIVE) 110 else 70
        repeat(stars) { i ->
            val x = WallpaperRenderMath.seedUnit(seed + i * 37) * canvas.width
            val y0 = WallpaperRenderMath.seedUnit(seed + i * 97) * canvas.height
            val drift = sin((time * (0.04f + WallpaperRenderMath.seedUnit(seed + i * 13) * 0.08f)) + i) * base * 0.025f * motion
            val y = (y0 + drift + canvas.height) % canvas.height
            val radius = 0.7f + WallpaperRenderMath.seedUnit(seed + i * 7) * 1.5f * intensity
            paint.color = withAlpha(scene.palette.accent, 0.25f + WallpaperRenderMath.seedUnit(seed + i * 19) * 0.65f)
            canvas.drawCircle(x, y, radius, paint)
        }
        val orbitRadius = base * 0.20f
        val angle = WallpaperRenderMath.rotationRadians(time, 0.20f, 14f) * motion
        val cx = canvas.width * 0.5f
        val cy = canvas.height * 0.46f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = base * 0.006f
        paint.color = withAlpha(scene.palette.secondary, 0.45f)
        canvas.drawCircle(cx, cy, orbitRadius, paint)
        paint.style = Paint.Style.FILL
        val px = cx + cos(angle.toDouble()).toFloat() * orbitRadius
        val py = cy + sin(angle.toDouble()).toFloat() * orbitRadius
        paint.color = scene.palette.accent
        canvas.drawCircle(cx, cy, base * 0.06f, paint)
        paint.color = withAlpha(scene.palette.secondary, 0.95f)
        canvas.drawCircle(px.toFloat(), py.toFloat(), base * 0.025f, paint)
    }

    private fun drawNature(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        fillGradient(canvas, scene.palette.primary, scene.palette.secondary, vertical = true)
        val base = min(canvas.width, canvas.height).toFloat()
        val moonX = canvas.width * 0.72f + sin(time * 0.15f).toFloat() * base * 0.02f * motion
        val moonY = canvas.height * 0.25f
        paint.color = withAlpha(scene.palette.accent, 0.16f + intensity * 0.08f)
        canvas.drawCircle(moonX, moonY, base * 0.12f, paint)
        paint.color = withAlpha(Color.BLACK, 0.22f)
        canvas.drawCircle(moonX + base * 0.035f, moonY - base * 0.01f, base * 0.12f, paint)
        val seed = scene.id.hashCode()
        repeat(24) { i ->
            val x = WallpaperRenderMath.seedUnit(seed + i * 17) * canvas.width
            val y = canvas.height * (0.48f + WallpaperRenderMath.seedUnit(seed + i * 31) * 0.40f)
            val drift = sin(time * 0.18f + i).toFloat() * base * 0.02f * motion
            paint.color = withAlpha(scene.palette.accent, 0.20f + 0.45f * WallpaperRenderMath.seedUnit(seed + i * 23))
            canvas.drawCircle(x + drift, y, base * 0.006f, paint)
        }
        paint.color = withAlpha(scene.palette.secondary, 0.35f)
        repeat(5) { i ->
            val x = canvas.width * (0.05f + i * 0.23f)
            val sway = sin(time * 0.12f + i).toFloat() * base * 0.035f * motion
            canvas.drawRoundRect(
                RectF(x, canvas.height * 0.62f, x + base * 0.018f, canvas.height.toFloat()),
                base * 0.01f,
                base * 0.01f,
                paint
            )
            canvas.drawCircle(x + sway, canvas.height * 0.62f, base * 0.045f, paint)
        }
    }

    private fun drawAmoled(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        canvas.drawColor(Color.BLACK)
        val base = min(canvas.width, canvas.height).toFloat()
        val phase = time * 0.05f
        for (i in 0..3) {
            val x = canvas.width * (0.15f + i * 0.28f) + sin(phase + i).toFloat() * base * 0.05f * motion
            val y = canvas.height * (0.30f + (i % 2) * 0.34f)
            paint.shader = RadialGradient(
                x,
                y,
                base * (0.24f + intensity * 0.06f),
                scene.palette.accent,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            paint.alpha = (150 * intensity.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, base * 0.24f, paint)
            paint.alpha = 255
        }
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = base * 0.007f
        paint.color = withAlpha(scene.palette.accent, 0.55f)
        val radius = base * 0.20f
        val rotation = WallpaperRenderMath.rotationRadians(time, 0.12f, 18f) * motion
        canvas.save()
        canvas.rotate(Math.toDegrees(rotation.toDouble()).toFloat(), canvas.width / 2f, canvas.height / 2f)
        canvas.drawArc(
            RectF(canvas.width / 2f - radius, canvas.height / 2f - radius, canvas.width / 2f + radius, canvas.height / 2f + radius),
            20f,
            285f,
            false,
            paint
        )
        canvas.restore()
        paint.style = Paint.Style.FILL
    }

    private fun drawParticleFlow(canvas: Canvas, scene: WallpaperScene, time: Float, intensity: Float, motion: Float) {
        fillGradient(canvas, scene.palette.primary, scene.palette.secondary, vertical = false)
        val base = min(canvas.width, canvas.height).toFloat()
        val seed = scene.id.hashCode()
        val count = if (motion == 0f) 18 else if (scene.batteryProfile == BatteryProfile.ACTIVE) 95 else 65
        repeat(count) { i ->
            val sx = WallpaperRenderMath.seedUnit(seed + i * 17)
            val sy = WallpaperRenderMath.seedUnit(seed + i * 29)
            val speed = 0.04f + WallpaperRenderMath.seedUnit(seed + i * 43) * 0.08f
            val x = ((sx + time * speed * motion) % 1f) * canvas.width
            val y = ((sy + sin(time * 0.25f + i * 0.37f).toFloat() * 0.05f * motion + 1f) % 1f) * canvas.height
            val radius = base * (0.0025f + WallpaperRenderMath.seedUnit(seed + i * 59) * 0.004f) * (0.7f + intensity * 0.5f)
            paint.color = withAlpha(scene.palette.accent, 0.18f + WallpaperRenderMath.seedUnit(seed + i * 71) * 0.70f)
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    private fun fillGradient(canvas: Canvas, start: Int, end: Int, vertical: Boolean) {
        paint.shader = if (vertical) {
            LinearGradient(0f, 0f, 0f, canvas.height.toFloat(), start, end, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), start, end, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
        paint.shader = null
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        return Color.argb(
            (alpha.coerceIn(0f, 1f) * 255f).toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
