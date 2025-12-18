package com.example.ids.ui.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import com.example.ids.ui.weather.WeatherRetrofitInstance
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GardeningWorkerTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)

        every { context.applicationContext } returns context
        every { context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE) } returns sharedPrefs

        mockkObject(PlantManager)
        mockkObject(NotificationHelper)
        mockkObject(WeatherRetrofitInstance)

        every { NotificationHelper.sendNotification(any(), any(), any()) } just Runs
        every { PlantManager.loadPlants(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testNotificaInnaffiatura_InviataSePiantaAssetata() = runTest {
        every { sharedPrefs.getBoolean("notifications_enabled", true) } returns true
        every { sharedPrefs.getBoolean("notif_care", true) } returns true

        val piantaAssetata = SavedPlant(
            commonName = "Basilico",
            scientificName = "Ocimum basilicum",
            accuracy = "98%",
            lastWatered = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 5),
            wateringFrequency = 3
        )
        PlantManager.plants.clear()
        PlantManager.plants.add(piantaAssetata)

        val worker = TestListenableWorkerBuilder<GardeningWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        verify {
            NotificationHelper.sendNotification(
                any(),
                eq("Verdify Reminder 💧"),
                match { it.contains("Basilico") }
            )
        }
    }
}