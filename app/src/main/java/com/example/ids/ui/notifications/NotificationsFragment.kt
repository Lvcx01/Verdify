package com.example.ids.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ids.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.d("NotifFrag", "onResume: Caricamento dati...")
        loadData()
    }

    private fun loadData() {
        val notifs = NotificationStorage.getNotifications(requireContext())
        Log.d("NotifFrag", "Dati caricati: ${notifs.size} notifiche trovate.")

        if (notifs.isNotEmpty()) {
            binding.recyclerNotifications.visibility = View.VISIBLE
            binding.emptyStateView.visibility = View.GONE
            val adapter = NotificationsAdapter(notifs)
            binding.recyclerNotifications.adapter = adapter
        } else {
            binding.recyclerNotifications.visibility = View.GONE
            binding.emptyStateView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}