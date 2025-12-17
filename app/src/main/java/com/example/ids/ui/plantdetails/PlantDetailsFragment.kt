package com.example.ids.ui.plantdetails

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ids.databinding.FragmentPlantDetailsBinding
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import com.example.ids.ui.weather.WeatherRetrofitInstance
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDetailsFragment : Fragment() {

    private var _binding: FragmentPlantDetailsBinding? = null
    private val binding get() = _binding!!

    private var currentPlant: SavedPlant? = null
    private var isDataFetched = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlantDetailsBinding.inflate(inflater, container, false)

        val commonName = arguments?.getString("commonName") ?: "Unknown"
        val scientificName = arguments?.getString("scientificName") ?: "Unknown"
        val imagePath = arguments?.getString("imagePath")

        currentPlant = PlantManager.plants.find { it.commonName == commonName }

        if (currentPlant == null) {
            Toast.makeText(context, "Errore: Pianta non trovata nel database", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return binding.root
        }

        updateWateringUI()

        binding.detailCommonName.text = commonName
        binding.detailScientificName.text = scientificName

        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.detailImage.setImageBitmap(bitmap)
            }
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnDeletePlant.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Rimuovi Pianta")
                .setMessage("Sei sicuro?")
                .setPositiveButton("Sì") { _, _ ->
                    PlantManager.deletePlant(requireContext(), commonName, scientificName, imagePath)
                    findNavController().navigateUp()
                }
                .setNegativeButton("No", null)
                .show()
        }

        binding.btnWaterDone.setOnClickListener {
            onWateredClicked()
        }

        binding.btnAskAi.setOnClickListener {
            fetchWeatherAndAskGemini()
        }

        return binding.root
    }

    private fun updateWateringUI() {
        val plant = currentPlant ?: return
        val colorGreenFilled = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.ids.R.color.gardener_green)
        val colorGreenText = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.ids.R.color.gardener_dark_green)
        val colorLightBg = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.ids.R.color.icon_bg_light)
        val colorWhite = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.ids.R.color.white)

        val daysSince = calculateDaysSinceWatering(plant.lastWatered)
        val frequency = if (plant.wateringFrequency <= 0) 7 else plant.wateringFrequency

        if (plant.lastWatered != 0L && daysSince >= frequency) {
            binding.btnWaterDone.text = "Annaffia Ora 💧"
            binding.btnWaterDone.setBackgroundColor(colorGreenFilled)
            binding.btnWaterDone.setTextColor(colorWhite)
            binding.btnWaterDone.iconTint = android.content.res.ColorStateList.valueOf(colorWhite)
        } else {
            if (plant.lastWatered == 0L) {
                binding.btnWaterDone.text = "Mai annaffiata"
            } else {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateString = dateFormat.format(Date(plant.lastWatered))
                binding.btnWaterDone.text = "Annaffiata il: $dateString"
            }
            binding.btnWaterDone.setBackgroundColor(colorLightBg)
            binding.btnWaterDone.setTextColor(colorGreenText)
            binding.btnWaterDone.iconTint = android.content.res.ColorStateList.valueOf(colorGreenText)
        }
    }

    private fun fetchWeatherAndAskGemini() {
        if (isDataFetched) return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvAiResponse.text = "Sto leggendo i sensori ambientali..."
        binding.btnAskAi.isEnabled = false

        isDataFetched = true

        val dateFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val defaultReport = "Dati meteo non disponibili (No GPS). Assumo condizioni standard indoor: 20°C."
            askAiWithLogic(defaultReport, todayDate)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val cancellationToken = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    lifecycleScope.launch {
                        try {
                            val data = WeatherRetrofitInstance.api.getCurrentWeather(
                                location.latitude, location.longitude, lang = "it"
                            )
                            val report = """
                                - Condizione: ${data.weather.firstOrNull()?.description}
                                - Temperatura: ${data.main.temp}°C
                                - Umidità: ${data.main.humidity}%
                                - Vento: ${data.wind.speed} km/h
                            """.trimIndent()

                            askAiWithLogic(report, todayDate)

                        } catch (_: Exception) {
                            askAiWithLogic("Errore Meteo. Uso dati standard.", todayDate)
                        }
                    }
                } else {
                    askAiWithLogic("Posizione non trovata. Uso dati standard.", todayDate)
                }
            }
            .addOnFailureListener {
                askAiWithLogic("Errore GPS. Uso dati standard.", todayDate)
            }
    }

    private fun askAiWithLogic(weatherReport: String, dateString: String) {
        val plant = currentPlant ?: return

        lifecycleScope.launch {
            binding.tvAiResponse.text = "Analisi botanica in corso..."

            val daysSinceWatering = calculateDaysSinceWatering(plant.lastWatered)
            val fullResponse = PlantCareAI.askForCareTips(
                plantName = plant.commonName,
                weatherReport = weatherReport,
                currentDate = dateString,
                daysSinceWatering = daysSinceWatering
            )

            val regex = Regex("\\[FREQ:\\s*(\\d+)]")
            val match = regex.find(fullResponse)

            var finalMessage = fullResponse

            if (match != null) {
                val daysString = match.groupValues[1]
                val newFrequency = daysString.toIntOrNull() ?: 7

                plant.wateringFrequency = newFrequency
                PlantManager.savePlants(requireContext())

                Log.d("PlantApp", "Nuova frequenza per ${plant.commonName}: $newFrequency giorni")

                finalMessage = fullResponse.replace(match.value, "").trim()
            }

            if (_binding != null) {
                binding.progressBar.visibility = View.GONE
                binding.tvAiResponse.text = finalMessage
                binding.btnAskAi.isEnabled = true
                isDataFetched = false
            }
        }
    }

    private fun onWateredClicked() {
        val plant = currentPlant ?: return

        plant.lastWatered = System.currentTimeMillis()
        PlantManager.savePlants(requireContext())

        Toast.makeText(context, "Pianta innaffiata! Timer resettato.", Toast.LENGTH_SHORT).show()
        updateWateringUI()
    }

    private fun calculateDaysSinceWatering(lastWateredTime: Long): Int {
        if (lastWateredTime == 0L) return 999
        val diff = System.currentTimeMillis() - lastWateredTime
        return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}