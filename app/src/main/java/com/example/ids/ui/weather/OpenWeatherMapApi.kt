package com.example.ids.ui.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = com.example.ids.BuildConfig.OPENWEATHER_API_KEY,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "it"
    ): WeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = com.example.ids.BuildConfig.OPENWEATHER_API_KEY,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "it"
    ): ForecastResponse
}

data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Long
)

data class ForecastResponse(val list: List<ForecastItem>)

data class ForecastItem(
    val dt: Long,
    val dt_txt: String,
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int,
    val temp_min: Double,
    val temp_max: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double
)

data class Clouds(
    val all: Int
)