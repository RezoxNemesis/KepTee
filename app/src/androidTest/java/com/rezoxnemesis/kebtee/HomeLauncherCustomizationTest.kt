package com.rezoxnemesis.kebtee

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class HomeLauncherCustomizationTest {
    @get:Rule val compose = createAndroidComposeRule<HomeLauncherActivity>()

    @Test fun homeLayoutControlsAreReachable() {
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Edit Home layout")
            .assertIsDisplayed()
            .performClick()

        compose.onNodeWithText("Home layout").assertIsDisplayed()
        compose.onNodeWithText("Show app labels").assertIsDisplayed()
        compose.onNodeWithText("Lock Home layout").assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
    }
}
