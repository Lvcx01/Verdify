package com.example.ids.ui.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapApi {

    // Meteo Attuale (quello che avevi già)
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = "f01a686f585c6b3ca8c6323479220151",
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "it"
    ): WeatherResponse

    // NUOVO: Previsioni 5 giorni / 3 ore
    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = "f01a686f585c6b3ca8c6323479220151",
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "it"
    ): ForecastResponse
}

// --- CLASSI DATI PER IL METEO ATTUALE ---
data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double
)

data class Weather(
    val description: String,
    val icon: String
)

// --- CLASSI DATI PER LE PREVISIONI ---
data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long,        // Timestamp
    val dt_txt: String,  // Data leggibile es: "2023-11-25 12:00:00"
    val main: Main,
    val weather: List<Weather>
)