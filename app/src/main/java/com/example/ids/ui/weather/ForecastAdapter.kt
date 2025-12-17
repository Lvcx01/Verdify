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

class ForecastAdapter(private val items: List<ForecastItem>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.itemDay)
        val tvDesc: TextView = view.findViewById(R.id.itemDesc) // Descrizione
        val tvTemp: TextView = view.findViewById(R.id.itemTemp)
        val imgIcon: ImageView = view.findViewById(R.id.itemIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val item = items[position]

        try {
            val inFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val outFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
            val date = inFormat.parse(item.dt_txt)
            holder.tvDay.text = if (date != null) outFormat.format(date) else ""
        } catch (e: Exception) {
            holder.tvDay.text = ""
        }

        val description = item.weather.firstOrNull()?.description ?: ""
        holder.tvDesc.text = description.split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        holder.tvTemp.text = "${item.main.temp.toInt()}°"
        val iconCode = item.weather.firstOrNull()?.icon ?: "01d"
        val url = "https://openweathermap.org/img/w/$iconCode.png"
        Glide.with(holder.itemView.context).load(url).into(holder.imgIcon)
    }
    override fun getItemCount() = items.size
}