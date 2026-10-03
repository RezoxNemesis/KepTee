package com.rezoxnemesis.kebtee

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.rezoxnemesis.kebtee.wallpaper.WallpaperPreferences
import com.rezoxnemesis.kebtee.wallpaper.WallpaperSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DashboardNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val preferences get() = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences(WallpaperPreferences.FILE_NAME, Context.MODE_PRIVATE)

    @Before fun launchDashboard() {
        preferences.edit().clear().putBoolean("onboarding_complete", true).commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    @After fun closeDashboard() { scenario.close() }

    private fun waitForText(text: String) {
        compose.waitUntil(10_000) {
            runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    @Test fun controlsRemainReachableOnShortLandscapeScreen() {
        scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) {
            var landscape = false
            scenario.onActivity { landscape = it.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
            landscape
        }
        waitForText("Light & motion")
        compose.onNodeWithText("Light & motion").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("60 FPS").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Reset wallpaper settings").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Reset wallpaper settings?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Reset wallpaper settings?").assertDoesNotExist()
    }

    @Test fun wallpaperControlsPersistAcrossRecreationAndResetToDefaults() {
        compose.onNodeWithText("60 FPS").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Reduced motion toggle").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle {
            val saved = WallpaperPreferences.read(preferences)
            assertEquals(60, saved.fps)
            assertTrue(saved.reduceMotion)
        }
        scenario.recreate()
        waitForText("Performance")
        compose.onNodeWithText("60 FPS").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Reduced motion toggle").performScrollTo().assertIsOn()
        compose.onNodeWithText("Reset wallpaper settings").performScrollTo().performClick()
        compose.onNodeWithText("Reset").performClick()
        compose.runOnIdle { assertEquals(WallpaperSettings(), WallpaperPreferences.read(preferences)) }
        scenario.recreate()
        waitForText("Performance")
        compose.onNodeWithText("30 FPS").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Reduced motion toggle").performScrollTo().assertIsOff()
    }
}
