package com.rezoxnemesis.kebtee

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class HomeApp(val label: String, val packageName: String, val className: String, val icon: Bitmap) {
    val key: String get() = "$packageName/$className"
}

class HomeLauncherActivity : ComponentActivity() {
    private val night = Color(0xFF070B18)
    private val panel = Color(0xFF121A2D)
    private var installedApps by mutableStateOf<List<HomeApp>>(emptyList())
    private var launcherAccent by mutableStateOf(Color(0xFF22D3EE))
    private var appsLoadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 24)
        launcherAccent = accentFromPreference(getSharedPreferences("kebtee_preferences", MODE_PRIVATE).getString("accent", "violet"))
        val launcherPreferences = getSharedPreferences("kebtee_launcher", MODE_PRIVATE)

        setContent {
            val apps = installedApps
            var favoriteKeys by remember { mutableStateOf(launcherPreferences.getStringSet("favorites", null)?.toSet()) }
            LaunchedEffect(apps) {
                if (apps.isNotEmpty() && favoriteKeys == null) {
                    val defaults = apps.filter { app ->
                        listOf("phone", "camera", "chrome", "messages", "settings").any { keyword ->
                            app.label.contains(keyword, ignoreCase = true)
                        }
                    }.take(5).ifEmpty { apps.take(5) }.map { it.key }.toSet()
                    favoriteKeys = defaults
                    launcherPreferences.edit().putStringSet("favorites", defaults).apply()
                }
            }
            val activeFavoriteKeys = favoriteKeys ?: emptySet()
            val cyan = launcherAccent
            var search by remember { mutableStateOf("") }
            var showAllApps by remember { mutableStateOf(false) }
            var showLayoutSettings by remember { mutableStateOf(false) }
            var gridColumns by remember {
                mutableIntStateOf(launcherPreferences.getInt("grid_columns", 4).coerceIn(3, 6))
            }
            var iconSizeDp by remember {
                mutableIntStateOf(launcherPreferences.getInt("icon_size_dp", 60).coerceIn(48, 80))
            }
            var showLabels by remember {
                mutableStateOf(launcherPreferences.getBoolean("show_labels", true))
            }
            var layoutLocked by remember {
                mutableStateOf(launcherPreferences.getBoolean("layout_locked", false))
            }
            var hiddenKeys by remember {
                mutableStateOf(launcherPreferences.getStringSet("hidden_apps", emptySet())?.toSet() ?: emptySet())
            }
            val visibleApps = remember(apps, hiddenKeys) { apps.filter { it.key !in hiddenKeys } }
            val filtered = remember(search, visibleApps) {
                if (search.isBlank()) visibleApps else visibleApps.filter { it.label.contains(search.trim(), ignoreCase = true) }
            }
            val favorites = remember(activeFavoriteKeys, visibleApps) { visibleApps.filter { it.key in activeFavoriteKeys } }

            BackHandler(enabled = showAllApps) { showAllApps = false }
            MaterialTheme(colorScheme = darkColorScheme(primary = cyan, secondary = cyan, background = night, surface = panel)) {
                Column(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.46f))))
                        .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("KepTee", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text(if (showAllApps) "YOUR SPACE. YOUR APPS." else "YOUR SPACE. YOUR FAVOURITES.", color = cyan, fontSize = 10.sp, letterSpacing = 1.4.sp)
                        }
                        Surface(color = Color(0xFF182440), shape = RoundedCornerShape(18.dp)) {
                            Text("${visibleApps.size} APPS", color = Color(0xFFB9C7E5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { showLayoutSettings = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit Home layout", tint = cyan)
                        }
                        IconButton(onClick = { startActivity(Intent(this@HomeLauncherActivity, MainActivity::class.java)) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Open KepTee customization dashboard", tint = cyan)
                        }
                    }

                    if (showAllApps) {
                        Spacer(Modifier.height(22.dp))
                        Text("Find your next move.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Tap an app to open it. Use the star to pin it to Home.", color = Color(0xFFADB8D1), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
                        OutlinedTextField(
                            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            placeholder = { Text("Search apps", color = Color(0xFF8997B8)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = cyan) },
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = cyan, unfocusedBorderColor = Color(0xFF293451),
                                cursorColor = cyan, focusedContainerColor = panel, unfocusedContainerColor = panel
                            )
                        )
                        Spacer(Modifier.height(14.dp))
                        if (filtered.isEmpty()) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No apps match that search.", color = Color(0xFFADB8D1), textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns), modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                items(filtered, key = { it.key }) { app ->
                                    AppTile(
                                        app = app, pinned = app.key in activeFavoriteKeys, cyan = cyan,
                                        iconSizeDp = iconSizeDp,
                                        showLabel = showLabels,
                                        canEdit = !layoutLocked,
                                        onOpen = { launchApp(app) },
                                        onTogglePin = {
                                            val updated = if (app.key in activeFavoriteKeys) activeFavoriteKeys - app.key else activeFavoriteKeys + app.key
                                            favoriteKeys = updated
                                            launcherPreferences.edit().putStringSet("favorites", updated).apply()
                                        },
                                        onHide = {
                                            val updated = hiddenKeys + app.key
                                            hiddenKeys = updated
                                            launcherPreferences.edit().putStringSet("hidden_apps", updated).apply()
                                        }
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { showAllApps = false; search = "" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Back to Home", color = cyan)
                        }
                    } else {
                        Spacer(Modifier.height(28.dp))
                        Text("Your essentials.", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                        Text("Keep your everyday apps one tap away.", color = Color(0xFFADB8D1), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                        Spacer(Modifier.height(20.dp))
                        if (favorites.isEmpty()) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Your Home is ready for its first pin.", color = Color.White, textAlign = TextAlign.Center)
                                    Text("Open All Apps and tap a star to add favourites.", color = Color(0xFFADB8D1), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns), modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                items(favorites, key = { it.key }) { app ->
                                    AppTile(
                                        app = app, pinned = true, cyan = cyan,
                                        iconSizeDp = iconSizeDp,
                                        showLabel = showLabels,
                                        canEdit = !layoutLocked,
                                        onOpen = { launchApp(app) },
                                        onTogglePin = {
                                            val updated = activeFavoriteKeys - app.key
                                            favoriteKeys = updated
                                            launcherPreferences.edit().putStringSet("favorites", updated).apply()
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { showAllApps = true },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color(0xFF07111E))
                        ) { Text("All apps  ·  ${visibleApps.size}", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (layoutLocked) "Home layout is locked. Open layout controls to edit." else "Your pins stay saved on this device.",
                            color = Color(0xFF7786A8), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (showLayoutSettings) {
                HomeLayoutDialog(
                    gridColumns = gridColumns,
                    iconSizeDp = iconSizeDp,
                    showLabels = showLabels,
                    layoutLocked = layoutLocked,
                    hiddenAppCount = hiddenKeys.size,
                    onGridColumnsChange = { value ->
                        gridColumns = value
                        launcherPreferences.edit().putInt("grid_columns", value).apply()
                    },
                    onIconSizeChange = { value ->
                        iconSizeDp = value
                        launcherPreferences.edit().putInt("icon_size_dp", value).apply()
                    },
                    onShowLabelsChange = { value ->
                        showLabels = value
                        launcherPreferences.edit().putBoolean("show_labels", value).apply()
                    },
                    onLayoutLockedChange = { value ->
                        layoutLocked = value
                        launcherPreferences.edit().putBoolean("layout_locked", value).apply()
                    },
                    onRestoreHiddenApps = {
                        hiddenKeys = emptySet()
                        launcherPreferences.edit().remove("hidden_apps").apply()
                    },
                    onDismiss = { showLayoutSettings = false }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launcherAccent = accentFromPreference(getSharedPreferences("kebtee_preferences", MODE_PRIVATE).getString("accent", "violet"))
        refreshInstalledApps()
    }

    override fun onDestroy() {
        appsLoadJob?.cancel()
        super.onDestroy()
    }

    private fun refreshInstalledApps() {
        appsLoadJob?.cancel()
        appsLoadJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { loadApps() }.getOrElse { emptyList() }
            }
            installedApps = result
        }
    }

    @Composable
    private fun AppTile(
        app: HomeApp,
        pinned: Boolean,
        cyan: Color,
        iconSizeDp: Int,
        showLabel: Boolean,
        canEdit: Boolean,
        onOpen: () -> Unit,
        onTogglePin: () -> Unit,
        onHide: (() -> Unit)? = null
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Surface(
                    color = Color(0xFF17213A), shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(iconSizeDp.dp).clickable(onClick = onOpen)
                ) {
                    Image(bitmap = app.icon.asImageBitmap(), contentDescription = app.label, modifier = Modifier.padding(8.dp))
                }
                if (canEdit) {
                    Surface(
                        color = if (pinned) cyan else Color(0xFF283451),
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.TopEnd).size(19.dp).clickable(onClick = onTogglePin)
                    ) {
                        Text(if (pinned) "★" else "+", color = Color(0xFF07111E), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 1.dp))
                    }
                    if (onHide != null) {
                        Surface(
                            color = Color(0xCC101827),
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.TopStart).size(19.dp).clickable(onClick = onHide)
                        ) {
                            Text("×", color = Color(0xFFDDE5F5), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (showLabel) {
                Spacer(Modifier.height(7.dp))
                Text(app.label, color = Color(0xFFE8EDFA), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }

    @Composable
    private fun HomeLayoutDialog(
        gridColumns: Int,
        iconSizeDp: Int,
        showLabels: Boolean,
        layoutLocked: Boolean,
        hiddenAppCount: Int,
        onGridColumnsChange: (Int) -> Unit,
        onIconSizeChange: (Int) -> Unit,
        onShowLabelsChange: (Boolean) -> Unit,
        onLayoutLockedChange: (Boolean) -> Unit,
        onRestoreHiddenApps: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xE611182B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFC7D0E5),
            title = { Text("Home layout", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Grid · ${gridColumns} columns", color = Color.White)
                    Slider(
                        value = gridColumns.toFloat(),
                        onValueChange = { onGridColumnsChange(it.roundToInt().coerceIn(3, 6)) },
                        valueRange = 3f..6f,
                        steps = 2
                    )
                    Text("Icon size · ${iconSizeDp}dp", color = Color.White)
                    Slider(
                        value = iconSizeDp.toFloat(),
                        onValueChange = { onIconSizeChange((it / 4f).roundToInt().times(4).coerceIn(48, 80)) },
                        valueRange = 48f..80f,
                        steps = 7
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show app labels", Modifier.weight(1f), color = Color.White)
                        Switch(checked = showLabels, onCheckedChange = onShowLabelsChange)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Lock Home layout", color = Color.White)
                            Text("Prevents pin/unpin/hide edits while locked.", color = Color(0xFF9EABC9), fontSize = 11.sp)
                        }
                        Switch(checked = layoutLocked, onCheckedChange = onLayoutLockedChange)
                    }
                    if (hiddenAppCount > 0) {
                        OutlinedButton(onClick = onRestoreHiddenApps, modifier = Modifier.fillMaxWidth()) {
                            Text("Restore hidden apps · $hiddenAppCount")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
        )
    }

    @Suppress("DEPRECATION")
    private fun loadApps(): List<HomeApp> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL).mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            try {
                HomeApp(info.loadLabel(packageManager).toString(), activity.packageName, activity.name, info.loadIcon(packageManager).toBitmap(72, 72))
            } catch (_: Exception) { null }
        }.distinctBy { it.key }.sortedBy { it.label.lowercase() }
    }

    private fun launchApp(app: HomeApp) {
        try {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(app.packageName, app.className)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
    }
}
