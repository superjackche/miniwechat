package com.example.nearbychater

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the manifest application boots and completes dependency wiring. */
@RunWith(AndroidJUnit4::class)
class ApplicationStartupInstrumentedTest {
    @Test
    fun manifestApplicationInitializesDependencies() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val applicationInfo = targetContext.packageManager.getApplicationInfo(targetContext.packageName, 0)
        assertTrue(applicationInfo.className == NearbyChaterApplication::class.java.name)

        val application = targetContext.applicationContext as Application
        assertTrue(application is NearbyChaterApplication)
        val nearbyChaterApplication = application as NearbyChaterApplication
        assertNotNull(nearbyChaterApplication.logManager)
        assertNotNull(nearbyChaterApplication.nearbyChatService)
        assertNotNull(nearbyChaterApplication.chatRepository)
    }
}
