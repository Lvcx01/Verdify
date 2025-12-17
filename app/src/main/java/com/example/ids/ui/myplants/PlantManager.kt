package com.example.ids.ui.myplants

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PlantManager {
    private const val FILE_NAME = "my_plants.json"
    val plants = mutableListOf<SavedPlant>()

    fun savePlants(context: Context) {
        val gson = Gson()
        val jsonString = gson.toJson(plants)

        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }
    }

    fun deletePlant(context: Context, commonName: String, scientificName: String, imagePath: String?) {
        val wasRemoved = plants.removeAll { plant ->
            plant.commonName == commonName &&
                    plant.scientificName == scientificName &&
                    plant.imagePath == imagePath
        }

        if (wasRemoved) {
            savePlants(context)

            if (imagePath != null) {
                try {
                    val file = java.io.File(imagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadPlants(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return

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

    fun saveImageToStorage(context: Context, bitmap: android.graphics.Bitmap): String {
        val filename = "plant_${System.currentTimeMillis()}.jpg"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return File(context.filesDir, filename).absolutePath
    }
}