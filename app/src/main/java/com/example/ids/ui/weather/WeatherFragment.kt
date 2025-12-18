package com.example.ids.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ids.R
import com.example.ids.databinding.FragmentWeatherBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getCurrentLocationWeather()
            } else {
                Toast.makeText(context, "Permission denied. Showing default.", Toast.LENGTH_SHORT).show()
                fetchWeatherData(41.9028, 12.4964)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.recyclerForecast.layoutManager = LinearLayoutManager(context)

        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)
        binding.tvDate.text = dateFormat.format(Date()).uppercase()

        setDarkThemeColors(true)

        checkPermissionsAndLoad()

        return binding.root
    }

    private fun checkPermissionsAndLoad() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getCurrentLocationWeather()
        }
    }

    private fun getCurrentLocationWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        binding.tvLocation.text = "Locating..."

        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeatherData(location.latitude, location.longitude)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) fetchWeatherData(lastLoc.latitude, lastLoc.longitude)
                        else fetchWeatherData(41.9028, 12.4964)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "GPS Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val current = withContext(Dispatchers.IO) {
                    WeatherRepository.getWeatherData(requireContext(), lat, lon)
                }
                updateCurrentUI(current)

                val forecast = WeatherRetrofitInstance.api.getForecast(lat, lon)
                updateForecastList(forecast.list)

            } catch (e: Exception) {
                android.util.Log.e("WeatherFragment", "Errore recupero meteo: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Impossibile aggiornare il meteo. Controlla la connessione.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateCurrentUI(data: WeatherResponse) {
        if (_binding == null) return

        binding.tvLocation.text = "📍 ${data.name}"
        binding.tvTemperature.text = "${data.main.temp.toInt()}°"

        val desc = data.weather.firstOrNull()?.description ?: ""
        binding.tvCondition.text = desc.replaceFirstChar { it.uppercase() }

        binding.tvHumidity.text = "${data.main.humidity}%"
        val windKmh = (data.wind.speed * 3.6).toInt()
        binding.tvWind.text = "$windKmh km/h"

        val iconCode = data.weather.firstOrNull()?.icon ?: "01d"
        val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"
        Glide.with(this).load(iconUrl).into(binding.imgWeatherIcon)

        val conditionId = data.weather.firstOrNull()?.id ?: 800
        updateBackgroundTheme(conditionId)
    }

    private fun updateForecastList(fullList: List<ForecastItem>) {
        if (_binding == null) return
        val dailyList = fullList.filter { it.dt_txt.contains("12:00:00") }
        val displayList = dailyList.ifEmpty { fullList.take(5) }
        val adapter = ForecastAdapter(displayList)
        binding.recyclerForecast.adapter = adapter
    }

    private fun updateBackgroundTheme(conditionId: Int) {
        val backgroundView = binding.weatherScroll

        when (conditionId) {
            in 200..232 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_thunder)
                setDarkThemeColors(false)
                binding.tvSuggestionTitle.text = "Storm Warning"
                binding.tvSuggestionDesc.text = "Keep delicate plants indoors."
            }
            in 300..321 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_drizzle)
                setDarkThemeColors(true)
                binding.tvSuggestionTitle.text = "Light Drizzle"
                binding.tvSuggestionDesc.text = "No need to water today."
            }
            in 500..531 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_rainy)
                setDarkThemeColors(false)
                binding.tvSuggestionTitle.text = "Rainy Day"
                binding.tvSuggestionDesc.text = "Collect rainwater if you can!"
            }
            in 600..622 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_snow)
                setDarkThemeColors(true)
                binding.tvSuggestionTitle.text = "Snow Falling"
                binding.tvSuggestionDesc.text = "Protect roots from frost."
            }
            in 701..781 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_fog)
                setDarkThemeColors(true)
                binding.tvSuggestionTitle.text = "Foggy"
                binding.tvSuggestionDesc.text = "High humidity levels."
            }
            800 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_sunny)
                setDarkThemeColors(true)
                binding.tvSuggestionTitle.text = "All Clear"
                binding.tvSuggestionDesc.text = "Perfect conditions for gardening."
            }
            801 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_sunny)
                setDarkThemeColors(true)
                binding.tvSuggestionTitle.text = "Mostly Sunny"
                binding.tvSuggestionDesc.text = "Great day for outdoor work."
            }
            802 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_cloudy)
                setDarkThemeColors(true)
                binding.tvSuggestionTitle.text = "Partly Cloudy"
                binding.tvSuggestionDesc.text = "Good for planting, not too hot."
            }
            803, 804 -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_overcast)
                setDarkThemeColors(false)
                binding.tvSuggestionTitle.text = "Overcast"
                binding.tvSuggestionDesc.text = "Low light conditions."
            }
            else -> {
                backgroundView.setBackgroundResource(R.drawable.bg_weather_sunny)
                setDarkThemeColors(true)
            }
        }
    }

    private fun setDarkThemeColors(isDarkText: Boolean) {
        val color = if (isDarkText) 0xFF333333.toInt() else 0xFFFFFFFF.toInt()
        val colorSecondary = if (isDarkText) 0xFF555555.toInt() else 0xFFDDDDDD.toInt()

        binding.tvDate.setTextColor(color)
        binding.tvCondition.setTextColor(color)
        binding.tvTemperature.setTextColor(color)
        binding.tvForecastTitle.setTextColor(color)
        binding.tvHumidity.setTextColor(color)
        binding.tvWind.setTextColor(color)
        binding.tvSuggestionTitle.setTextColor(color)

        binding.tvSuggestionDesc.setTextColor(colorSecondary)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}