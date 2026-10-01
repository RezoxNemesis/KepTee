package com.rezoxnemesis.kebtee

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.rezoxnemesis.kebtee.wallpaper.KebTeeLiveWallpaperService
import com.rezoxnemesis.kebtee.wallpaper.WallpaperStudioActivity

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
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        openSystemSettings("notifications")
    }
    private val testNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showTestNotification()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 24)
        val preferences = getSharedPreferences("kebtee_preferences", MODE_PRIVATE)
        val audioManager = getSystemService(AudioManager::class.java)
        setContent {
            var accent by remember { mutableStateOf(accentFromPreference(preferences.getString("accent", "violet"))) }
            var reduceMotion by remember { mutableStateOf(preferences.getBoolean("reduce_motion", false)) }
            MaterialTheme(
                colorScheme = darkColorScheme(primary = accent, secondary = Cyan, tertiary = Pink, background = Night, surface = Panel)
            ) {
                KebTeeHome(
                    accent = accent,
                    reduceMotion = reduceMotion,
                    audioManager = audioManager,
                    onReduceMotionChange = {
                        reduceMotion = it
                        preferences.edit().putBoolean("reduce_motion", it).apply()
                    },
                    onAccentChange = {
                        accent = it
                        preferences.edit().putString("accent", preferenceForAccent(it)).apply()
                    },
                    onLiveWallpaper = { startActivity(Intent(this@MainActivity, WallpaperStudioActivity::class.java)) },
                    onTestNotification = { showTestNotification() },
                    onOpenSystemSettings = { action ->
                        if (action == "notifications") openNotificationControls() else openSystemSettings(action)
                    }
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

    private fun openNotificationControls() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else openSystemSettings("notifications")
    }

    private fun showTestNotification() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            testNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val notification = android.app.Notification.Builder(this, "kebtee_updates")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("KebTee notification test")
            .setContentText("Notifications are working on this device.")
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(1001, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("kebtee_updates", "KebTee updates", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Optional updates from KebTee." }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun openSystemSettings(action: String) {
        val intent = when (action) {
            "home" -> Intent(Settings.ACTION_HOME_SETTINGS)
            "sound" -> if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_VOLUME) else Intent(Settings.ACTION_SOUND_SETTINGS)
            "internet" -> if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_WIFI_SETTINGS)
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

private enum class FeatureAction { WALLPAPER, THEME, HOME, VOLUME, NOTIFICATIONS, MOTION, CONTROL_CENTER }
private data class Feature(val title: String, val subtitle: String, val icon: ImageVector, val accent: Color, val action: FeatureAction)

@Composable
private fun KebTeeHome(
    accent: Color,
    reduceMotion: Boolean,
    audioManager: AudioManager,
    onReduceMotionChange: (Boolean) -> Unit,
    onAccentChange: (Color) -> Unit,
    onLiveWallpaper: () -> Unit,
    onTestNotification: () -> Unit,
    onOpenSystemSettings: (String) -> Unit
) {
    var selectedFeature by remember { mutableStateOf<Feature?>(null) }
    val features = listOf(
        Feature("Live Wallpaper", "Animated scenes for your screen", Icons.Default.Wallpaper, Cyan, FeatureAction.WALLPAPER),
        Feature("Theme Studio", "Choose your KebTee accent", Icons.Default.Palette, Violet, FeatureAction.THEME),
        Feature("Home Experience", "Open your home-screen settings", Icons.Default.Home, Pink, FeatureAction.HOME),
        Feature("Volume Lab", "Custom in-app audio sliders", Icons.Default.Tune, Cyan, FeatureAction.VOLUME),
        Feature("Notifications", "Manage KebTee notifications", Icons.Default.Notifications, Violet, FeatureAction.NOTIFICATIONS),
        Feature("Effects & Motion", "Set a lighter visual experience", Icons.Default.Bolt, Pink, FeatureAction.MOTION),
        Feature("Control Center", "Fast access to system panels", Icons.Default.Tune, Green, FeatureAction.CONTROL_CENTER)
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
                                Text("MONOCHROME / 001", fontSize = 10.sp, letterSpacing = 2.sp, color = Cyan, fontWeight = FontWeight.Bold)
                                Text("KebTee Silhouette", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Live wallpaper • battery-aware", fontSize = 12.sp, color = Color(0xFFBBC6E0))
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        SilhouettePreview(reduceMotion)
                        Spacer(Modifier.height(15.dp))
                        Text("The exact monochrome reference artwork, animated with a subtle continuous breathing pulse.", fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFD8E0F5))
                        Spacer(Modifier.height(17.dp))
                        Button(
                            onClick = onLiveWallpaper, modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null)
                            Spacer(Modifier.width(9.dp))
                            Text("Choose & Apply Wallpaper", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CUSTOMIZATION LABS", fontSize = 11.sp, letterSpacing = 1.8.sp, color = Color(0xFF98A6C8), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("7 TOOLS", fontSize = 10.sp, color = Muted)
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
            feature, accent, reduceMotion, audioManager, onReduceMotionChange, onAccentChange, onOpenSystemSettings, onTestNotification,
            { selectedFeature = null }
        )
    }
}

@Composable
private fun SilhouettePreview(reduceMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "silhouette-preview")
    val animatedScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "silhouette-scale"
    )
    val animatedAlpha by transition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "silhouette-glow"
    )
    Box(
        Modifier.fillMaxWidth().height(144.dp)
            .background(Color.Black, RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.kebtee_silhouette),
            contentDescription = "KebTee monochrome silhouette reference wallpaper",
            modifier = Modifier.align(Alignment.Center).fillMaxHeight().graphicsLayer(
                scaleX = if (reduceMotion) 1f else animatedScale,
                scaleY = if (reduceMotion) 1f else animatedScale
            ),
            contentScale = ContentScale.Fit,
            alpha = if (reduceMotion) 1f else animatedAlpha
        )
        if (reduceMotion) {
            Text(
                "REDUCED MOTION",
                Modifier.align(Alignment.TopStart).padding(10.dp),
                color = Color(0xFFB8C8E9), fontSize = 8.sp, letterSpacing = 1.4.sp
            )
        }
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
    audioManager: AudioManager,
    onReduceMotionChange: (Boolean) -> Unit,
    onAccentChange: (Color) -> Unit,
    onOpenSystemSettings: (String) -> Unit,
    onTestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF121A2D),
        titleContentColor = Color.White, textContentColor = Color(0xFFB7C3DD),
        title = { Text(feature.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        Text(
                            "Adjust audio streams directly from KebTee. Android may link ring and notification volume on some devices; the system volume popup itself remains controlled by Android.",
                            color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        listOf(
                            AudioManager.STREAM_MUSIC to "Media",
                            AudioManager.STREAM_RING to "Ringtone",
                            AudioManager.STREAM_NOTIFICATION to "Notifications",
                            AudioManager.STREAM_ALARM to "Alarm",
                            AudioManager.STREAM_SYSTEM to "System sounds"
                        ).forEach { (stream, label) ->
                            val maxVolume = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
                            var level by remember(stream) {
                                mutableFloatStateOf(audioManager.getStreamVolume(stream).coerceIn(0, maxVolume).toFloat())
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("${level.roundToInt()} / $maxVolume", color = Cyan, fontSize = 11.sp)
                                }
                                Slider(
                                    value = level,
                                    onValueChange = { newLevel ->
                                        level = newLevel
                                        try {
                                            audioManager.setStreamVolume(stream, newLevel.roundToInt(), 0)
                                        } catch (_: SecurityException) {
                                            // OEM restrictions may prevent changing a particular stream.
                                        } catch (_: IllegalArgumentException) {
                                            // Some Android builds do not expose every stream equally.
                                        }
                                    },
                                    valueRange = 0f..maxVolume.toFloat(),
                                    steps = (maxVolume - 1).coerceAtLeast(0),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Text("Note: some Android versions merge ring and notification volume.", color = Muted, fontSize = 11.sp)
                    }
                    FeatureAction.NOTIFICATIONS -> {
                        Text("Send a real test notification to verify KebTee's notification channel, or open Android settings to adjust permission and alerts.")
                        Button(
                            onClick = { onTestNotification(); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Send test notification") }
                        OutlinedButton(
                            onClick = { onOpenSystemSettings("notifications"); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Notification settings") }
                    }
                    FeatureAction.MOTION -> {
                        Text("Reduce visual motion in KebTee's preview. The live wallpaper service also pauses drawing when it isn't visible.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reduced-motion preview", Modifier.weight(1f), color = Color.White)
                            Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange)
                        }
                    }
                    FeatureAction.CONTROL_CENTER -> {
                        Text(
                            "A KebTee quick-access hub for Android's native control panels. These shortcuts open the real system controls; Android still owns the notification shade and Quick Settings.",
                            color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Button(onClick = { onOpenSystemSettings("internet") }, modifier = Modifier.fillMaxWidth()) { Text("Internet & Wi-Fi") }
                        OutlinedButton(onClick = { onOpenSystemSettings("bluetooth") }, modifier = Modifier.fillMaxWidth()) { Text("Bluetooth") }
                        OutlinedButton(onClick = { onOpenSystemSettings("brightness") }, modifier = Modifier.fillMaxWidth()) { Text("Brightness & display") }
                        OutlinedButton(onClick = { onOpenSystemSettings("sound") }, modifier = Modifier.fillMaxWidth()) { Text("System volume panel") }
                        OutlinedButton(onClick = { onOpenSystemSettings("notifications") }, modifier = Modifier.fillMaxWidth()) { Text("Notification controls") }
                    }
                    FeatureAction.WALLPAPER -> Text("Open the Android live wallpaper picker.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Cyan) } }
    )
}
