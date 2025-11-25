package com.example.ids.ui.myplants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ids.R

class PlantsAdapter(
    private val plants: List<SavedPlant>,
    private val onClick: (SavedPlant) -> Unit // Funzione che scatta al click
) : RecyclerView.Adapter<PlantsAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.plantName)
        val scientific: TextView = view.findViewById(R.id.scientificName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.name.text = plant.commonName
        holder.scientific.text = plant.scientificName

        // CARICAMENTO IMMAGINE
        if (plant.imagePath != null) {
            val imgFile = java.io.File(plant.imagePath)
            if (imgFile.exists()) {
                val myBitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                // Assumi che l'ID della ImageView nel layout item_plant sia "plantImage"
                // Se non hai dato un ID alla ImageView in item_plant.xml, fallo ora!
                holder.itemView.findViewById<android.widget.ImageView>(R.id.plantImage).setImageBitmap(myBitmap)
            }
        } else {
            // Immagine di default se non ce n'è una
            holder.itemView.findViewById<android.widget.ImageView>(R.id.plantImage).setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener { onClick(plant) }
    }

    override fun getItemCount() = plants.size
}