package com.rezoxnemesis.kebtee

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush as UiBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezoxnemesis.kebtee.wallpaper.KebTeeLiveWallpaperService

private val Night = Color(0xFF070B18)
private val Panel = Color(0xFF11182B)
private val Violet = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF22D3EE)
private val Pink = Color(0xFFEC4899)
private val Green = Color(0xFF6EE7B7)
private val Muted = Color(0xFF9EABC9)

internal fun accentFromPreference(value: String?): Color = when (value) {
    "cyan" -> Cyan
    "pink" -> Pink
    "green" -> Green
    else -> Violet
}

internal fun preferenceForAccent(color: Color): String = when (color) {
    Cyan -> "cyan"
    Pink -> "pink"
    Green -> "green"
    else -> "violet"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 24)
        val preferences = getSharedPreferences("kebtee_preferences", MODE_PRIVATE)
        setContent {
            var accent by remember { mutableStateOf(accentFromPreference(preferences.getString("accent", "violet"))) }
            var reduceMotion by remember { mutableStateOf(preferences.getBoolean("reduce_motion", false)) }
            MaterialTheme(
                colorScheme = darkColorScheme(primary = accent, secondary = Cyan, tertiary = Pink, background = Night, surface = Panel)
            ) {
                KebTeeHome(
                    accent = accent,
                    reduceMotion = reduceMotion,
                    onReduceMotionChange = {
                        reduceMotion = it
                        preferences.edit().putBoolean("reduce_motion", it).apply()
                    },
                    onAccentChange = {
                        accent = it
                        preferences.edit().putString("accent", preferenceForAccent(it)).apply()
                    },
                    onLiveWallpaper = { openWallpaperPicker() },
                    onOpenSystemSettings = { action -> openSystemSettings(action) }
                )
            }
        }
    }

    private fun openWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, KebTeeLiveWallpaperService::class.java))
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            }
        }
    }

    private fun openSystemSettings(action: String) {
        val intent = when (action) {
            "home" -> Intent(Settings.ACTION_HOME_SETTINGS)
            "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

private enum class FeatureAction { WALLPAPER, THEME, HOME, VOLUME, NOTIFICATIONS, MOTION }
private data class Feature(val title: String, val subtitle: String, val icon: ImageVector, val accent: Color, val action: FeatureAction)

@Composable
private fun KebTeeHome(
    accent: Color,
    reduceMotion: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    onAccentChange: (Color) -> Unit,
    onLiveWallpaper: () -> Unit,
    onOpenSystemSettings: (String) -> Unit
) {
    var selectedFeature by remember { mutableStateOf<Feature?>(null) }
    val features = listOf(
        Feature("Live Wallpaper", "Animated scenes for your screen", Icons.Default.Wallpaper, Cyan, FeatureAction.WALLPAPER),
        Feature("Theme Studio", "Choose your KebTee accent", Icons.Default.Palette, Violet, FeatureAction.THEME),
        Feature("Home Experience", "Open your home-screen settings", Icons.Default.Home, Pink, FeatureAction.HOME),
        Feature("Volume Lab", "Jump to Android sound controls", Icons.Default.Tune, Cyan, FeatureAction.VOLUME),
        Feature("Notifications", "Manage KebTee notifications", Icons.Default.Notifications, Violet, FeatureAction.NOTIFICATIONS),
        Feature("Effects & Motion", "Set a lighter visual experience", Icons.Default.Bolt, Pink, FeatureAction.MOTION)
    )

    Surface(Modifier.fillMaxSize(), color = Night) {
        Column(
            Modifier.fillMaxSize()
                .background(UiBrush.verticalGradient(listOf(Color(0xFF10172D), Night, Color(0xFF090D1C))))
                .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.rezoxnemesis.kebtee.R.drawable.ic_kebtee_mark),
                    contentDescription = "KebTee logo",
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.width(13.dp))
                Column {
                    Text("KebTee", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("MAKE EVERY PIXEL YOURS", fontSize = 10.sp, letterSpacing = 1.8.sp, color = Cyan)
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color(0xFF14253A), border = BorderStroke(1.dp, Color(0xFF29425B))) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Green, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("BETA", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Your phone.\nYour atmosphere.", fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Personalize the screen, then make it work for you.", fontSize = 14.sp, color = Color(0xFFADB8D1), modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(28.dp), color = Panel,
                border = BorderStroke(1.dp, Color(0xFF293451)), modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    Box(Modifier.matchParentSize().background(
                        UiBrush.linearGradient(listOf(Color(0xFF263C68), Color(0xFF211A45), Color(0xFF11182B)))
                    ))
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(
                                UiBrush.linearGradient(listOf(Cyan.copy(alpha = 0.25f), Violet.copy(alpha = 0.35f))),
                                RoundedCornerShape(16.dp)
                            ), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Cyan, modifier = Modifier.size(27.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AURORA / 001", fontSize = 10.sp, letterSpacing = 2.sp, color = Cyan, fontWeight = FontWeight.Bold)
                                Text("KebTee Aurora", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Live wallpaper • battery-aware", fontSize = 12.sp, color = Color(0xFFBBC6E0))
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        AuroraPreview(accent, reduceMotion)
                        Spacer(Modifier.height(15.dp))
                        Text("A custom animated aurora that pauses drawing when it isn't visible.", fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFD8E0F5))
                        Spacer(Modifier.height(17.dp))
                        Button(
                            onClick = onLiveWallpaper, modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null)
                            Spacer(Modifier.width(9.dp))
                            Text("Preview & Apply Wallpaper", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CUSTOMIZATION LABS", fontSize = 11.sp, letterSpacing = 1.8.sp, color = Color(0xFF98A6C8), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("6 TOOLS", fontSize = 10.sp, color = Muted)
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 22.dp)
            ) {
                items(features) { feature ->
                    FeatureCard(feature) {
                        if (feature.action == FeatureAction.WALLPAPER) onLiveWallpaper() else selectedFeature = feature
                    }
                }
            }
        }
    }

    selectedFeature?.let { feature ->
        FeatureDialog(
            feature, accent, reduceMotion, onReduceMotionChange, onAccentChange, onOpenSystemSettings,
            { selectedFeature = null }
        )
    }
}

@Composable
private fun AuroraPreview(accent: Color, reduceMotion: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(104.dp).background(Color(0xFF080D1B), RoundedCornerShape(18.dp)).padding(1.dp)
    ) {
        Box(Modifier.fillMaxSize().background(
            UiBrush.verticalGradient(listOf(Color(0xFF101B39), Color(0xFF080D1B))), RoundedCornerShape(17.dp)
        ))
        if (!reduceMotion) {
            Box(Modifier.fillMaxWidth(0.78f).height(65.dp).align(Alignment.Center)
                .background(UiBrush.horizontalGradient(listOf(accent.copy(alpha = 0.04f), accent.copy(alpha = 0.48f), Cyan.copy(alpha = 0.25f), Pink.copy(alpha = 0.10f))), RoundedCornerShape(50)))
        }
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.55f).height(2.dp)
            .background(UiBrush.horizontalGradient(listOf(Color.Transparent, Cyan, accent, Color.Transparent))))
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(7) { index ->
                Box(Modifier.size(if (index == 3) 5.dp else 3.dp).background(if (index % 2 == 0) Cyan.copy(alpha = 0.7f) else accent.copy(alpha = 0.75f), CircleShape))
            }
        }
        Text(if (reduceMotion) "REDUCED MOTION" else "AURORA VISUAL PREVIEW", Modifier.align(Alignment.TopStart).padding(10.dp), color = Color(0xFFB8C8E9), fontSize = 8.sp, letterSpacing = 1.4.sp)
    }
}

@Composable
private fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().height(142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp), color = Panel, border = BorderStroke(1.dp, Color(0xFF283451))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).background(feature.accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(feature.icon, contentDescription = null, tint = feature.accent)
            }
            Column {
                Text(feature.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(feature.subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun FeatureDialog(
    feature: Feature,
    selectedAccent: Color,
    reduceMotion: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    onAccentChange: (Color) -> Unit,
    onOpenSystemSettings: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF121A2D),
        titleContentColor = Color.White, textContentColor = Color(0xFFB7C3DD),
        title = { Text(feature.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (feature.action) {
                    FeatureAction.THEME -> {
                        Text("Choose the accent used across KebTee. Your choice updates the interface immediately.")
                        listOf(Violet to "Ultraviolet", Cyan to "Cyber cyan", Pink to "Pulse pink", Green to "Mint circuit").forEach { (color, label) ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { onAccentChange(color) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedAccent == color) color.copy(alpha = 0.18f) else Color(0xFF1A2439),
                                border = BorderStroke(1.dp, if (selectedAccent == color) color else Color(0xFF2B3854))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(20.dp).background(color, CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Text(label, Modifier.weight(1f), color = Color.White)
                                    if (selectedAccent == color) Icon(Icons.Default.Check, contentDescription = "Selected", tint = color)
                                }
                            }
                        }
                    }
                    FeatureAction.HOME -> {
                        Text("Android controls which launcher is active. Open system Home settings to choose your default launcher or configure your current one.")
                        Button(onClick = { onOpenSystemSettings("home"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Open Home settings") }
                    }
                    FeatureAction.VOLUME -> {
                        Text("Android keeps the system volume panel under OS control. Use system sound settings for media, calls, alarms and accessibility volume.")
                        Button(onClick = { onOpenSystemSettings("sound"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Open sound settings") }
                    }
                    FeatureAction.NOTIFICATIONS -> {
                        Text("Notification appearance and permission are managed by Android. Open KebTee's app notification settings to review the available controls.")
                        Button(onClick = { onOpenSystemSettings("notifications"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Open notification settings") }
                    }
                    FeatureAction.MOTION -> {
                        Text("Reduce visual motion in KebTee's preview. The live wallpaper service also pauses drawing when it isn't visible.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reduced-motion preview", Modifier.weight(1f), color = Color.White)
                            Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange)
                        }
                    }
                    FeatureAction.WALLPAPER -> Text("Open the Android live wallpaper picker.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Cyan) } }
    )
}
