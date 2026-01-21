package com.example.ids

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private val reqPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            val permissionName = it.key
            val isGranted = it.value
            if (!isGranted) {
                showPermissionDeniedDialog(permissionName)
            }
        }
    }
    private fun checkAndRequestPermissions() {
        val missingPermissions = reqPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        }
    }
    private fun showPermissionDeniedDialog(permission: String) {
        val message = when (permission) {
            Manifest.permission.CAMERA -> "Serve la fotocamera per vedere le piante! 🌱"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Serve la posizione per il meteo locale! 🌦️"
            Manifest.permission.POST_NOTIFICATIONS -> "Servono le notifiche per ricordarti l'acqua! 💧"
            else -> "Questa funzione richiede un permesso per funzionare."
        }

        AlertDialog.Builder(this)
            .setTitle("Permesso Mancante")
            .setMessage(message)
            .setPositiveButton("Riprova") { _, _ ->
                requestPermissionLauncher.launch(arrayOf(permission))
            }
            .setNegativeButton("Ignora", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val termsAccepted = prefs.getBoolean("terms_accepted", false)
        if(!termsAccepted){
            showTermsDialog()
        }else {
            checkAndRequestPermissions()
        }
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

    private fun showTermsDialog() {
        val termsText = """
            Benvenuto in Verdify!
            
            Per offrirti la migliore esperienza, l'app richiede i seguenti permessi:
            • Fotocamera e Galleria (per identificare le piante)
            • Posizione (per il meteo locale)
            • Archivio (per salvare i dati)
            
            Responsabilità dell'utente:
            1. L'utente si assume la responsabilità della cura delle piante e dell'incolumità durante eventi atmosferici.
            2. L'utente è responsabile del backup dei propri dati; in caso di disinstallazione i dati locali andranno persi.
            3. L'uso dell'app implica l'accettazione di queste linee guida.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Termini e Condizioni")
            .setMessage(termsText)
            .setCancelable(false)
            .setPositiveButton("Conferma e Accetta") { _, _ ->
                val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("terms_accepted", true).apply()
                checkAndRequestPermissions()
            }
            .show()
    }

    private fun setupDailyWorker() {
        val workRequest = PeriodicWorkRequestBuilder<GardeningWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .addTag("gardening_daily_work")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "VerdifyDailyCare",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}