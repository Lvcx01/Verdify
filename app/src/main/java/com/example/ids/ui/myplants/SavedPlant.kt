package com.example.ids.ui.myplants

data class SavedPlant(
    val commonName: String,
    val scientificName: String,
    val accuracy: String,
    val imagePath: String? = null,
    var wateringFrequency: Int = 7,
    var lastWatered: Long = 0L
)