package com.example.ids.ui.weather

import android.content.Context
import com.google.gson.Gson

object WeatherRepository {

    private const val PREFS_NAME = "weather_cache"
    private const val KEY_CURRENT_DATA = "current_weather_json"
    private const val KEY_LAST_UPDATE = "last_update_timestamp"

    private const val CACHE_DURATION_MS = 15 * 60 * 1000

    suspend fun getWeatherData(context: Context, lat: Double, lon: Double): WeatherResponse {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastUpdate < CACHE_DURATION_MS) {
            val json = prefs.getString(KEY_CURRENT_DATA, null)
            if (json != null) {
                try {
                    android.util.Log.d("WeatherRepo", "Uso dati dalla CACHE (nessuna chiamata API)")
                    return Gson().fromJson(json, WeatherResponse::class.java)
                } catch (e: Exception) {
                    // Errore lettura cache, proseguiamo col network
                }
            }
        }

        android.util.Log.d("WeatherRepo", "Cache scaduta o assente. Chiamo API...")
        val response = WeatherRetrofitInstance.api.getCurrentWeather(lat, lon, lang = "it")

        saveToCache(context, response)

        return response
    }

    fun saveToCache(context: Context, data: WeatherResponse) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(data)

        prefs.edit()
            .putString(KEY_CURRENT_DATA, json)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()

        android.util.Log.d("WeatherRepo", "Dati salvati in Cache.")
    }
}