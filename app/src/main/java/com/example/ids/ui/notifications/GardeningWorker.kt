package com.example.ids.ui.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.weather.WeatherRetrofitInstance
import java.util.concurrent.TimeUnit

class GardeningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", false)

        if (!notificationsEnabled) return Result.success()

        if (prefs.getBoolean("notif_care", false)) {
            try {
                PlantManager.loadPlants(context)

                val thirstyPlants = PlantManager.plants.filter { plant ->
                    if (plant.wateringFrequency <= 0) return@filter false

                    if (plant.lastWatered == 0L) return@filter true

                    val diff = System.currentTimeMillis() - plant.lastWatered
                    val daysPassed = TimeUnit.MILLISECONDS.toDays(diff)

                    daysPassed >= plant.wateringFrequency
                }

                if (thirstyPlants.isNotEmpty()) {
                    val plantNames = thirstyPlants.joinToString(", ") { it.commonName }
                    sendNotification(
                        "Verdify Reminder 💧",
                        "È ora di innaffiare: $plantNames"
                    )
                }
            } catch (e: Exception) {
                Log.e("GardeningWorker", "Errore controllo piante: ${e.message}")
            }
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
                            "❄️ Gelo in arrivo (${temp.toInt()}°C)",
                            "AZIONE RICHIESTA: Porta dentro le piante tropicali o coprile con tessuto non tessuto stasera!"
                        )
                    } else if (temp > 30.0) {
                        sendNotification(
                            "🔥 Ondata di Calore (${temp.toInt()}°C)",
                            "AZIONE RICHIESTA: Innaffia stasera dopo il tramonto ed evita potature stressanti."
                        )
                    }

                } catch (e: Exception) {
                    Log.e("GardeningWorker", "Errore Meteo Giornaliero: ${e.message}")
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(title: String, content: String) {
        NotificationHelper.sendNotification(applicationContext, title, content)
    }
}