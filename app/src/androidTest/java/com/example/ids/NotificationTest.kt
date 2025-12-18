package com.example.ids

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.ids.ui.notifications.NotificationHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun testSendNotification_RunsWithoutCrashing() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        NotificationHelper.sendNotification(
            appContext,
            "TEST AUTOMATICO",
            "Se leggi questo, il test funziona!"
        )
    }
}