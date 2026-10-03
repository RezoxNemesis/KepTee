package com.rezoxnemesis.kebtee

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeLauncherCustomizationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<HomeLauncherActivity>
    private val preferences get() = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences(LauncherLayoutStore.FILE, Context.MODE_PRIVATE)

    @Before fun launchHome() {
        preferences.edit().clear().putString(LauncherLayoutStore.KEY, LauncherLayoutStore.encode(LauncherLayout())).commit()
        scenario = ActivityScenario.launch(HomeLauncherActivity::class.java)
        compose.waitForIdle()
    }

    @After fun closeHome() { scenario.close() }

    private fun editHome() { compose.onNodeWithText("Edit home").performClick() }

    private fun lockToggle() = compose.onNodeWithContentDescription("Lock layout toggle")

    @Test fun homeLayoutControlsPersistAcrossRecreation() {
        editHome()
        compose.onNodeWithText("App labels").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add page").performScrollTo().performClick()
        compose.onNodeWithText("List").performScrollTo().performClick()
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle {
            val saved = LauncherLayoutStore.read(preferences)
            assertEquals(2, saved.pages.size)
            assertEquals("List", saved.drawerStyle)
        }
        scenario.recreate()
        compose.waitForIdle()
        editHome()
        compose.onNodeWithText("• List").performScrollTo().assertIsDisplayed()
        lockToggle().performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("Add page").assertDoesNotExist()
        compose.onNodeWithText("Done").performClick()
        scenario.recreate()
        compose.waitForIdle()
        editHome()
        lockToggle().assertIsOn()
        compose.runOnIdle { assertTrue(LauncherLayoutStore.read(preferences).locked) }
        compose.onNodeWithText("Done").performClick()
    }

    @Test fun drawerSearchHandlesNoResultsAndReturnsHome() {
        compose.onNodeWithText("All apps").performClick()
        compose.onNodeWithText("Search apps").performTextInput("keptee_nonexistent_app_817364")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("No apps match that search.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("No apps match that search.").assertIsDisplayed()
        compose.onNodeWithText("Back to Home").performClick()
        compose.onNodeWithText("All apps").assertIsDisplayed()
        compose.onNodeWithText("Search apps").assertDoesNotExist()
    }
}
