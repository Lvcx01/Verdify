package com.example.ids.ui.weather

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.ids.ui.notifications.NotificationHelper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WeatherCheckWorkerTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)

        every { context.applicationContext } returns context
        every { context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        mockkObject(WeatherRetrofitInstance)
        mockkObject(WeatherRepository)
        mockkObject(NotificationHelper)

        every { WeatherRepository.saveToCache(any(), any()) } just Runs
        every { NotificationHelper.sendNotification(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testMeteo_TempestaInviaNotifica() = runTest {
        every { sharedPrefs.getBoolean("notifications_enabled", true) } returns true
        every { sharedPrefs.getBoolean("notif_weather", true) } returns true

        every { sharedPrefs.getString("saved_lat", null) } returns "45.0"
        every { sharedPrefs.getString("saved_lon", null) } returns "11.0"
        every { sharedPrefs.getInt("last_weather_id", -1) } returns -1
        every { sharedPrefs.getString("last_weather_date", "") } returns "2023-01-01"

        val mockApi = mockk<OpenWeatherMapApi>()
        val mockResponse = mockk<WeatherResponse>(relaxed = true)

        val weatherInfo = Weather(id = 200, main = "Thunderstorm", description = "heavy storm", icon = "11d")
        every { mockResponse.weather } returns listOf(weatherInfo)
        every { mockResponse.main } returns Main(temp = 20.0, 0.0, 80, 0.0, 0.0)

        coEvery { mockApi.getCurrentWeather(any(), any(), any(), any(), any()) } returns mockResponse
        every { WeatherRetrofitInstance.api } returns mockApi

        val worker = TestListenableWorkerBuilder<WeatherCheckWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        verify {
            NotificationHelper.sendNotification(
                any(),
                match { it.contains("Storm") },
                any()
            )
        }
    }

    @Test
    fun testMeteo_GeloInviaNotificaPrioritaria() = runTest {
        every { sharedPrefs.getBoolean("notifications_enabled", true) } returns true
        every { sharedPrefs.getBoolean("notif_weather", true) } returns true

        every { sharedPrefs.getString("saved_lat", null) } returns "45.0"
        every { sharedPrefs.getString("saved_lon", null) } returns "11.0"

        val mockApi = mockk<OpenWeatherMapApi>()
        val mockResponse = mockk<WeatherResponse>(relaxed = true)

        val weatherInfo = Weather(id = 800, main = "Clear", description = "clear sky", icon = "01d")
        every { mockResponse.weather } returns listOf(weatherInfo)
        every { mockResponse.main } returns Main(temp = 1.0, 0.0, 40, 0.0, 0.0)

        coEvery { mockApi.getCurrentWeather(any(), any(), any(), any(), any()) } returns mockResponse
        every { WeatherRetrofitInstance.api } returns mockApi

        val worker = TestListenableWorkerBuilder<WeatherCheckWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        verify {
            NotificationHelper.sendNotification(
                any(),
                match { it.contains("Frost") },
                any()
            )
        }
    }
}