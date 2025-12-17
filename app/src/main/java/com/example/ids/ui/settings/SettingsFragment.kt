package com.example.ids.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ids.databinding.FragmentSettingsBinding
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import com.example.ids.ui.notifications.GardeningWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import com.example.ids.ui.weather.WeatherCheckWorker
data class BackupItem(
    val commonName: String,
    val scientificName: String,
    val accuracy: String,
    val imageBase64: String?
)

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Notifications Allowed!", Toast.LENGTH_SHORT).show()
        } else {
            binding.switchNotifications.isChecked = false // Se nega, spegni tutto
        }
    }

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) performImport(uri)
    }
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) performExport(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        setupButtons()
        loadPreferences()

        return binding.root
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnExport.setOnClickListener { createDocumentLauncher.launch("plant_backup.json") }
        binding.btnImport.setOnClickListener { importFileLauncher.launch("application/json") }

        val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            updateControlsState(isChecked)
            val weatherEnabled = prefs.getBoolean("notif_weather", false)
            updateWorkers(isChecked, weatherEnabled)

            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        binding.checkWeather.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_weather", isChecked).apply()

            val masterEnabled = binding.switchNotifications.isChecked
            updateWorkers(masterEnabled, isChecked)
        }

        binding.checkCare.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_care", isChecked).apply()
        }
    }

    private fun updateControlsState(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        binding.checkWeather.isEnabled = enabled
        binding.checkCare.isEnabled = enabled
    }

    private fun updateWorkers(enableAll: Boolean, enableWeather: Boolean) {
        val workManager = WorkManager.getInstance(requireContext())

        if (enableAll) {
            val dailyRequest = PeriodicWorkRequestBuilder<GardeningWorker>(24, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(
                "VerdifyDailyCare",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyRequest
            )
        } else {
            workManager.cancelUniqueWork("VerdifyDailyCare")
        }

        if (enableAll && enableWeather) {
            val weatherRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(15, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "VerdifyWeatherWatch",
                ExistingPeriodicWorkPolicy.UPDATE, // UPDATE riavvia il timer se cambiano le settings
                weatherRequest
            )
        } else {
            workManager.cancelUniqueWork("VerdifyWeatherWatch")
        }
    }

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val allEnabled = prefs.getBoolean("notifications_enabled", false)

        binding.switchNotifications.isChecked = allEnabled
        binding.checkWeather.isChecked = prefs.getBoolean("notif_weather", false)
        binding.checkCare.isChecked = prefs.getBoolean("notif_care", false)

        updateControlsState(allEnabled)
    }

    private fun scheduleNotifications() {
        val workRequest = PeriodicWorkRequestBuilder<GardeningWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "VerdifyDailyWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun performExport(uri: Uri) {
        binding.btnExport.isEnabled = false
        binding.btnExport.text = "Salvataggio..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupList = mutableListOf<BackupItem>()
                for (plant in PlantManager.plants) {
                    var base64Image: String? = null
                    if (plant.imagePath != null) {
                        val file = File(plant.imagePath)
                        if (file.exists()) {
                            val bytes = file.readBytes()
                            base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                        }
                    }
                    backupList.add(BackupItem(plant.commonName, plant.scientificName, plant.accuracy, base64Image))
                }
                val jsonString = Gson().toJson(backupList)
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup completato!", Toast.LENGTH_SHORT).show()
                    binding.btnExport.isEnabled = true
                    binding.btnExport.text = "Export Data"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnExport.isEnabled = true
                    binding.btnExport.text = "Export Data"
                }
            }
        }
    }

    private fun performImport(uri: Uri) {
        binding.btnImport.isEnabled = false
        binding.btnImport.text = "Importazione..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val reader = InputStreamReader(inputStream)
                val listType = object : TypeToken<List<BackupItem>>() {}.type
                val backupList: List<BackupItem> = Gson().fromJson(reader, listType)

                val newPlants = mutableListOf<SavedPlant>()

                for (item in backupList) {
                    var imagePath: String? = null
                    if (item.imageBase64 != null) {
                        try {
                            val imageBytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (bitmap != null) {
                                imagePath = PlantManager.saveImageToStorage(requireContext(), bitmap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    newPlants.add(SavedPlant(item.commonName, item.scientificName, item.accuracy, imagePath))
                }

                withContext(Dispatchers.Main) {
                    PlantManager.plants.clear()
                    PlantManager.plants.addAll(newPlants)
                    PlantManager.savePlants(requireContext())

                    Toast.makeText(context, "Dati importati correttamente!", Toast.LENGTH_SHORT).show()
                    binding.btnImport.isEnabled = true
                    binding.btnImport.text = "Import Data"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "File non valido: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnImport.isEnabled = true
                    binding.btnImport.text = "Import Data"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}