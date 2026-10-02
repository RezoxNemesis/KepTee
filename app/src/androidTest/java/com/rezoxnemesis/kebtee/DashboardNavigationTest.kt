package com.rezoxnemesis.kebtee

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class DashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun toolsRemainReachableOnShortLandscapeScreen() {
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        compose.onNodeWithText("Control Center").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        compose.onNodeWithText("Theme Studio").performScrollTo().performClick()
        compose.onNodeWithText("Cyber cyan").assertIsDisplayed().performClick()
        compose.onNodeWithText("Done").performClick()
    }
}
