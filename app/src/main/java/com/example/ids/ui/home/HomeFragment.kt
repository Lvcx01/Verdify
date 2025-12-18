package com.example.ids.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ids.R
import com.example.ids.databinding.FragmentHomeBinding
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import com.example.ids.ui.weather.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchCurrentWeather()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                binding.tvWeatherLocation.text = "Tap for Settings"
                binding.tvWeatherCondition.text = "GPS Blocked"
            } else {
                binding.tvWeatherLocation.text = "Tap to Allow GPS"
                binding.tvWeatherCondition.text = "Permission needed"
            }
            binding.tvWeatherTemp.text = "--"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkUserName()
        setupGreeting()
        setupClickListeners()
        updateDashboardData()

        applyTouchAnimation(binding.weatherCard, type = "scale_small")
        applyTouchAnimation(binding.identifyCard, type = "rotate_only")
        applyTouchAnimation(binding.btnMyPlants, type = "elevation")
        applyTouchAnimation(binding.btnSettings, type = "elevation")

        fetchCurrentWeather()
    }

    override fun onResume() {
        super.onResume()
        updateDashboardData()

        if (isLocationPermissionGranted()) {
            fetchCurrentWeather()
        }
    }

    private fun fetchCurrentWeather() {
        if (!isLocationPermissionGranted()) {
            binding.tvWeatherLocation.text = "Tap to Allow GPS"
            binding.tvWeatherCondition.text = "Permission needed"

            binding.weatherCard.setOnClickListener {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    openAppSettings()
                }
            }
            return
        }

        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            binding.tvWeatherLocation.text = "GPS Disabled"
            binding.tvWeatherCondition.text = "Tap to Turn On"
            binding.tvWeatherTemp.text = "--"
            binding.weatherCard.setOnClickListener {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            return
        }

        binding.weatherCard.setOnClickListener { findNavController().navigate(R.id.nav_weather) }

        binding.tvWeatherCondition.text = "Locating..."

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
                .addOnSuccessListener { location ->
                    if (location != null) {
                        updateWeatherUI(location.latitude, location.longitude)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                updateWeatherUI(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                binding.tvWeatherLocation.text = "GPS Searching..."
                                binding.tvWeatherCondition.text = "Tap to Retry"
                                binding.tvWeatherTemp.text = "--"

                                binding.weatherCard.setOnClickListener { fetchCurrentWeather() }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    binding.tvWeatherLocation.text = "GPS Error"
                    binding.tvWeatherCondition.text = "Tap to Retry"
                    binding.weatherCard.setOnClickListener { fetchCurrentWeather() }
                }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", requireContext().packageName, null)
            startActivity(intent)
            Toast.makeText(context, "Please enable Location in Permissions", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Open Settings Manually", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWeatherUI(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                if (_binding == null) return@launch

                val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                prefs.edit().putString("saved_lat", "$lat").putString("saved_lon", "$lon").apply()

                val response = withContext(Dispatchers.IO) {
                    WeatherRepository.getWeatherData(requireContext(), lat, lon)
                }

                binding.tvWeatherLocation.text = "📍 ${response.name}"
                binding.tvWeatherTemp.text = "${response.main.temp.roundToInt()}"

                val rawCond = response.weather.firstOrNull()?.description ?: "-"
                binding.tvWeatherCondition.text = rawCond.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }

                binding.weatherCard.setOnClickListener { findNavController().navigate(R.id.nav_weather) }

            } catch (e: HttpException) {
                if (e.code() == 401) setFakeWeatherData("Demo City")
                else if (_binding != null) binding.tvWeatherCondition.text = "Net Error"
            } catch (_: Exception) {
                if (_binding != null) binding.tvWeatherCondition.text = "Offline"
            }
        }
    }

    private fun setFakeWeatherData(locationMsg: String) {
        if (_binding == null) return
        binding.tvWeatherLocation.text = locationMsg
        binding.tvWeatherTemp.text = "22"
        binding.tvWeatherCondition.text = "Sunny (Demo)"
        binding.weatherCard.setOnClickListener { findNavController().navigate(R.id.nav_weather) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyTouchAnimation(view: View, type: String) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    when (type) {
                        "scale_small" -> v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(100).start()
                        "rotate_only" -> {
                            binding.plantIllustration.animate().rotation(15f).setDuration(200).start()
                            v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                        }
                        "elevation" -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                    }
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    when (type) {
                        "scale_small", "elevation" -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        "rotate_only" -> {
                            binding.plantIllustration.animate().rotation(0f).setDuration(200).start()
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }
                    }
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun updateDashboardData() {
        PlantManager.loadPlants(requireContext())
        val plants = PlantManager.plants
        binding.plantCount.text = plants.size.toString()

        if (plants.isNotEmpty()) {
            binding.emptyGardenPlaceholder.visibility = View.GONE
            binding.horizontalScrollPlants.visibility = View.VISIBLE
            populateHorizontalList(plants)
        } else {
            binding.emptyGardenPlaceholder.visibility = View.VISIBLE
            binding.horizontalScrollPlants.visibility = View.GONE
        }
    }

    private fun populateHorizontalList(plants: List<SavedPlant>) {
        binding.horizontalPlantsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160f, resources.displayMetrics).toInt()
        val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()

        for (plant in plants.take(10)) {
            val cardView = inflater.inflate(R.layout.item_plant_card, binding.horizontalPlantsContainer, false)
            val params = LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, marginPx, 0)
            cardView.layoutParams = params

            val imgView = cardView.findViewById<ImageView>(R.id.itemImage)
            val nameView = cardView.findViewById<TextView>(R.id.itemName)
            val sciView = cardView.findViewById<TextView>(R.id.itemScientific)

            nameView.text = plant.commonName
            sciView.text = plant.scientificName

            if (plant.imagePath != null) {
                val file = File(plant.imagePath)
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    imgView.setImageBitmap(bitmap)
                }
            }

            cardView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("commonName", plant.commonName)
                bundle.putString("scientificName", plant.scientificName)
                bundle.putString("imagePath", plant.imagePath)
                findNavController().navigate(R.id.action_show_plant_details, bundle)
            }
            binding.horizontalPlantsContainer.addView(cardView)
        }
    }

    private fun checkUserName() {
        val prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val savedName = prefs.getString("username", null)

        if (savedName == null) {
            val input = android.widget.EditText(requireContext())
            input.hint = "Your Name"
            AlertDialog.Builder(requireContext())
                .setTitle("Welcome to Verdify!")
                .setMessage("What's your name?")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save") { _, _ ->
                    val name = input.text.toString()
                    val finalName = name.ifEmpty { "Gardener" }
                    prefs.edit().putString("username", finalName).apply()
                    binding.tvUsername.text = finalName
                }
                .show()
        } else {
            binding.tvUsername.text = savedName
        }
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when (hour) {
            in 5..11 -> "Good Morning,"
            in 12..17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    private fun setupClickListeners() {
        binding.identifyCard.setOnClickListener { findNavController().navigate(R.id.nav_identify) }
        binding.weatherCard.setOnClickListener { findNavController().navigate(R.id.nav_weather) }
        binding.btnMyPlants.setOnClickListener { findNavController().navigate(R.id.nav_my_plants) }
        binding.btnSeeAll.setOnClickListener { findNavController().navigate(R.id.nav_my_plants) }
        binding.btnSettings.setOnClickListener { findNavController().navigate(R.id.nav_settings) }
        binding.btnNotifications.setOnClickListener { findNavController().navigate(R.id.nav_notifications) }
        binding.emptyGardenPlaceholder.setOnClickListener { findNavController().navigate(R.id.nav_identify) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}