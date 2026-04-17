package com.marineyachtradar.mayara.ui.smoke

import com.marineyachtradar.mayara.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test: verifies that MainActivity launches without crashing on the JVM
 * using Robolectric (no emulator required).
 *
 * This is the CI-friendly alternative to the instrumented smoke test in
 * src/androidTest/. It catches regressions introduced by Compose composition
 * or GL renderer changes that would crash the activity on startup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivitySmokeTest {

    @Test
    fun activityLaunchesWithoutCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .setup()          // create → start → resume
        val activity = controller.get()
        assertNotNull("MainActivity must launch without crashing", activity)
        controller.destroy()
    }
}
