package com.example.ids.ui.identify

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ids.databinding.FragmentIdentifyBinding
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class IdentifyFragment : Fragment() {

    private var _binding: FragmentIdentifyBinding? = null
    private val binding get() = _binding!!
    private var isPhotoSelected = false

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) loadOptimizedImage(imageUri)
        }
    }

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                val rotatedBitmap = if (imageBitmap.width > imageBitmap.height) {
                    rotateBitmap(imageBitmap, 90f)
                } else {
                    imageBitmap
                }
                binding.imagePreview.setImageBitmap(rotatedBitmap)
                showImageContainer()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                if (!shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraButton.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        binding.galleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            galleryResultLauncher.launch(galleryIntent)
        }

        binding.identifyButton.setOnClickListener {
            identifyPlant()
        }
    }

    private fun checkPermissionAndOpenCamera() {
        val permission = android.Manifest.permission.CAMERA

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("Camera access is blocked. Please enable it in Settings to take photos.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", requireContext().packageName, null)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot open settings manually", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            cameraResultLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(requireContext(), "Camera app not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOptimizedImage(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        binding.imagePreview.setImageBitmap(bitmap)
                        showImageContainer()
                    } else {
                        Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showImageContainer() {
        binding.placeholderContainer.visibility = View.GONE
        binding.imagePreview.visibility = View.VISIBLE
        isPhotoSelected = true
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun identifyPlant() {
        if (!isPhotoSelected) {
            Toast.makeText(requireContext(), "Please select an image!", Toast.LENGTH_LONG).show()
            return
        }

        binding.identifyButton.isEnabled = false
        binding.identifyButton.text = "ANALYZING..."

        val bitmap = (binding.imagePreview.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val byteArray = stream.toByteArray()

                val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("images", "image.jpg", requestBody)
                val organPart = "auto".toRequestBody("text/plain".toMediaTypeOrNull())

                val myApiKey = com.example.ids.BuildConfig.PLANTNET_API_KEY

                val response = RetrofitInstance.api.identifyPlant(
                    apiKey = myApiKey.trim(),
                    image = imagePart,
                    organ = organPart
                )

                withContext(Dispatchers.Main) {
                    binding.identifyButton.isEnabled = true
                    binding.identifyButton.text = "IDENTIFY PLANT"

                    val results = response.results
                    if (!results.isNullOrEmpty()) {
                        showConfirmationDialog(results[0])
                    } else {
                        showErrorDialog("No match found.", "Try a clearer photo.")
                    }
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    binding.identifyButton.isEnabled = true
                    binding.identifyButton.text = "IDENTIFY PLANT"
                    if (e.code() == 404) showErrorDialog("Not a plant?", "AI found nothing.")
                    else showErrorDialog("Server Error", "Code: ${e.code()}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.identifyButton.isEnabled = true
                    binding.identifyButton.text = "IDENTIFY PLANT"
                    showErrorDialog("Connection Error", e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }

    private fun showConfirmationDialog(result: PlantNetResult) {
        val scorePercent = (result.score * 100).toInt()
        val scientificName = result.species.scientificName
        val commonName = result.species.commonNames?.firstOrNull() ?: scientificName

        val message = "Scientific: $scientificName\nConfidence: $scorePercent%"

        AlertDialog.Builder(requireContext())
            .setTitle("Is this a $commonName?")
            .setMessage(message)
            .setPositiveButton("Yes, Save") { dialog, _ ->
                saveIdentifiedPlant(result)
                dialog.dismiss()
                resetUI()
            }
            .setNegativeButton("No, Retry") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showErrorDialog(title: String, msg: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveIdentifiedPlant(result: PlantNetResult) {
        val name = result.species.commonNames?.firstOrNull() ?: result.species.scientificName
        val bitmap = (binding.imagePreview.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        val path = if(bitmap != null) PlantManager.saveImageToStorage(requireContext(), bitmap) else null
        val accuracyString = "${(result.score * 100).toInt()}%"

        val newPlant = SavedPlant(
            commonName = name.replaceFirstChar { it.uppercase() },
            scientificName = result.species.scientificName,
            accuracy = accuracyString,
            imagePath = path
        )
        PlantManager.plants.add(0, newPlant)
        PlantManager.savePlants(requireContext())
        Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun resetUI() {
        binding.imagePreview.setImageDrawable(null)
        binding.imagePreview.visibility = View.INVISIBLE
        binding.placeholderContainer.visibility = View.VISIBLE
        isPhotoSelected = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}