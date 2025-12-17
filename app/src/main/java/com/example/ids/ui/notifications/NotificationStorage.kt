package com.example.ids.ui.notifications

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object NotificationStorage {
    private const val PREFS_NAME = "verdify_notifs"
    private const val KEY_HISTORY = "history"

    fun saveNotification(context: Context, title: String, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        val json = prefs.getString(KEY_HISTORY, "[]")
        val type = object : TypeToken<MutableList<AppNotification>>() {}.type
        val list: MutableList<AppNotification> = gson.fromJson(json, type) ?: mutableListOf()

        val newNotif = AppNotification(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        list.add(0, newNotif)

        if (list.size > 50) list.removeAt(list.lastIndex)

        val newJson = gson.toJson(list)
        prefs.edit().putString(KEY_HISTORY, newJson).apply()

        Log.d("NotifStorage", "Lista salvata. Elementi totali: ${list.size}")
    }

    fun getNotifications(context: Context): List<AppNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, "[]")
        val type = object : TypeToken<List<AppNotification>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    private val gson = Gson()
}