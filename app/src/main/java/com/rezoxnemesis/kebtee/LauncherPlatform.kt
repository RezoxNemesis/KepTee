package com.rezoxnemesis.kebtee

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView

/** Activity-owned widget host. Construct in onCreate, before the activity is STARTED. */
class LauncherWidgets(
    private val activity: ComponentActivity,
    private val onMessage: (String) -> Unit
) {
    private val preferences = activity.getSharedPreferences("keptee_widgets", Context.MODE_PRIVATE)
    private val manager = AppWidgetManager.getInstance(activity)
    private val host = AppWidgetHost(activity, HOST_ID)
    private var listening = false
    private var pendingId: Int
        get() = preferences.getInt("pending_id", AppWidgetManager.INVALID_APPWIDGET_ID)
        set(value) { preferences.edit().putInt("pending_id", value).apply() }

    var widgetIds by mutableStateOf(
        preferences.getString("widget_ids", "").orEmpty().split(',')
            .mapNotNull(String::toIntOrNull).filter { it > 0 }.distinct()
    )
        private set

    private val picker = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            discardPending()
            return@registerForActivityResult
        }
        // Use only the ID allocated by our host; never adopt an ID supplied by another host.
        if (manager.getAppWidgetInfo(id) != null) {
            configureOrFinish(id)
        } else {
            @Suppress("DEPRECATION")
            val provider = result.data?.getParcelableExtra<ComponentName>(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER)
            if (provider == null) failPending("This widget picker did not return a usable widget.")
            else bind(id, provider)
        }
    }
    private val binding = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK && manager.getAppWidgetInfo(id) != null) configureOrFinish(id)
        else discardPending()
    }

    fun startListening() {
        if (listening) return
        try {
            host.startListening()
            listening = true
        } catch (_: RuntimeException) {
            onMessage("Android could not start widget updates. Try returning to Home.")
        }
    }

    fun stopListening() {
        if (!listening) return
        try { host.stopListening() } catch (_: RuntimeException) { /* Host may already be detached. */ }
        listening = false
    }

    fun addWidget() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // A new explicit request replaces a cancelled/interrupted flow left after process death.
            discardPending()
            if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                onMessage("Android could not clear the previous widget request. Please try again.")
                return
            }
        }
        try {
            val id = host.allocateAppWidgetId()
            pendingId = id
            picker.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        } catch (_: RuntimeException) {
            failPending("This device does not provide an Android widget picker.")
        }
    }

    private fun bind(id: Int, provider: ComponentName) {
        try {
            if (manager.bindAppWidgetIdIfAllowed(id, provider)) configureOrFinish(id)
            else binding.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider))
        } catch (_: RuntimeException) {
            failPending("Android could not bind this widget. Try a different widget.")
        }
    }

    private fun configureOrFinish(id: Int) {
        val info = manager.getAppWidgetInfo(id)
        if (info == null) {
            failPending("The widget provider is no longer available.")
            return
        }
        if (info.configure == null) finishPending(id)
        else try {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, CONFIGURE_REQUEST_CODE, null)
        } catch (_: RuntimeException) {
            failPending("The widget's setup screen could not be opened.")
        }
    }

    /** Forward the owning Activity's legacy result here for provider configuration. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != CONFIGURE_REQUEST_CODE) return false
        val id = pendingId
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (resultCode == Activity.RESULT_OK && manager.getAppWidgetInfo(id) != null) finishPending(id)
            else discardPending()
        }
        return true
    }

    private fun finishPending(id: Int) {
        widgetIds = (widgetIds + id).distinct()
        preferences.edit().putString("widget_ids", widgetIds.joinToString(","))
            .remove("pending_id").apply()
    }

    fun removeWidget(id: Int) {
        if (id !in widgetIds) return
        try { host.deleteAppWidgetId(id) } catch (_: RuntimeException) {
            onMessage("Android could not remove this widget. Please try again.")
            return
        }
        widgetIds = widgetIds - id
        preferences.edit().putString("widget_ids", widgetIds.joinToString(",")).remove("height_$id").apply()
    }

    fun widgetHeight(id: Int): Int = preferences.getInt("height_$id", 180).coerceIn(100, 480)

    fun resizeWidget(id: Int, heightDp: Int) {
        if (id in widgetIds) preferences.edit().putInt("height_$id", heightDp.coerceIn(100, 480)).apply()
    }

    internal fun info(id: Int) = manager.getAppWidgetInfo(id)
    internal fun createView(id: Int) = info(id)?.let { host.createView(activity, id, it) }

    private fun discardPending() {
        val id = pendingId
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try { host.deleteAppWidgetId(id) } catch (_: RuntimeException) { /* Keep persisted ID to retry cleanup. */ return }
        }
        preferences.edit().remove("pending_id").apply()
    }

    private fun failPending(message: String) {
        discardPending()
        onMessage(message)
    }

    companion object {
        private const val HOST_ID = 2048
        const val CONFIGURE_REQUEST_CODE = 7312
    }
}

/** Render the provider's RemoteViews; child controls remain interactive. */
@Composable
fun HostedLauncherWidget(controller: LauncherWidgets, id: Int, modifier: Modifier = Modifier) {
    val info = controller.info(id)
    if (info == null) {
        Text("Widget unavailable. Remove it and add it again after installing its app.", modifier)
        return
    }
    val density = LocalDensity.current.density
    var width by androidx.compose.runtime.remember(id) { mutableStateOf(0) }
    var height by androidx.compose.runtime.remember(id) { mutableStateOf(0) }
    AndroidView(
        modifier = modifier.onSizeChanged {
            width = (it.width / density).toInt()
            height = (it.height / density).toInt()
        },
        factory = { context ->
            try {
                controller.createView(id) ?: android.widget.TextView(context).apply { text = "Widget unavailable" }
            } catch (_: RuntimeException) {
                android.widget.TextView(context).apply { text = "This widget could not be displayed. Remove it and try adding it again." }
            }
        },
        update = { view ->
            if (view is android.appwidget.AppWidgetHostView && width > 0 && height > 0) {
                try {
                    @Suppress("DEPRECATION")
                    view.updateAppWidgetSize(null, width, height, width, height)
                } catch (_: RuntimeException) { /* Provider may have been removed while visible. */ }
            }
        }
    )
}

data class LauncherShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val user: UserHandle,
    val icon: Drawable?
)

data class ShortcutQueryResult(val shortcuts: List<LauncherShortcut> = emptyList(), val error: String? = null)

/** Shortcut access is granted by Android to the selected default launcher. */
class LauncherShortcuts(private val activity: ComponentActivity) {
    private val launcher = activity.getSystemService(LauncherApps::class.java)

    fun query(packageName: String): ShortcutQueryResult {
        return try {
            if (!launcher.hasShortcutHostPermission()) {
                ShortcutQueryResult(error = "Set KepTee as your default Home app to access app shortcuts.")
            } else {
                val query = LauncherApps.ShortcutQuery().setPackage(packageName).setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
                val shortcuts = launcher.getShortcuts(query, android.os.Process.myUserHandle()).orEmpty()
                    .filter(ShortcutInfo::isEnabled).sortedBy(ShortcutInfo::getRank).map { info ->
                        LauncherShortcut(info.id, info.`package`, info.shortLabel?.toString() ?: info.id, info.userHandle,
                            try { launcher.getShortcutIconDrawable(info, activity.resources.displayMetrics.densityDpi) }
                            catch (_: RuntimeException) { null })
                    }
                ShortcutQueryResult(shortcuts, if (shortcuts.isEmpty()) "This app has no available shortcuts." else null)
            }
        } catch (_: SecurityException) {
            ShortcutQueryResult(error = "Android denied shortcut access. Check your default Home app setting.")
        } catch (_: RuntimeException) {
            ShortcutQueryResult(error = "This app's shortcuts are temporarily unavailable.")
        }
    }

    /** Null means launched; otherwise the returned message is suitable for a snackbar. */
    fun launch(shortcut: LauncherShortcut): String? = try {
        if (!launcher.hasShortcutHostPermission()) "Set KepTee as your default Home app to open shortcuts."
        else {
            launcher.startShortcut(shortcut.packageName, shortcut.id, null, null, shortcut.user)
            null
        }
    } catch (_: SecurityException) {
        "Android denied access to this shortcut. Check your default Home app setting."
    } catch (_: RuntimeException) {
        "This shortcut is no longer available. Open the app to continue."
    }
}
