package com.example.ids.ui.plantdetails

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ids.databinding.FragmentPlantDetailsBinding
import com.example.ids.ui.myplants.PlantManager
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
    private var isDataFetched = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlantDetailsBinding.inflate(inflater, container, false)

        val commonName = arguments?.getString("commonName") ?: "Unknown"
        val scientificName = arguments?.getString("scientificName") ?: "Unknown"
        val imagePath = arguments?.getString("imagePath")

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
                .setTitle("Remove Plant")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    PlantManager.deletePlant(requireContext(), commonName, scientificName, imagePath)
                    findNavController().navigateUp()
                }
                .setNegativeButton("No", null)
                .show()
        }
        fetchWeatherAndAskGemini(commonName)

        return binding.root
    }

    private fun fetchWeatherAndAskGemini(plantName: String) {
        if(isDataFetched) return
        binding.tvGeminiTips.text = "Gathering environmental data..."
        isDataFetched = true;

        val dateFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            val defaultReport = "Dati meteo non disponibili (No GPS). Assumo condizioni standard indoor: 20°C, No Vento, Luce indiretta."
            askGeminiWithReport(plantName, defaultReport, todayDate)
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
                                - Nuvolosità: ${data.clouds.all}%
                            """.trimIndent()

                            askGeminiWithReport(plantName, report, todayDate)

                        } catch (e: Exception) {
                            askGeminiWithReport(plantName, "Errore Meteo. Considera condizioni standard.", todayDate)
                        }
                    }
                } else {
                    askGeminiWithReport(plantName, "Posizione non trovata. Considera condizioni standard.", todayDate)
                }
            }
            .addOnFailureListener {
                askGeminiWithReport(plantName, "Errore GPS. Considera condizioni standard.", todayDate)
            }
    }

    private fun askGeminiWithReport(plant: String, report: String, date: String) {
        lifecycleScope.launch {
            if (_binding != null) {
                binding.tvGeminiTips.text = "Analyzing pruning season & weather..."
            }
            val tips = PlantCareAI.askForCareTips(plant, report, date)

            if (_binding != null) {
                binding.tvGeminiTips.text = tips
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}