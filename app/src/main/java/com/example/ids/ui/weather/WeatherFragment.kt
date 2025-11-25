package com.example.ids.ui.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.ids.databinding.FragmentWeatherBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var weatherRunnable: Runnable

    // Aggiorna ogni 15 minuti
    private val UPDATE_INTERVAL = 15 * 60 * 1000L

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getLocationAndUpdateWeather()
            } else {
                Toast.makeText(requireContext(), "Serve la posizione per il meteo locale", Toast.LENGTH_LONG).show()
                // Carica un meteo di default (es. Roma) se l'utente nega?
                // fetchWeatherData(41.9028, 12.4964)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.forecastRecyclerView.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Avvia il ciclo di aggiornamento
        weatherRunnable = Runnable {
            getLocationAndUpdateWeather()
            handler.postDelayed(weatherRunnable, UPDATE_INTERVAL)
        }
        handler.post(weatherRunnable)
    }

    private fun getLocationAndUpdateWeather() {
        // Controllo Permessi
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        // Provo a prendere l'ultima posizione nota
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchWeatherData(location.latitude, location.longitude)
            } else {
                // Se è null (succede spesso negli emulatori o se il GPS era spento), forziamo una richiesta
                requestNewLocationData()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        // Questa funzione forza il GPS a cercare dove siamo ORA
        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeatherData(location.latitude, location.longitude)
                } else {
                    Toast.makeText(requireContext(), "Attiva il GPS e apri Maps per un fix iniziale", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                // 1. Meteo Attuale
                val currentWeather = WeatherRetrofitInstance.api.getCurrentWeather(lat, lon)
                updateCurrentUI(currentWeather)

                // 2. Previsioni
                val forecastResponse = WeatherRetrofitInstance.api.getForecast(lat, lon)
                updateForecastUI(forecastResponse.list)

                // 3. Controllo Pericoli (Logica personalizzata)
                checkWeatherAlerts(forecastResponse.list)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore connessione: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun updateCurrentUI(data: WeatherResponse) {
        binding.cityName.text = data.name
        binding.temperature.text = "${data.main.temp.toInt()}°C"
        binding.weatherDescription.text = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""

        val iconCode = data.weather.firstOrNull()?.icon ?: "01d"
        val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"
        Glide.with(this).load(iconUrl).into(binding.weatherIcon)
    }

    private fun updateForecastUI(fullList: List<ForecastItem>) {
        val filteredList = mutableListOf<ForecastItem>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Filtriamo per avere solo le ore 12:00 dei prossimi giorni
        for (item in fullList) {
            if (item.dt_txt.contains("12:00:00") && !item.dt_txt.startsWith(today)) {
                filteredList.add(item)
            }
        }

        // Se la lista filtrata è vuota (es. sono le 13:00 e non ci sono le 12 di oggi), prendiamo i primi slot dei giorni successivi
        // Ma per semplicità usiamo quello che abbiamo trovato
        val adapter = ForecastAdapter(filteredList.take(3))
        binding.forecastRecyclerView.adapter = adapter
    }

    // --- LOGICA ALLERTE PERSONALIZZATA ---
    private fun checkWeatherAlerts(fullList: List<ForecastItem>) {
        var alertMessage = ""
        var hasAlert = false

        // Controlliamo tutte le fasce orarie dei prossimi giorni
        for (item in fullList) {
            val temp = item.main.temp
            val weatherId = try { item.weather[0].icon.substring(0, 2).toInt() } catch (e:Exception) { 0 } // Hack per capire il tipo grossolano
            val description = item.weather[0].description

            // 1. GELO
            if (temp <= 0) {
                alertMessage = "Allerta Gelo: proteggi le piante sensibili!"
                hasAlert = true
                break
            }
            // 2. CALDO ESTREMO
            if (temp >= 35) {
                alertMessage = "Allerta Caldo: innaffia abbondantemente!"
                hasAlert = true
                break
            }
            // 3. TEMPESTA/NEVE (Codici OpenWeather: 2xx Thunderstorm, 6xx Snow)
            // L'icona è tipo "11d". Prendiamo i primi due caratteri. "11" è thunderstorm, "13" è neve.
            if (item.weather[0].icon.startsWith("11")) {
                alertMessage = "Temporali in arrivo: metti al riparo le piante!"
                hasAlert = true
                break
            }
            if (item.weather[0].icon.startsWith("13")) {
                alertMessage = "Prevista Neve: copri le piante esterne!"
                hasAlert = true
                break
            }
        }

        if (hasAlert) {
            binding.warningCard.visibility = View.VISIBLE
            binding.warningText.text = alertMessage
        } else {
            binding.warningCard.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(weatherRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}