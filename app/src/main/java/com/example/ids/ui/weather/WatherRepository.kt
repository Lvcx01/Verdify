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
                    android.util.Log.d("WeatherRepo", "✅ Cache FRESCA trovata. Risparmio chiamata API.")
                    return Gson().fromJson(json, WeatherResponse::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("WeatherRepo", "Errore lettura cache fresca: ${e.message}")
                }
            }
        }
        android.util.Log.d("WeatherRepo", "🔄 Cache scaduta o assente. Chiamo API...")

        return try {
            val response = WeatherRetrofitInstance.api.getCurrentWeather(lat, lon, lang = "it")

            saveToCache(context, response)
            android.util.Log.d("WeatherRepo", "✅ Dati scaricati e salvati in cache.")

            response

        } catch (e: Exception) {
            android.util.Log.e("WeatherRepo", "❌ Errore API: ${e.message}. Tento recupero cache vecchia...")
            val oldJson = prefs.getString(KEY_CURRENT_DATA, null)

            if (oldJson != null) {
                android.util.Log.w("WeatherRepo", "⚠️ Uso dati SCADUTI come fallback per mancanza rete.")
                try {
                    return Gson().fromJson(oldJson, WeatherResponse::class.java)
                } catch (_: Exception) {
                    android.util.Log.e("WeatherRepo", "Anche la cache vecchia è corrotta.")
                    throw e
                }
            } else {
                throw e
            }
        }
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