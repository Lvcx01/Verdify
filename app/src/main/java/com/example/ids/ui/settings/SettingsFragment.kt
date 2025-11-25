package com.example.ids.ui.settings

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.ids.databinding.FragmentSettingsBinding
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// Classe dati per il backup
data class BackupItem(
    val commonName: String,
    val scientificName: String,
    val accuracy: String,
    val imageBase64: String?
)

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // 1. LAUNCHER PER L'IMPORT (Scegli file da leggere)
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            performImport(uri)
        }
    }

    // 2. NUOVO LAUNCHER PER L'EXPORT (Scegli dove salvare il file)
    // Questo apre la finestra "Salva" di sistema
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            // Se l'utente ha scelto una cartella e confermato, procediamo alla scrittura
            performExport(uri)
        }
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
        // EXPORT: Lancia la richiesta di creazione file
        binding.btnExport.setOnClickListener {
            // "plant_backup.json" è il nome suggerito, l'utente può cambiarlo
            createDocumentLauncher.launch("plant_backup.json")
        }

        // IMPORT: Lancia la richiesta di apertura file
        binding.btnImport.setOnClickListener {
            importFileLauncher.launch("application/json")
        }

        // --- Switch Notifiche ---
        val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            binding.checkWeather.isEnabled = isChecked
            binding.checkCare.isEnabled = isChecked
        }
        binding.checkWeather.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_weather", isChecked).apply()
        }
        binding.checkCare.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_care", isChecked).apply()
        }
    }

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val allEnabled = prefs.getBoolean("notifications_enabled", true)
        binding.switchNotifications.isChecked = allEnabled
        binding.checkWeather.isChecked = prefs.getBoolean("notif_weather", true)
        binding.checkCare.isChecked = prefs.getBoolean("notif_care", true)
        binding.checkWeather.isEnabled = allEnabled
        binding.checkCare.isEnabled = allEnabled
    }

    // --- LOGICA EXPORT (Scrittura su URI locale) ---
    private fun performExport(uri: Uri) {
        binding.btnExport.isEnabled = false
        binding.btnExport.text = "Salvataggio..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupList = mutableListOf<BackupItem>()

                // Conversione Immagini -> Base64
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

                // Creazione JSON
                val jsonString = Gson().toJson(backupList)

                // SCRITTURA DIRETTA SUL FILE SCELTO DALL'UTENTE
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup salvato con successo!", Toast.LENGTH_LONG).show()
                    binding.btnExport.isEnabled = true
                    binding.btnExport.text = "Esporta Dati (Backup)"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore salvataggio: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnExport.isEnabled = true
                    binding.btnExport.text = "Esporta Dati (Backup)"
                }
            }
        }
    }

    // --- LOGICA IMPORT (Lettura da URI locale) ---
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
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
                    binding.btnImport.text = "Importa Dati"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "File non valido: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnImport.isEnabled = true
                    binding.btnImport.text = "Importa Dati"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}