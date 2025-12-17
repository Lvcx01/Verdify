package com.example.ids.ui.myplants

data class SavedPlant(
    val commonName: String,
    val scientificName: String,
    val accuracy: String,
    val imagePath: String? = null
)