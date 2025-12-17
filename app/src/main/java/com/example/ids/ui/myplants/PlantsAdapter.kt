package com.example.ids.ui.myplants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ids.R
import java.io.File

class PlantsAdapter(
    private val plants: List<SavedPlant>,
    private val onClick: (SavedPlant) -> Unit
) : RecyclerView.Adapter<PlantsAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.itemImage)
        val name: TextView = view.findViewById(R.id.itemName)
        val scientific: TextView = view.findViewById(R.id.itemScientific)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_card, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]

        holder.name.text = plant.commonName
        holder.scientific.text = plant.scientificName

        if (plant.imagePath != null) {
            val imgFile = File(plant.imagePath)
            if (imgFile.exists()) {
                val myBitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                holder.image.setImageBitmap(myBitmap)
            } else {
                holder.image.setImageResource(R.drawable.ic_plant_illustration)
            }
        } else {
            holder.image.setImageResource(R.drawable.ic_plant_illustration)
        }

        holder.itemView.setOnClickListener { onClick(plant) }
    }

    override fun getItemCount() = plants.size
}