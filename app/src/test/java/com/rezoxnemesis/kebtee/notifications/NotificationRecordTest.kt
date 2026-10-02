package com.rezoxnemesis.kebtee.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRecordTest {
    @Test
    fun notificationRecordKeepsStableIdentityAndContent() {
        val record = NotificationRecord("pkg:key", "pkg", "Example", "Hello", "World", 1234L)
        assertEquals("pkg:key", record.key)
        assertEquals("Example", record.appLabel)
        assertEquals("Hello", record.title)
        assertEquals("World", record.text)
        assertTrue(record.timestamp > 0L)
    }
}