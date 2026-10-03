package com.rezoxnemesis.kebtee

import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezoxnemesis.kebtee.wallpaper.KebTeeLiveWallpaperService
import com.rezoxnemesis.kebtee.wallpaper.WallpaperPreferences
import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import kotlin.math.roundToInt

private val SettingsSilver = Color(0xFFE5E6E8)
private val SettingsMuted = Color(0xFFA8ABB0)
private val SettingsGlass = Color(0xFF141517)

class MainActivity : ComponentActivity() {
    private var isDefaultHome by mutableStateOf(false)
    private var wallpaperApplied by mutableStateOf(false)
    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshSystemState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        val preferences = getSharedPreferences(WallpaperPreferences.FILE_NAME, MODE_PRIVATE)
        refreshSystemState()
        setContent {
            var wallpaper by remember { mutableStateOf(WallpaperPreferences.read(preferences)) }
            var onboarding by remember { mutableStateOf(!preferences.getBoolean("onboarding_complete", false)) }
            var resetRequested by remember { mutableStateOf(false) }
            val update: (WallpaperSettings) -> Unit = {
                wallpaper = it.normalized()
                WallpaperPreferences.write(preferences, wallpaper)
            }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = SettingsSilver, onPrimary = Color.Black,
                secondary = SettingsSilver, background = Color.Black,
                surface = SettingsGlass, onSurface = SettingsSilver,
                onBackground = SettingsSilver, outline = Color(0xFF3A3C40)
            )) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RasterResourceImage(
                                resId = R.drawable.kebtee_logo_master,
                                contentDescription = "KepTee logo",
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("KepTee", fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text("Your space. In motion.", color = SettingsMuted, fontSize = 12.sp)
                            }
                        }
                        if (onboarding) {
                            SettingsPanel {
                                Text("Make yourself at home", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text("Choose KepTee as your home, then bring the original silhouette to life. You can return to these controls anytime.", color = SettingsMuted)
                                SetupActions(isDefaultHome, wallpaperApplied, ::requestHomeRole, ::openWallpaperPicker)
                                Text("On your home screen, swipe up to open your apps. Long-press an app to organise it. Open launcher settings to customise your layout and gestures.", color = SettingsMuted, fontSize = 13.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        preferences.edit().putBoolean("onboarding_complete", true).apply()
                                        onboarding = false
                                    }) { Text(if (isDefaultHome && wallpaperApplied) "Finish setup" else "Skip setup") }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("HOME EXPERIENCE", fontSize = 11.sp, letterSpacing = 2.sp, color = SettingsMuted)
                            Text(if (isDefaultHome) "KepTee is your default home" else "Your launcher is ready", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (isDefaultHome) "Press Home to return to your space." else "Android will ask you to choose a default home app.", color = SettingsMuted, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { safeStart(Intent(this@MainActivity, HomeLauncherActivity::class.java)) }, modifier = Modifier.weight(1f)) { Text("Open launcher") }
                                OutlinedButton(onClick = ::requestHomeRole, modifier = Modifier.weight(1f)) { Text(if (isDefaultHome) "Home settings" else "Set as default home") }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF292B2E))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("LIVE WALLPAPER", fontSize = 11.sp, letterSpacing = 2.sp, color = SettingsMuted)
                            Text("Silhouette", fontSize = 28.sp, fontWeight = FontWeight.Light)
                            RasterResourceImage(
                                resId = R.drawable.kebtee_silhouette,
                                contentDescription = "Original KepTee silhouette artwork",
                                modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black)
                            )
                            Text(if (wallpaperApplied) "Silhouette is active. Controls below update it immediately." else "Original artwork with light, particles and motion. Preview the real wallpaper in Android before applying it.", color = SettingsMuted, fontSize = 13.sp)
                            Button(onClick = ::openWallpaperPicker, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Choose & Apply Wallpaper") }
                            Text("Android controls whether a live wallpaper appears on Home, the lock screen, or both. Available choices depend on your device.", color = SettingsMuted, fontSize = 12.sp)
                        }
                        SettingsPanel {
                            Text("Light & motion", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            ValueSlider("Animation speed", "${(wallpaper.speed * 100).roundToInt()}%", wallpaper.speed, 0.25f..2f) { update(wallpaper.copy(speed = it)) }
                            ValueSlider("Glow intensity", "${(wallpaper.glow * 100).roundToInt()}%", wallpaper.glow, 0f..1f) { update(wallpaper.copy(glow = it)) }
                            ValueSlider("Particle density", wallpaper.particleDensity.toString(), wallpaper.particleDensity.toFloat(), 0f..60f) { update(wallpaper.copy(particleDensity = it.roundToInt())) }
                            SettingsToggle("Touch effects", "Ripple light from a touch on your wallpaper.", wallpaper.touchEffects) { update(wallpaper.copy(touchEffects = it)) }
                            SettingsToggle("Time effects", "Let the light change through the day.", wallpaper.timeEffects) { update(wallpaper.copy(timeEffects = it)) }
                            SettingsToggle("Charging effects", "A gentle light response while charging.", wallpaper.chargingEffects) { update(wallpaper.copy(chargingEffects = it)) }
                        }
                        SettingsPanel {
                            Text("Performance", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("Wallpaper frame rate", fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(15, 30, 60).forEach { fps ->
                                    FilterChip(selected = wallpaper.fps == fps, onClick = { update(wallpaper.copy(fps = fps)) }, label = { Text("$fps FPS") })
                                }
                            }
                            Text("A target rate, not a guarantee. Android power saving and lock-screen dimming reduce animation; hidden wallpaper stops rendering.", color = SettingsMuted, fontSize = 12.sp)
                            SettingsToggle("Battery saver", "Limit updates to 10 FPS and turn off particles.", wallpaper.batterySaver) { update(wallpaper.copy(batterySaver = it)) }
                            SettingsToggle("Reduced motion", "Keep a still scene, refreshed only on changes.", wallpaper.reduceMotion) { update(wallpaper.copy(reduceMotion = it)) }
                            SettingsToggle("Dim on lock screen", "Lower light and motion when the device is locked.", wallpaper.dimOnLock) { update(wallpaper.copy(dimOnLock = it)) }
                        }
                        OutlinedButton(onClick = { resetRequested = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset wallpaper settings") }
                        TextButton(onClick = { onboarding = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Show setup guide") }
                        Text("KepTee • On-device personalisation", color = SettingsMuted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
                if (resetRequested) {
                    AlertDialog(onDismissRequest = { resetRequested = false }, title = { Text("Reset wallpaper settings?") }, text = { Text("Restore the default light, motion and performance settings. Your home layout stays as it is.") }, confirmButton = {
                        TextButton(onClick = { update(WallpaperSettings()); resetRequested = false }) { Text("Reset") }
                    }, dismissButton = { TextButton(onClick = { resetRequested = false }) { Text("Cancel") } })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemState()
    }

    private fun refreshSystemState() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        isDefaultHome = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
        wallpaperApplied = try {
            WallpaperManager.getInstance(this).wallpaperInfo?.component == ComponentName(this, KebTeeLiveWallpaperService::class.java)
        } catch (_: SecurityException) { false }
    }

    private fun requestHomeRole() {
        if (!isDefaultHome && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_HOME) && !manager.isRoleHeld(RoleManager.ROLE_HOME)) {
                try {
                    homeRoleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                } catch (_: android.content.ActivityNotFoundException) { /* OEM fallback below. */ }
                  catch (_: SecurityException) { /* OEM fallback below. */ }
            }
        }
        safeStart(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun openWallpaperPicker() {
        val preview = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, KebTeeLiveWallpaperService::class.java)
        )
        if (!tryStart(preview) && !tryStart(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))) {
            Toast.makeText(this, "This device does not provide a live wallpaper picker.", Toast.LENGTH_LONG).show()
        }
    }

    private fun safeStart(intent: Intent) {
        if (!tryStart(intent)) Toast.makeText(this, "This screen is unavailable on your device.", Toast.LENGTH_LONG).show()
    }

    private fun tryStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: android.content.ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }
}

@Composable
private fun RasterResourceImage(
    resId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setImageResource(resId)
                this.contentDescription = contentDescription
            }
        },
        update = { view ->
            view.setImageResource(resId)
            view.contentDescription = contentDescription
        }
    )
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = SettingsGlass, border = BorderStroke(1.dp, Color(0xFF2C2E31)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun SetupActions(isHome: Boolean, isWallpaper: Boolean, onHome: () -> Unit, onWallpaper: () -> Unit) {
    OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(if (isHome) "Default home is ready" else "Set as default home") }
    OutlinedButton(onClick = onWallpaper, modifier = Modifier.fillMaxWidth()) { Text(if (isWallpaper) "Preview your live wallpaper" else "Set up live wallpaper") }
}

@Composable
private fun SettingsToggle(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(detail, color = SettingsMuted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = "$title toggle" })
    }
}

@Composable
private fun ValueSlider(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(valueLabel, color = SettingsMuted)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().semantics { contentDescription = title })
    }
}
