package com.rezoxnemesis.kebtee

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class DashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeoutMillis: Long = 10_000) {
        compose.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    @Test fun toolsRemainReachableOnShortLandscapeScreen() {
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // Orientation changes recreate MainActivity. Do not interact with semantics
        // from the detached owner; wait until the new Compose tree is attached.
        waitForText("Control Center")

        compose.onNodeWithText("Control Center").performScrollTo().assertIsDisplayed().performClick()
        waitForText("Done")
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()

        waitForText("Theme Studio")
        compose.onNodeWithText("Theme Studio").performScrollTo().assertIsDisplayed().performClick()
        waitForText("Cyber cyan")
        compose.onNodeWithText("Cyber cyan").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
    }
}
