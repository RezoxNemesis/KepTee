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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Settings
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
import com.rezoxnemesis.kebtee.notifications.NotificationFeedStore
import com.rezoxnemesis.kebtee.themes.ThemePresets
import androidx.compose.ui.platform.LocalContext

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
            .setContentTitle("KepTee notification test")
            .setContentText("KepTee notifications are working on this device.")
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(1001, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("kebtee_updates", "KepTee updates", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Optional updates from KepTee." }
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
            "notification_listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
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
    var activeTab by remember { mutableStateOf("Home") }
    val features = listOf(
        Feature("Themes", "Color and style", Icons.Default.Palette, Violet, FeatureAction.THEME),
        Feature("Icons", "Personalize app look", Icons.Default.Brush, Cyan, FeatureAction.THEME),
        Feature("Volume Lab", "Tune audio streams", Icons.Default.Tune, Pink, FeatureAction.VOLUME),
        Feature("Motion", "Smooth or reduce effects", Icons.Default.Bolt, Green, FeatureAction.MOTION),
        Feature("Notifications", "Alerts and permissions", Icons.Default.Notifications, Violet, FeatureAction.NOTIFICATIONS),
        Feature("Home launcher", "Choose your home app", Icons.Default.Home, Cyan, FeatureAction.HOME),
        Feature("System tools", "Wi-Fi, display and sound", Icons.Default.Tune, Pink, FeatureAction.CONTROL_CENTER)
    )

    Surface(Modifier.fillMaxSize(), color = Night) {
        Column(
            Modifier.fillMaxSize()
                .background(UiBrush.verticalGradient(listOf(Color(0xFF0B1430), Night, Color(0xFF050812))))
                .statusBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_kebtee_mark),
                    contentDescription = "KepTee logo",
                    modifier = Modifier.size(46.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("KepTee", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("LIVE BEYOND ORDINARY", fontSize = 9.sp, letterSpacing = 1.5.sp, color = Cyan)
                }
                Surface(shape = CircleShape, color = Color(0xFF12233B), border = BorderStroke(1.dp, Color(0xFF29435F))) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Green, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("READY", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                    }
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Text("Your screen.\nYour atmosphere.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "A calmer space for wallpapers, themes and everyday controls.",
                    fontSize = 13.sp, lineHeight = 19.sp, color = Muted,
                    modifier = Modifier.padding(top = 7.dp, bottom = 18.dp)
                )

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Panel,
                    border = BorderStroke(1.dp, Color(0xFF263B60)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(186.dp)) {
                            Canvas(Modifier.matchParentSize()) {
                                drawRect(UiBrush.verticalGradient(listOf(Color(0xFF092B60), Color(0xFF101B43), Color(0xFF080E22))))
                                drawCircle(
                                    brush = UiBrush.radialGradient(listOf(Color(0x5522D3EE), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.36f), radius = size.width * 0.28f),
                                    radius = size.width * 0.28f,
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.36f)
                                )
                                drawCircle(Color(0xFFB7E8FF), radius = size.width * 0.065f, center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.36f))
                                val far = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.78f)
                                    lineTo(size.width * 0.18f, size.height * 0.44f)
                                    lineTo(size.width * 0.32f, size.height * 0.68f)
                                    lineTo(size.width * 0.51f, size.height * 0.35f)
                                    lineTo(size.width * 0.77f, size.height * 0.77f)
                                    lineTo(size.width, size.height * 0.51f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(far, Color(0xFF174A8B))
                                val near = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.86f)
                                    lineTo(size.width * 0.26f, size.height * 0.62f)
                                    lineTo(size.width * 0.45f, size.height * 0.83f)
                                    lineTo(size.width * 0.72f, size.height * 0.58f)
                                    lineTo(size.width, size.height * 0.82f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(near, Color(0xFF07152F))
                            }
                            Surface(
                                Modifier.align(Alignment.TopStart).padding(14.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xCC07162C),
                                border = BorderStroke(1.dp, Color(0x7738C8FF))
                            ) {
                                Text("FEATURED  /  LIVE WALLPAPER", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                            }
                            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Text("Cinematic night", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text("A living sky for your home screen", color = Color(0xFFD1DDF5), fontSize = 11.sp)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("KepTee Live", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Subtle motion · AMOLED friendly", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            Button(
                                onClick = onLiveWallpaper,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Make it yours", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Your tools, neatly in one place.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Text("7 TOOLS", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                features.chunked(2).forEachIndexed { rowIndex, rowFeatures ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowFeatures.forEach { feature ->
                            MinimalFeatureCard(
                                feature = feature,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (feature.action == FeatureAction.WALLPAPER) onLiveWallpaper() else selectedFeature = feature
                                }
                            )
                        }
                        if (rowFeatures.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                Surface(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
                    color = Color(0xFF0D1527),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFF202D47))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(Color(0x1F22D3EE), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Cyan)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Built around your device", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("System actions open Android's own controls when required.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }

            NavigationBar(
                containerColor = Color(0xFF080D1A),
                contentColor = Muted,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding().height(66.dp)
            ) {
                val navItems = listOf(
                    Triple("Home", Icons.Default.Home, "Home"),
                    Triple("Wallpapers", Icons.Default.Wallpaper, "Wallpapers"),
                    Triple("Customize", Icons.Default.Palette, "Customize"),
                    Triple("Settings", Icons.Default.Tune, "Settings")
                )
                navItems.forEach { (label, icon, route) ->
                    NavigationBarItem(
                        selected = activeTab == route,
                        onClick = {
                            activeTab = route
                            when (route) {
                                "Wallpapers" -> onLiveWallpaper()
                                "Customize" -> selectedFeature = features.first()
                                "Settings" -> selectedFeature = features.last()
                            }
                        },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp)) },
                        label = { Text(label, fontSize = 9.sp) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cyan,
                            selectedTextColor = Cyan,
                            indicatorColor = Color(0xFF102843),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    }

    selectedFeature?.let { feature ->
        FeatureDialog(
            feature, accent, reduceMotion, audioManager, onReduceMotionChange, onAccentChange,
            onOpenSystemSettings, onTestNotification, { selectedFeature = null }
        )
    }
}

@Composable
private fun MinimalFeatureCard(feature: Feature, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = Color(0xFF10182A),
        border = BorderStroke(1.dp, Color(0xFF25334F))
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(39.dp).background(feature.accent.copy(alpha = 0.13f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, contentDescription = null, tint = feature.accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(feature.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(feature.subtitle, color = Muted, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
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
            contentDescription = "KepTee monochrome silhouette reference wallpaper",
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
                        Text("Choose a reusable visual preset. Applying a preset changes the live accent immediately and stores the choice locally.")
                        ThemePresets.builtIns.forEach { preset ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { onAccentChange(preset.accent) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedAccent == preset.accent) preset.accent.copy(alpha = 0.18f) else Color(0xFF1A2439),
                                border = BorderStroke(1.dp, if (selectedAccent == preset.accent) preset.accent else Color(0xFF2B3854))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(20.dp).background(preset.accent, CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(preset.name, color = Color.White, fontWeight = FontWeight.Medium)
                                        Text("Motion profile ${(preset.motionIntensity * 100).roundToInt()}%", color = Muted, fontSize = 10.sp)
                                    }
                                    if (selectedAccent == preset.accent) Icon(Icons.Default.Check, contentDescription = "Selected", tint = preset.accent)
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
                            "Adjust audio streams directly from KepTee. Android may link ring and notification volume on some devices; the system volume popup itself remains controlled by Android.",
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
                        val context = LocalContext.current
                        val feed = remember(feature.action) { NotificationFeedStore.list(context).take(8) }
                        Text(
                            "KepTee can send its own notifications or, after explicit Android Notification Access permission, show a local feed of recent notifications. The system notification shade remains owned by Android.",
                            color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Button(onClick = { onTestNotification(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Send test notification")
                        }
                        OutlinedButton(onClick = { onOpenSystemSettings("notifications"); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                            Text("App notification settings")
                        }
                        OutlinedButton(onClick = { onOpenSystemSettings("notification_listener"); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Enable notification access")
                        }
                        if (feed.isNotEmpty()) {
                            Text("Recent notifications", color = Color.White, fontWeight = FontWeight.SemiBold)
                            feed.forEach { item ->
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1A2439),
                                    border = BorderStroke(1.dp, Color(0xFF2B3854))
                                ) {
                                    Column(Modifier.padding(11.dp)) {
                                        Text(item.appLabel, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(item.title.ifBlank { "Notification" }, color = Color.White, fontWeight = FontWeight.Medium)
                                        if (item.text.isNotBlank()) {
                                            Text(item.text, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("No listener-feed items yet. Enable notification access, then reopen this panel after notifications arrive.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                    FeatureAction.MOTION -> {
                        Text("Reduce visual motion in KepTee's preview. The live wallpaper service also pauses drawing when it isn't visible.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reduced-motion preview", Modifier.weight(1f), color = Color.White)
                            Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange)
                        }
                    }
                    FeatureAction.CONTROL_CENTER -> {
                        Text(
                            "A KepTee quick-access hub for Android's native control panels. These shortcuts open the real system controls; Android still owns the notification shade and Quick Settings.",
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
