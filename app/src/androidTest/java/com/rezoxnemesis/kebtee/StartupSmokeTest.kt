package com.rezoxnemesis.kebtee

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-level smoke tests. These catch crashes that a successful APK compile cannot detect.
 */
@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {

    @Test
    fun customizationDashboardLaunchesAndRemainsAlive() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse("Dashboard unexpectedly finished during startup", activity.isFinishing)
                assertNotNull("Dashboard has no window", activity.window)
                assertNotNull("Dashboard has no content view", activity.findViewById(android.R.id.content))
            }
        }
    }

    @Test
    fun homeLauncherLaunchesAndRemainsAlive() {
        ActivityScenario.launch(HomeLauncherActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse("Home launcher unexpectedly finished during startup", activity.isFinishing)
                assertNotNull("Home launcher has no window", activity.window)
                assertNotNull("Home launcher has no content view", activity.findViewById(android.R.id.content))
            }
        }
    }

    @Test
    fun homeLauncherSurvivesBackgroundResumeAndRecreation() {
        ActivityScenario.launch(HomeLauncherActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertNotNull(activity.findViewById(android.R.id.content))
            }
        }
    }
}
