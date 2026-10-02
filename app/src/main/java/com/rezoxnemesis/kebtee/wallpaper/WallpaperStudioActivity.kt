package com.rezoxnemesis.kebtee.wallpaper

import android.app.WallpaperManager
import android.graphics.Bitmap
import com.rezoxnemesis.kebtee.R
import android.graphics.Canvas as AndroidCanvas
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.rezoxnemesis.kebtee.ui.designsystem.KebTeeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.os.SystemClock

class WallpaperStudioActivity : ComponentActivity() {
    private val preferences by lazy { WallpaperPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KebTeeTheme {
                WallpaperStudioScreen(
                    preferences = preferences,
                    onBack = { finish() },
                    onApply = { scene -> applyWallpaper(scene) }
                )
            }
        }
    }

    private fun applyWallpaper(scene: WallpaperScene) {
        preferences.setSelectedScene(scene.id)
        preferences.markApplied(scene.id)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@WallpaperStudioActivity, KebTeeLiveWallpaperService::class.java)
            )
        }
        runCatching { startActivity(intent) }
            .onFailure { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
    }
}

@Composable
private fun WallpaperStudioScreen(
    preferences: WallpaperPreferences,
    onBack: () -> Unit,
    onApply: (WallpaperScene) -> Unit
) {
    var selected by remember {
        mutableStateOf(WallpaperCatalog.byId(preferences.selectedSceneId()) ?: WallpaperCatalog.scenes.first())
    }
    var selectedCategory by remember { mutableStateOf<WallpaperCategory?>(null) }
    var speed by remember { mutableFloatStateOf(preferences.settingsFor(selected.id).speed) }
    var intensity by remember { mutableFloatStateOf(preferences.settingsFor(selected.id).intensity) }
    var batteryMode by remember { mutableStateOf(preferences.settingsFor(selected.id).batteryMode) }
    var reducedMotion by remember { mutableStateOf(preferences.settingsFor(selected.id).reducedMotion) }

    val filtered = remember(selectedCategory) {
        if (selectedCategory == null) WallpaperCatalog.scenes
        else WallpaperCatalog.scenes.filter { it.category == selectedCategory }
    }

    androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(primary = Color(0xFF22D3EE))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF070B18)) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Wallpaper Studio", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${WallpaperCatalog.scenes.size} built-in live scenes", color = Color(0xFF8FA1C3), fontSize = 11.sp)
                    }
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF6EE7B7))
                }

                Spacer(Modifier.height(12.dp))
                AnimatedWallpaperPreview(
                    scene = selected,
                    speed = speed,
                    intensity = intensity,
                    batteryMode = batteryMode,
                    reducedMotion = reducedMotion
                )

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingPill("Speed ${"%.1f".format(speed)}x")
                    SettingPill("Intensity ${"%.0f".format(intensity * 100)}%")
                    if (batteryMode) SettingPill("Battery")
                    if (reducedMotion) SettingPill("Reduced")
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                    WallpaperCategory.values().forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { scene ->
                        val favorite = preferences.isFavorite(scene.id)
                        Card(
                            onClick = {
                                selected = scene
                                val restored = preferences.settingsFor(scene.id)
                                speed = restored.speed
                                intensity = restored.intensity
                                batteryMode = restored.batteryMode
                                reducedMotion = restored.reducedMotion
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121A2D)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column {
                                StaticWallpaperPreview(scene, Modifier.fillMaxWidth().height(126.dp))
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(scene.title, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = { preferences.setFavorite(scene.id, !favorite) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = if (favorite) Color(0xFFF472B6) else Color(0xFF8190AE)
                                            )
                                        }
                                    }
                                    Text(
                                        buildString {
                                            append(scene.category.name.lowercase().replace('_', ' '))
                                            if (scene.amoledFriendly) append(" • AMOLED")
                                        },
                                        color = Color(0xFF8FA1C3),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10182B))) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text("Selected • ${selected.title}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Speed", color = Color(0xFF8FA1C3), fontSize = 11.sp)
                        Slider(value = speed, onValueChange = {
                            speed = it
                            preferences.saveSettings(selected.id, preferences.settingsFor(selected.id).copy(speed = it))
                        }, valueRange = 0.15f..2f)
                        Text("Intensity", color = Color(0xFF8FA1C3), fontSize = 11.sp)
                        Slider(value = intensity, onValueChange = {
                            intensity = it
                            preferences.saveSettings(selected.id, preferences.settingsFor(selected.id).copy(intensity = it))
                        }, valueRange = 0f..1.25f)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    batteryMode = !batteryMode
                                    preferences.saveSettings(selected.id, preferences.settingsFor(selected.id).copy(batteryMode = batteryMode))
                                }
                            ) {
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(if (batteryMode) "Battery mode on" else "Battery mode")
                            }
                            Button(
                                onClick = {
                                    reducedMotion = !reducedMotion
                                    preferences.saveSettings(selected.id, preferences.settingsFor(selected.id).copy(reducedMotion = reducedMotion))
                                }
                            ) {
                                Icon(if (reducedMotion) Icons.Default.PauseCircleOutline else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(if (reducedMotion) "Motion off" else "Motion on")
                            }
                        }
                        Button(
                            onClick = { onApply(selected) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Apply ${selected.title}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingPill(text: String) {
    Surface(color = Color(0xFF182440), shape = RoundedCornerShape(50)) {
        Text(text, color = Color(0xFFB9C7E5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
    }
}

@Composable
private fun StaticWallpaperPreview(
    scene: WallpaperScene,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val silhouette = remember(context) { loadKebTeeSilhouette(context) }
    val bitmap = remember(scene.id, silhouette) {
        Bitmap.createBitmap(720, 480, Bitmap.Config.ARGB_8888).also {
            WallpaperRenderer(silhouette).draw(
                AndroidCanvas(it),
                scene,
                WallpaperRenderState(
                    width = 720,
                    height = 480,
                    timeSeconds = 4f,
                    speed = 1f,
                    intensity = 1f
                )
            )
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = scene.title,
        modifier = modifier.background(Color.Black),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun AnimatedWallpaperPreview(
    scene: WallpaperScene,
    speed: Float,
    intensity: Float,
    batteryMode: Boolean,
    reducedMotion: Boolean
) {
    val context = LocalContext.current
    val silhouette = remember(context) { BitmapFactory.decodeResource(context.resources, R.drawable.kebtee_silhouette) }
    val bitmap = remember(scene.id) {
        Bitmap.createBitmap(720, 405, Bitmap.Config.ARGB_8888)
    }
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(scene.id, speed, intensity, batteryMode, reducedMotion, silhouette) {
        val renderer = WallpaperRenderer(silhouette)
        val startedAt = SystemClock.uptimeMillis()
        while (isActive) {
            val elapsed = (SystemClock.uptimeMillis() - startedAt) / 1000f
            renderer.draw(
                AndroidCanvas(bitmap),
                scene,
                WallpaperRenderState(
                    width = bitmap.width,
                    height = bitmap.height,
                    timeSeconds = elapsed,
                    speed = speed,
                    intensity = intensity,
                    reducedMotion = reducedMotion,
                    batteryMode = batteryMode
                )
            )
            tick.intValue++
            if (reducedMotion) break
            delay(if (batteryMode) 66L else 50L)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(24.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = scene.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentScale = ContentScale.Crop
        )
    }
}
