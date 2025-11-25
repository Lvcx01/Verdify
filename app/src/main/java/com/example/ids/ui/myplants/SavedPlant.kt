package com.example.ids.ui.myplants

data class SavedPlant(
    val commonName: String,
    val scientificName: String,
    val accuracy: String,
    val imagePath: String? = null // Nuovo campo: percorso del file sul telefono
)