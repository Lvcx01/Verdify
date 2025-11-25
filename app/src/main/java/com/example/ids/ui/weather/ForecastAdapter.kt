package com.example.ids.ui.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ids.R
import java.text.SimpleDateFormat
import java.util.Locale

class ForecastAdapter(private val forecastList: List<ForecastItem>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.forecastDate)
        val tempText: TextView = view.findViewById(R.id.forecastTemp)
        val iconImage: ImageView = view.findViewById(R.id.forecastIcon)
        // NUOVO CAMPO
        val descText: TextView = view.findViewById(R.id.forecastDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val item = forecastList[position]

        // 1. Data
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE d", Locale.getDefault()) // Es: Lun 25
            val date = inputFormat.parse(item.dt_txt)
            holder.dateText.text = if (date != null) outputFormat.format(date).replaceFirstChar { it.uppercase() } else item.dt_txt
        } catch (e: Exception) {
            holder.dateText.text = item.dt_txt
        }

        // 2. Temperatura
        holder.tempText.text = "${item.main.temp.toInt()}°C"

        // 3. Icona
        val iconUrl = "https://openweathermap.org/img/w/${item.weather[0].icon}.png"
        Glide.with(holder.itemView.context).load(iconUrl).into(holder.iconImage)

        // 4. NUOVO: Descrizione (Prima lettera maiuscola)
        val description = item.weather[0].description
        holder.descText.text = description.replaceFirstChar { it.uppercase() }
    }

    override fun getItemCount() = forecastList.size
}