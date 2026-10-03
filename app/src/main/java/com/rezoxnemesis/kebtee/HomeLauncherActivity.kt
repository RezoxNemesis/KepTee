package com.rezoxnemesis.kebtee

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Silver = Color(0xFFE1E4E8)
private val Glass = Color(0xDC101214)
private data class HomeApp(val label: String, val key: String, val icon: Bitmap, val category: Int)

class HomeLauncherActivity : ComponentActivity() {
    private var apps by mutableStateOf<List<HomeApp>>(emptyList())
    private var loading by mutableStateOf(true)
    private var loadError by mutableStateOf<String?>(null)
    private var layout by mutableStateOf(LauncherLayout())
    private var homeRequests by mutableIntStateOf(0)
    private var notificationAccess by mutableStateOf(false)
    private var loadJob: Job? = null
    private lateinit var widgets: LauncherWidgets
    private lateinit var shortcuts: LauncherShortcuts
    private lateinit var backup: LauncherBackup
    private val preferences by lazy { getSharedPreferences(LauncherLayoutStore.FILE, MODE_PRIVATE) }
    private val usage by lazy { getSharedPreferences("keptee_local_usage", MODE_PRIVATE) }
    private val iconCache = android.util.LruCache<String, Bitmap>(240)
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { iconCache.evictAll(); refreshApps() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        layout = LauncherLayoutStore.read(preferences)
        widgets = LauncherWidgets(this, ::message)
        shortcuts = LauncherShortcuts(this)
        backup = LauncherBackup(this, ::save, ::message)
        ContextCompat.registerReceiver(this, packages, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }, ContextCompat.RECEIVER_EXPORTED)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Silver, onPrimary = Color.Black,
                secondary = Silver, onSecondary = Color.Black,
                background = Color.Black, onBackground = Silver,
                surface = Glass, onSurface = Silver,
                surfaceVariant = Color(0xFF1A1C1F), onSurfaceVariant = Silver.copy(alpha = .72f),
                outline = Silver.copy(alpha = .24f)
            )) {
                Launcher()
            }
        }
    }

    override fun onStart() { super.onStart(); widgets.startListening() }
    override fun onStop() { widgets.stopListening(); super.onStop() }
    override fun onResume() {
        super.onResume()
        layout = LauncherLayoutStore.read(preferences)
        notificationAccess = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        refreshApps()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); homeRequests++ }
    @Deprecated("Widget provider configuration uses the platform host result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgets.onActivityResult(requestCode, resultCode)
    }
    override fun onDestroy() {
        loadJob?.cancel(); unregisterReceiver(packages); iconCache.evictAll(); super.onDestroy()
    }

    private fun save(value: LauncherLayout) { layout = value.normalized(); LauncherLayoutStore.write(preferences, layout) }
    private fun message(value: String) { Toast.makeText(this, value, Toast.LENGTH_LONG).show() }
    private fun settings() { startActivity(Intent(this, MainActivity::class.java)) }

    private fun notificationAccessSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            message("Notification access settings are unavailable on this device.")
        }
    }

    private fun refreshApps() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            loading = true
            try {
                val result = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                        .mapNotNull { info ->
                            val activity = info.activityInfo ?: return@mapNotNull null
                            val key = ComponentName(activity.packageName, activity.name).flattenToString()
                            try {
                                val icon = iconCache.get(key) ?: info.loadIcon(packageManager).toBitmap(96, 96).also { iconCache.put(key, it) }
                                HomeApp(info.loadLabel(packageManager).toString(), key, icon, activity.applicationInfo.category)
                            } catch (_: PackageManager.NameNotFoundException) { null }
                        }.distinctBy { it.key }.sortedBy { it.label.lowercase(Locale.getDefault()) }
                }
                apps = result; loadError = null
                if (!preferences.contains(LauncherLayoutStore.KEY)) {
                    val migrated = LauncherLayoutStore.read(preferences)
                    if (preferences.all.keys.any { it in setOf("favorites", "hidden_apps", "grid_columns", "icon_size_dp", "show_labels", "layout_locked") }) save(migrated) else {
                        val defaults = result.filter { app -> listOf("phone", "camera", "messages", "chrome").any { app.label.contains(it, true) } }.take(4).ifEmpty { result.take(4) }
                        save(LauncherLayout(dock = defaults.map { it.key }))
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: RuntimeException) { loadError = "Apps could not be loaded. Tap Retry." }
            finally { if (isActive) loading = false }
        }
    }

    private fun launch(app: HomeApp) {
        try {
            val component = ComponentName.unflattenFromString(app.key) ?: return
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
            usage.edit().putLong("last:${app.key}", System.currentTimeMillis())
                .putInt("count:${app.key}", (usage.getInt("count:${app.key}", 0).toLong().coerceAtLeast(0) + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).apply()
        } catch (_: ActivityNotFoundException) { message("This app is no longer available."); refreshApps() }
        catch (_: SecurityException) { message("Android blocked this app. Check its profile or restrictions.") }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun Launcher() {
        var drawer by rememberSaveable { mutableStateOf(false) }
        var focusSearch by rememberSaveable { mutableStateOf(false) }
        val searchFocus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        var query by rememberSaveable { mutableStateOf("") }
        var page by rememberSaveable { mutableIntStateOf(0) }
        var edit by rememberSaveable { mutableStateOf(false) }
        var selected by remember { mutableStateOf<HomeApp?>(null) }
        var folderId by rememberSaveable { mutableStateOf<String?>(null) }
        var sort by rememberSaveable { mutableStateOf("A–Z") }
        var dragKey by remember { mutableStateOf<String?>(null) }
        var dragPoint by remember { mutableStateOf(Offset.Zero) }
        val targets = remember { mutableMapOf<String, Rect>() }
        val haptics = LocalHapticFeedback.current
        val available = remember(apps) { apps.associateBy { it.key } }
        val currentPage = page.coerceIn(0, layout.pages.lastIndex)
        val shown = remember(apps, query, layout.hidden, sort, drawer) {
            val filtered = apps.filter { it.key !in layout.hidden && (query.isBlank() || it.label.contains(query.trim(), true)) }
            when (sort) {
                "Recent" -> filtered.filter { usage.getLong("last:${it.key}", 0) > 0 }.sortedByDescending { usage.getLong("last:${it.key}", 0) }
                "Most used" -> filtered.filter { usage.getInt("count:${it.key}", 0) > 0 }.sortedByDescending { usage.getInt("count:${it.key}", 0) }
                else -> filtered
            }
        }
        fun action(name: String) {
            when (name) {
                "Search", "All apps" -> { drawer = true; query = ""; sort = "A–Z"; focusSearch = name == "Search" }
                "Edit home" -> edit = true
                "Settings" -> settings()
            }
        }
        fun drop() {
            val key = dragKey ?: return
            val target = targets.entries.filter { it.value.contains(dragPoint) }
                .minByOrNull { it.value.width * it.value.height }?.key
            if (!layout.locked) when {
                target == "dock" -> save(layout.remove(key).copy(dock = (layout.dock - key + key).takeLast(5)))
                target == "previous" -> { page = (currentPage - 1).coerceAtLeast(0); save(layout.place(key, page)) }
                target == "next" -> { page = (currentPage + 1).coerceAtMost(layout.pages.lastIndex); save(layout.place(key, page)) }
                target?.startsWith("item:") == true -> save(layout.group(key, target.removePrefix("item:")))
                target == "page" -> save(layout.place(key, currentPage))
                else -> message("Drop on Home, the dock, a page arrow, or another app.")
            }
            dragKey = null
        }
        LaunchedEffect(homeRequests) { drawer = false; query = ""; selected = null; folderId = null; edit = false }
        LaunchedEffect(drawer, focusSearch) {
            if (drawer && focusSearch) { searchFocus.requestFocus(); keyboard?.show() }
        }
        BackHandler(enabled = drawer || edit || selected != null || folderId != null) {
            when { selected != null -> selected = null; folderId != null -> folderId = null; edit -> edit = false; else -> { drawer = false; query = "" } }
        }
        // Consume Back on Home so Android does not reveal a previous launcher task.
        BackHandler(enabled = !drawer && !edit && selected == null && folderId == null) { }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (drawer) .88f else .18f))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("KepTee", fontSize = 24.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { edit = true }) { Text("Edit home") }
                    TextButton(onClick = ::settings) { Text("Settings") }
                }
                if (drawer) {
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().focusRequester(searchFocus), label = { Text("Search apps") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("A–Z", "Recent", "Most used").forEach { name -> TextButton(onClick = { sort = name }) { Text(if (sort == name) "• $name" else name) } }
                    }
                    Text("Long press for app actions. Usage is counted only in KepTee.", color = Silver.copy(alpha = .65f), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    when {
                        loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        loadError != null -> Column(Modifier.weight(1f)) { Text(loadError!!); TextButton(onClick = ::refreshApps) { Text("Retry") } }
                        shown.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (query.isNotBlank()) "No apps match that search." else "No apps in this view yet.") }
                        layout.drawerStyle == "List" -> LazyColumn(Modifier.weight(1f)) {
                            listItems(shown, key = { it.key }) { app ->
                                Row(Modifier.fillMaxWidth().combinedClickable(onClick = { launch(app) }, onLongClick = { selected = app }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Image(app.icon.asImageBitmap(), app.label, Modifier.size(44.dp)); Spacer(Modifier.width(16.dp)); Text(app.label)
                                }
                            }
                        }
                        else -> LazyVerticalGrid(GridCells.Fixed(layout.columns), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                            items(shown, key = { it.key }) { app -> AppIcon(app, onOpen = { launch(app) }, onActions = { selected = app }) }
                        }
                    }
                    TextButton(onClick = { drawer = false; query = "" }, Modifier.align(Alignment.CenterHorizontally)) { Text("Back to Home") }
                } else {
                    var swipe by remember { mutableFloatStateOf(0f) }
                    Column(Modifier.weight(1f).fillMaxWidth()
                        .onGloballyPositioned { targets["page"] = it.boundsInRoot() }
                        .pointerInput(layout.swipeDown) {
                            detectVerticalDragGestures(onDragStart = { swipe = 0f }, onDragEnd = {
                                if (swipe < -100) action("All apps") else if (swipe > 100) action(layout.swipeDown)
                            }) { change, amount -> change.consume(); swipe += amount }
                        }.pointerInput(layout.doubleTap) { detectTapGestures(onDoubleTap = { action(layout.doubleTap) }, onLongPress = { edit = true }) }) {
                        Text("SPACE ${(currentPage + 1).toString().padStart(2, '0')}", fontSize = 11.sp, letterSpacing = 3.sp, color = Silver.copy(alpha = .65f), modifier = Modifier.padding(vertical = 16.dp))
                        if (currentPage == 0 && widgets.widgetIds.isNotEmpty()) {
                            LazyColumn(Modifier.weight(1f)) {
                                listItems(widgets.widgetIds, key = { it }) { id ->
                                    var height by remember(id) { mutableIntStateOf(widgets.widgetHeight(id)) }
                                    Column(Modifier.padding(bottom = 10.dp)) {
                                        HostedLauncherWidget(widgets, id, Modifier.fillMaxWidth().height(height.dp))
                                        if (!layout.locked) Row {
                                            TextButton(onClick = { height = (height - 40).coerceAtLeast(100); widgets.resizeWidget(id, height) }) { Text("Smaller") }
                                            TextButton(onClick = { height = (height + 40).coerceAtMost(480); widgets.resizeWidget(id, height) }) { Text("Larger") }
                                            TextButton(onClick = { widgets.removeWidget(id) }) { Text("Remove widget") }
                                        }
                                    }
                                }
                            }
                        }
                        LazyVerticalGrid(GridCells.Fixed(layout.columns), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(layout.pages[currentPage].filter { item -> item.apps.any { it !in layout.hidden } }, key = { it.id }) { item ->
                                DisposableEffect(item.id, currentPage) { onDispose { targets.remove("item:${item.id}") } }
                                var bounds by remember { mutableStateOf(Rect.Zero) }
                                val first = item.apps.firstOrNull { it !in layout.hidden }?.let(available::get)
                                Column(Modifier.onGloballyPositioned { bounds = it.boundsInRoot(); targets["item:${item.id}"] = bounds }
                                    .then(if (!layout.locked && !item.isFolder) Modifier.pointerInput(item.id, layout, currentPage) {
                                        detectDragGesturesAfterLongPress(onDragStart = { offset ->
                                            dragKey = item.apps.first(); dragPoint = bounds.topLeft + offset; haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }, onDrag = { change, amount -> change.consume(); dragPoint += amount }, onDragEnd = { drop() }, onDragCancel = { dragKey = null })
                                    } else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (item.isFolder) {
                                        Surface(onClick = { folderId = item.id }, shape = RoundedCornerShape(20.dp), color = if (item.silver) Silver.copy(alpha = .35f) else Glass, border = BorderStroke(1.dp, Silver.copy(alpha = .25f)), modifier = Modifier.size(layout.iconSize.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Text("${item.apps.size}", fontSize = 22.sp) }
                                        }
                                        if (layout.labels) Text(item.name, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center)
                                    } else if (first != null) {
                                        AppIcon(first, { launch(first) }, { selected = first }, longPress = layout.locked)
                                        if (!layout.locked) TextButton(onClick = { selected = first }, modifier = Modifier.semantics { contentDescription = "Actions for ${first.label}" }) { Text("•••") }
                                    }
                                    else TextButton(onClick = { if (!layout.locked) save(layout.remove(item.apps.first())) else message("Unlock the layout to remove missing apps.") }) { Text("Missing app", fontSize = 10.sp) }
                                }
                            }
                        }
                        if (layout.pages[currentPage].isEmpty() && widgets.widgetIds.isEmpty()) Text("Room for your world.\nAdd apps from All apps.", fontSize = 14.sp, color = Silver.copy(alpha = .7f), modifier = Modifier.padding(bottom = 24.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { page = (currentPage - 1).coerceAtLeast(0) }, Modifier.onGloballyPositioned { targets["previous"] = it.boundsInRoot() }) { Text("‹ Previous") }
                        Text("${currentPage + 1} / ${layout.pages.size}", color = Silver.copy(alpha = .6f), fontSize = 12.sp)
                        TextButton(onClick = { page = (currentPage + 1).coerceAtMost(layout.pages.lastIndex) }, Modifier.onGloballyPositioned { targets["next"] = it.boundsInRoot() }) { Text("Next ›") }
                    }
                    Surface(shape = RoundedCornerShape(26.dp), color = Glass, border = BorderStroke(1.dp, Silver.copy(alpha = .16f)), modifier = Modifier.fillMaxWidth().onGloballyPositioned { targets["dock"] = it.boundsInRoot() }) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            layout.dock.filter { it !in layout.hidden }.mapNotNull(available::get).forEach { app -> Box(Modifier.weight(1f)) { AppIcon(app, { launch(app) }, { selected = app }, dock = true) } }
                            if (layout.dock.isEmpty()) Text("Long press an app in All apps to add it here.", fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { action("All apps") }) { Text("All apps") }
                        TextButton(onClick = { action("Search") }) { Text("Search") }
                    }
                }
            }
            if (dragKey != null) Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding(), color = Silver, shape = RoundedCornerShape(16.dp)) {
                Text("Move ${available[dragKey]?.label.orEmpty()} • release to place", color = Color.Black, modifier = Modifier.padding(12.dp))
            }
        }
        if (edit) EditHome(currentPage, onClose = { edit = false }, onPage = { page = it })
        selected?.let { app ->
            var shortcutResult by remember(app.key) { mutableStateOf<ShortcutQueryResult?>(null) }
            AlertDialog(onDismissRequest = { selected = null }, title = { Text(app.label) }, text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { launch(app); selected = null }) { Text("Open app") }
                    TextButton(onClick = { shortcutResult = shortcuts.query(app.key.substringBefore('/')) }) { Text("App shortcuts") }
                    shortcutResult?.let { result ->
                        result.error?.let { Text(it, fontSize = 12.sp) }
                        result.shortcuts.forEach { shortcut -> TextButton(onClick = { shortcuts.launch(shortcut)?.let(::message); selected = null }) { Text(shortcut.label) } }
                    }
                    if (!layout.locked) {
                        TextButton(onClick = { save(layout.place(app.key, currentPage)); selected = null; drawer = false }) { Text("Add to this page") }
                        TextButton(onClick = { save(layout.copy(dock = (layout.dock - app.key + app.key).takeLast(5))); selected = null }) { Text("Add to dock") }
                        TextButton(onClick = { save(layout.remove(app.key)); selected = null }) { Text("Remove from home and dock") }
                        TextButton(onClick = { save(layout.copy(hidden = layout.hidden + app.key)); selected = null }) { Text("Hide app") }
                    } else Text("Layout is locked. Unlock it in Edit home.")
                    TextButton(onClick = {
                        try { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.key.substringBefore('/')}"))) }
                        catch (_: ActivityNotFoundException) { message("App information is unavailable.") }
                    }) { Text("App information") }
                }
            }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } })
        }
        val folder = layout.pages.flatten().firstOrNull { it.id == folderId }
        if (folder != null) FolderDialog(folder, available, onClose = { folderId = null }, onApp = { selected = it })
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun AppIcon(app: HomeApp, onOpen: () -> Unit, onActions: () -> Unit, longPress: Boolean = true, dock: Boolean = false) {
        val badgeCount = NotificationBadgeState.counts[app.key.substringBefore('/')] ?: 0
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0x99191B1E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
                modifier = Modifier
                    .size((if (dock) layout.iconSize.coerceAtMost(56) else layout.iconSize).dp)
                    .semantics {
                        contentDescription = if (badgeCount > 0) "${app.label}, $badgeCount notifications" else app.label
                    }
                    .combinedClickable(onClick = onOpen, onLongClick = if (longPress) onActions else null)
            ) {
                Box {
                    Image(app.icon.asImageBitmap(), null, Modifier.padding(9.dp).fillMaxSize())
                    if (badgeCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Silver,
                            contentColor = Color.Black,
                            modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).heightIn(min = 18.dp)
                        ) {
                            Text(
                                if (badgeCount >= 99) "99+" else badgeCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            if (layout.labels && !dock) Text(app.label, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
        }
    }

    @Composable
    private fun EditHome(page: Int, onClose: () -> Unit, onPage: (Int) -> Unit) {
        AlertDialog(onDismissRequest = onClose, title = { Text("Edit home") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Lock layout", Modifier.weight(1f)); Switch(layout.locked, { save(layout.copy(locked = it)) }, modifier = Modifier.semantics { contentDescription = "Lock layout toggle" }) }
                if (!layout.locked) {
                    Text("Grid columns: ${layout.columns}")
                    Slider(layout.columns.toFloat(), { save(layout.copy(columns = it.toInt())) }, valueRange = 3f..6f, steps = 2)
                    Text("Icon size: ${layout.iconSize} dp")
                    Slider(layout.iconSize.toFloat(), { save(layout.copy(iconSize = it.toInt())) }, valueRange = 40f..72f)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("App labels", Modifier.weight(1f)); Switch(layout.labels, { save(layout.copy(labels = it)) }, modifier = Modifier.semantics { contentDescription = "App labels toggle" }) }
                    Row {
                        TextButton(onClick = { if (layout.pages.size < 12) { save(layout.copy(pages = layout.pages + listOf(emptyList()))); onPage(layout.pages.lastIndex) } else message("Maximum 12 pages.") }) { Text("Add page") }
                        TextButton(onClick = {
                            if (layout.pages.size == 1) message("Keep at least one page.")
                            else if (layout.pages[page].isNotEmpty()) message("Move this page's apps before removing it.")
                            else { save(layout.copy(pages = layout.pages.filterIndexed { i, _ -> i != page })); onPage(0) }
                        }) { Text("Remove page") }
                    }
                    TextButton(onClick = { widgets.addWidget(); onClose() }) { Text("Add widget") }
                    TextButton(onClick = { autoFolders(page) }) { Text("Group this page by app category") }
                    if (layout.hidden.isNotEmpty()) {
                        Text("Hidden apps")
                        layout.hidden.forEach { key -> TextButton(onClick = { save(layout.copy(hidden = layout.hidden - key)) }) { Text("Unhide ${apps.firstOrNull { it.key == key }?.label ?: key.substringBefore('/')}") } }
                    }
                }
                Choice("Drawer style", listOf("Grid", "List"), layout.drawerStyle) { save(layout.copy(drawerStyle = it)) }
                Choice("Folder style", listOf("Glass", "Radial"), layout.folderStyle) { save(layout.copy(folderStyle = it)) }
                Choice("Swipe down", LauncherLayout.ACTIONS, layout.swipeDown) { save(layout.copy(swipeDown = it)) }
                Choice("Double tap empty space", LauncherLayout.ACTIONS, layout.doubleTap) { save(layout.copy(doubleTap = it)) }
                Text("Notification badges", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(
                    if (notificationAccess) "Enabled. KepTee keeps only per-app counts in memory; notification content is not stored."
                    else "Optional. Android notification access is required to show real per-app badge counts.",
                    fontSize = 12.sp,
                    color = Silver.copy(alpha = .72f)
                )
                TextButton(onClick = ::notificationAccessSettings) {
                    Text(if (notificationAccess) "Notification access settings" else "Enable notification badges")
                }
                TextButton(onClick = { backup.backup(layout) }) { Text("Export backup") }
                TextButton(onClick = { backup.restore(); onClose() }) { Text("Restore backup") }
                Text("Swipe up for apps. Long press empty space to edit. Long press and drag a Home icon onto another to create a folder, or onto the dock or page arrows to move it.", fontSize = 12.sp, color = Silver.copy(alpha = .7f))
            }
        }, confirmButton = { TextButton(onClick = onClose) { Text("Done") } })
    }

    private fun autoFolders(page: Int) {
        if (layout.locked) return
        val original = layout.pages[page]
        val appsByKey = apps.associateBy { it.key }
        val groups = original.filter { !it.isFolder }.groupBy { appsByKey[it.apps.first()]?.category ?: -1 }
        val grouped = groups.flatMap { (category, entries) ->
            if (category < 0 || entries.size < 2) entries else listOf(HomeItem("folder:category:$category:${System.nanoTime()}", entries.flatMap { it.apps }, when(category) {
                0 -> "Games"; 1 -> "Audio"; 2 -> "Video"; 3 -> "Images"; 4 -> "Social"; 5 -> "News"; 6 -> "Maps"; else -> "Productivity"
            }))
        }
        save(layout.copy(pages = layout.pages.mapIndexed { i, items -> if (i == page) original.filter { it.isFolder } + grouped else items }))
    }

    @Composable
    private fun FolderDialog(folder: HomeItem, available: Map<String, HomeApp>, onClose: () -> Unit, onApp: (HomeApp) -> Unit) {
        var name by remember(folder.id) { mutableStateOf(folder.name) }
        fun update(value: HomeItem) { save(layout.copy(pages = layout.pages.map { items -> items.map { if (it.id == folder.id) value else it } })) }
        AlertDialog(onDismissRequest = onClose, title = { Text(folder.name) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val members = folder.apps.filter { it !in layout.hidden }.mapNotNull(available::get)
                if (layout.folderStyle == "Radial" && members.size in 2..8) {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        members.forEachIndexed { index, app ->
                            val angle = index * 2 * Math.PI / members.size - Math.PI / 2
                            TextButton(onClick = { launch(app) }, Modifier.offset((cos(angle) * 84).dp, (sin(angle) * 92).dp).width(86.dp)) { Text(app.label, maxLines = 2, fontSize = 11.sp, textAlign = TextAlign.Center) }
                        }
                    }
                }
                members.forEach { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { launch(app) }, Modifier.weight(1f)) { Text(app.label) }
                        if (!layout.locked) {
                            TextButton(onClick = { update(folder.copy(apps = listOf(app.key) + (folder.apps - app.key))) }) { Text("First") }
                            TextButton(onClick = { save(layout.place(app.key, layout.pages.indexOfFirst { folder in it })); if (folder.apps.size <= 2) onClose() }) { Text("Move out") }
                        }
                    }
                }
                if (!layout.locked) {
                    OutlinedTextField(name, { name = it.take(48) }, label = { Text("Folder name") }, singleLine = true)
                    TextButton(onClick = { update(folder.copy(name = name.ifBlank { "Folder" })); onClose() }) { Text("Rename") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Silver folder", Modifier.weight(1f)); Switch(folder.silver, { update(folder.copy(silver = it)) }) }
                }
            }
        }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
    }

    @Composable
    private fun Choice(title: String, values: List<String>, current: String, onChange: (String) -> Unit) {
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        values.chunked(2).forEach { row -> Row { row.forEach { value -> TextButton(onClick = { onChange(value) }) { Text(if (value == current) "• $value" else value) } } } }
    }
}
