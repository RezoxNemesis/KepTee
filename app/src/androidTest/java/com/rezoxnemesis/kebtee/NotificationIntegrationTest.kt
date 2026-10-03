package com.rezoxnemesis.kebtee

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationIntegrationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun notificationListenerIsRegisteredForRealLauncherBadges() {
        val resolved = context.packageManager.resolveService(
            Intent(NotificationListenerService.SERVICE_INTERFACE).setPackage(context.packageName),
            PackageManager.GET_META_DATA
        )
        assertNotNull("KepTee notification listener is not registered", resolved)
        val service = requireNotNull(resolved).serviceInfo
        assertEquals("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", service.permission)
        assertTrue("Notification listener must be exported for Android to bind it", service.exported)
    }
}
