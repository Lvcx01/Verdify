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
        // 1. Ottieni il contesto dell'app (come se fossi in una Activity)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // 2. Prova a mandare una notifica
        NotificationHelper.sendNotification(
            appContext,
            "TEST AUTOMATICO",
            "Se leggi questo, il test funziona!"
        )

        // Se il codice arriva qui senza crashare, il test è passato.
        // (In un test vero verificheresti anche se la notifica è apparsa, ma è molto complesso.
        // Per ora ci basta sapere che il NotificationHelper non esplode).
    }
}