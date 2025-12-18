package com.example.ids.ui.weather

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ids.ui.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("notifications_enabled", true) ||
            !prefs.getBoolean("notif_weather", true)) {
            return Result.success()
        }

        val latStr = prefs.getString("saved_lat", null)
        val lonStr = prefs.getString("saved_lon", null)

        if (latStr == null || lonStr == null) {
            Log.e("WeatherWorker", "Nessuna posizione salvata. Apro l'app per aggiornare.")
            return Result.failure()
        }

        return try {
            val response = WeatherRetrofitInstance.api.getCurrentWeather(
                latStr.toDouble(),
                lonStr.toDouble(),
                lang = "it"
            )

            WeatherRepository.saveToCache(applicationContext, response)

            val weatherId = response.weather.firstOrNull()?.id ?: 800
            val weatherDesc = response.weather.firstOrNull()?.description ?: ""

            checkAndNotify(weatherId, weatherDesc, response.main.temp, prefs)

            Result.success()

        } catch (e: Exception) {
            Log.e("WeatherWorker", "Errore worker: ${e.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun checkAndNotify(id: Int, desc: String, temp: Double, prefs: SharedPreferences) {
        val lastNotifiedId = prefs.getInt("last_weather_id", -1)
        val lastNotifiedDate = prefs.getString("last_weather_date", "")

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val isNewDay = lastNotifiedDate != todayDate

        if (id >= 800) {
            prefs.edit().putInt("last_weather_id", 800).apply()
        }

        var title = ""
        var message = ""
        var shouldNotify = false

        if (isNewDay || id != lastNotifiedId) {
            when (id) {
                in 200..232 -> {
                    title = "⛈️ Storm Alert"
                    message = "Thunderstorm detected! Bring sensitive plants inside."
                    shouldNotify = true
                }
                in 500..504, in 520..531 -> {
                    title = "🌧️ Heavy Rain"
                    message = "Heavy rain ($desc) expected. Check drainage."
                    shouldNotify = true
                }
                in 600..622 -> {
                    title = "❄️ Snow Alert"
                    message = "Snow detected! Protect roots from frost."
                    shouldNotify = true
                }
            }
        }

        if (temp < 2.0 && lastNotifiedId != -999) {
            title = "🥶 Frost Warning"
            message = "Temperature dropped to ${temp.toInt()}°C! Cover your plants."

            NotificationHelper.sendNotification(applicationContext, title, message)
            prefs.edit().putInt("last_weather_id", -999).putString("last_weather_date", todayDate).apply()
            return
        }

        if (shouldNotify) {
            NotificationHelper.sendNotification(applicationContext, title, message)

            prefs.edit()
                .putInt("last_weather_id", id)
                .putString("last_weather_date", todayDate)
                .apply()
        }
    }
}