package com.rezoxnemesis.kebtee

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Privacy-minimal notification badge state.
 *
 * KepTee keeps only an in-memory count per package. Notification titles, text,
 * extras, keys and other content are never persisted or transmitted.
 */
object NotificationBadgeState {
    private val main = Handler(Looper.getMainLooper())

    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    internal fun publish(notifications: Array<StatusBarNotification>?) {
        val next = notifications.orEmpty()
            .asSequence()
            .filter { it.packageName.isNotBlank() }
            .filter {
                val flags = it.notification.flags
                flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                    flags and Notification.FLAG_ONGOING_EVENT == 0
            }
            .groupingBy(StatusBarNotification::getPackageName)
            .eachCount()
            .mapValues { (_, count) -> count.coerceAtMost(99) }

        if (Looper.myLooper() == Looper.getMainLooper()) counts = next
        else main.post { counts = next }
    }

    internal fun clear() {
        if (Looper.myLooper() == Looper.getMainLooper()) counts = emptyMap()
        else main.post { counts = emptyMap() }
    }
}

/**
 * System-bound listener used only for on-device per-app badge counts.
 * Permission is granted explicitly by the user in Android notification-access settings.
 */
class KepTeeNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshCounts()
    }

    override fun onListenerDisconnected() {
        NotificationBadgeState.clear()
        super.onListenerDisconnected()
    }

    private fun refreshCounts() {
        try {
            NotificationBadgeState.publish(activeNotifications)
        } catch (_: SecurityException) {
            NotificationBadgeState.clear()
        } catch (_: RuntimeException) {
            // Android can invalidate the listener connection during role/settings changes.
            NotificationBadgeState.clear()
        }
    }
}
