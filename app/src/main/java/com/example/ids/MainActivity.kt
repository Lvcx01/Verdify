package com.example.ids

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ids.databinding.ActivityMainBinding
import com.example.ids.ui.notifications.GardeningWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        navView.setupWithNavController(navController)

        navView.setOnItemSelectedListener { item ->
            if (item.itemId != navView.selectedItemId) {
                androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
            } else {
                navController.popBackStack(item.itemId, false)
            }
            true
        }

        navView.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        setupDailyWorker()
    }

    private fun setupDailyWorker() {
        val workRequest = PeriodicWorkRequestBuilder<GardeningWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS) // Primo controllo tra 12 ore (o metti 15 minuti per testare)
            .addTag("gardening_daily_work")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "VerdifyDailyWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}