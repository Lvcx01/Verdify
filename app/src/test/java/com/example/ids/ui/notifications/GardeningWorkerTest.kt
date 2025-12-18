package com.example.ids.ui.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import com.example.ids.ui.weather.OpenWeatherMapApi
import com.example.ids.ui.weather.WeatherResponse
import com.example.ids.ui.weather.WeatherRetrofitInstance
import com.example.ids.ui.weather.Main
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

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
        mockkObject(WeatherRetrofitInstance)
        mockkObject(NotificationHelper)

        every { PlantManager.loadPlants(any()) } just Runs
        PlantManager.plants.clear()

        every { NotificationHelper.sendNotification(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `testNotificaInnaffiatura_InviataSePiantaAssetata`() = runTest {
        every { sharedPrefs.getBoolean("notifications_enabled", false) } returns true
        every { sharedPrefs.getBoolean("notif_care", false) } returns true

        val piantaAssetata = SavedPlant(
            commonName = "Basilico",
            scientificName = "Ocimum basilicum",
            accuracy = "98%",
            wateringFrequency = 7,
            lastWatered = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)
        )
        PlantManager.plants.add(piantaAssetata)

        val worker = TestListenableWorkerBuilder<GardeningWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        verify {
            NotificationHelper.sendNotification(
                any(),
                "Verdify Reminder 💧",
                match { it.contains("Basilico") }
            )
        }
    }

    @Test
    fun `testNotificaMeteo_InviataSeGelo`() = runTest {
        every { sharedPrefs.getBoolean("notifications_enabled", false) } returns true
        every { sharedPrefs.getBoolean("notif_weather", false) } returns true
        every { sharedPrefs.getString("saved_lat", null) } returns "45.0"
        every { sharedPrefs.getString("saved_lon", null) } returns "10.0"

        val mockApi = mockk<OpenWeatherMapApi>()
        val mockResponse = mockk<WeatherResponse>(relaxed = true)
        every { mockResponse.main } returns Main(temp = 2.0, 0.0, 50, 0.0, 0.0)

        coEvery { mockApi.getCurrentWeather(any(), any(), any(), any(), any()) } returns mockResponse
        every { WeatherRetrofitInstance.api } returns mockApi

        val worker = TestListenableWorkerBuilder<GardeningWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        verify {
            NotificationHelper.sendNotification(
                any(),
                match { it.contains("Gelo") },
                any()
            )
        }
    }
}