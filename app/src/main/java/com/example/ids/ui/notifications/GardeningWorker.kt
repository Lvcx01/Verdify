package com.example.ids.ui.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ids.ui.weather.WeatherRetrofitInstance

class GardeningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", false)

        if (!notificationsEnabled) return Result.success()

        if (prefs.getBoolean("notif_care", false)) {
            sendNotification("Verdify Care", "Remember to check your plants today! 🌿")
        }

        if (prefs.getBoolean("notif_weather", false)) {
            val latStr = prefs.getString("saved_lat", null)
            val lonStr = prefs.getString("saved_lon", null)

            if (latStr != null && lonStr != null) {
                try {
                    val response = WeatherRetrofitInstance.api.getCurrentWeather(
                        latStr.toDouble(),
                        lonStr.toDouble(),
                        lang = "it"
                    )

                    val temp = response.main.temp
                    if (temp < 5.0) {
                        sendNotification(
                            "🥶 Frost Alert",
                            "Temperature is low (${temp.toInt()}°C). Cover your sensitive plants tonight!"
                        )
                    } else if (temp > 30.0) {
                        sendNotification(
                            "☀️ Heat Warning",
                            "It's scorching (${temp.toInt()}°C)! Make sure plants have enough water."
                        )
                    }

                } catch (e: Exception) {
                    Log.e("GardeningWorker", "Errore Meteo Giornaliero: ${e.message}")
                }
            } else {
                Log.w("GardeningWorker", "Posizione non trovata per il meteo.")
            }
        }
        return Result.success()
    }

    private fun sendNotification(title: String, content: String) {
        NotificationHelper.sendNotification(applicationContext, title, content)
    }
}