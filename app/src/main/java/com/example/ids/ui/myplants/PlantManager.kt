package com.example.ids.ui.myplants

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PlantManager {
    private const val FILE_NAME = "my_plants.json"
    val plants = mutableListOf<SavedPlant>()

    // Salva la lista su file JSON
    fun savePlants(context: Context) {
        val gson = Gson()
        val jsonString = gson.toJson(plants)

        // Scrive il file in modalità privata (solo la tua app può leggerlo)
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }
    }

    // Carica la lista dal file JSON
    fun loadPlants(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return // Se il file non esiste, non fare nulla (lista vuota)

        try {
            val jsonString = file.readText()
            val listType = object : TypeToken<MutableList<SavedPlant>>() {}.type
            val savedList: MutableList<SavedPlant> = Gson().fromJson(jsonString, listType)

            plants.clear()
            plants.addAll(savedList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Funzione helper per salvare l'immagine fisica
    fun saveImageToStorage(context: Context, bitmap: android.graphics.Bitmap): String {
        val filename = "plant_${System.currentTimeMillis()}.jpg"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        }
        // Ritorna il percorso assoluto del file
        return File(context.filesDir, filename).absolutePath
    }
}