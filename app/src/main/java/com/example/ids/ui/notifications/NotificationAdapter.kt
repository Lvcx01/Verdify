package com.example.ids.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ids.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(private val list: List<AppNotification>) :
    RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder>() {

    class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notifTitle)
        val body: TextView = view.findViewById(R.id.notifBody)
        val date: TextView = view.findViewById(R.id.notifDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val item = list[position]

        holder.title.text = item.title
        holder.body.text = item.message
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateString = sdf.format(Date(item.timestamp))
            holder.date.text = dateString
        } catch (e: Exception) {
            holder.date.text = "Recently"
        }
    }

    override fun getItemCount() = list.size
}