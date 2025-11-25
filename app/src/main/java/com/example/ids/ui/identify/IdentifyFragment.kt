package com.example.ids.ui.identify

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ids.databinding.FragmentIdentifyBinding
import com.example.ids.ui.myplants.PlantManager
import com.example.ids.ui.myplants.SavedPlant
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

class IdentifyFragment : Fragment() {

    private var _binding: FragmentIdentifyBinding? = null
    private val binding get() = _binding!!

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            binding.imagePreview.setImageBitmap(imageBitmap)
        }
    }

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            binding.imagePreview.setImageURI(imageUri)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to use the camera.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.cameraButton.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openCamera()
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            }
        }

        binding.galleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryResultLauncher.launch(galleryIntent)
        }

        binding.identifyButton.setOnClickListener {
            identifyPlant()
        }

        return root
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraResultLauncher.launch(cameraIntent)
    }

    private fun identifyPlant() {
        // 1. Controllo se c'è l'immagine
        if (binding.imagePreview.drawable == null) {
            Toast.makeText(requireContext(), "Per favore seleziona prima una foto.", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostra un caricamento (opzionale ma consigliato per la UX)
        Toast.makeText(requireContext(), "Identificazione in corso...", Toast.LENGTH_SHORT).show()

        // 2. Preparazione dell'immagine (Questo lo avevi fatto bene!)
        val bitmap = binding.imagePreview.drawable.toBitmap()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        // "images" è la chiave richiesta da PlantNet
        val imagePart = MultipartBody.Part.createFormData("images", "image.jpg", requestBody)

        // 3. Preparazione dell'Organo (MANCAVA QUESTO)
        // Diciamo all'API che la foto è "automatica" (può essere foglia, fiore, ecc.)
        val organPart = "auto".toRequestBody("text/plain".toMediaTypeOrNull())

        // 4. La tua API KEY (MANCAVA QUESTA)
        // Incolla qui la tua NUOVA chiave API tra le virgolette
        val myApiKey = "2b10piJhPcJKsWwmDbuzSlap2"

        lifecycleScope.launch {
            try {
                // 5. Chiamata all'API aggiornata con i parametri
                val response = RetrofitInstance.api.identifyPlant(
                    apiKey = myApiKey,
                    image = imagePart,
                    organ = organPart
                )

                // 6. Gestione della risposta
                // Usiamo "?." perché results potrebbe essere null se non trova nulla
                val results = response.results

                if (!results.isNullOrEmpty()) {
                    // Prende il primo risultato (il più probabile)
                    showConfirmationDialog(results[0])
                } else {
                    Toast.makeText(requireContext(), "Nessuna pianta identificata.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: HttpException) {
                // Errore del server (es. 400, 500)
                val errorMsg = if (e.code() == 400) "Errore richiesta: Controlla i dati inviati" else "Errore Server: ${e.code()}"
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                e.printStackTrace() // Guarda il Logcat per i dettagli
            } catch (e: Exception) {
                // Errore generico (es. niente internet)
                Toast.makeText(requireContext(), "Errore: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun showConfirmationDialog(result: PlantNetResult) {
        val plantName = result.species.commonNames?.firstOrNull() ?: result.species.scientificName
        val accuracy = String.format("%.2f", result.score * 100)

        AlertDialog.Builder(requireContext())
            .setTitle("Pianta Identificata")
            .setMessage("È una $plantName?\n(Confidenza: $accuracy%)")
            .setPositiveButton("Conferma e Salva") { _, _ ->

                // 1. Recupera la Bitmap dall'anteprima
                val bitmap = (binding.imagePreview.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap

                var savedImagePath: String? = null

                // 2. Se c'è un'immagine, salvala su disco
                if (bitmap != null) {
                    try {
                        savedImagePath = PlantManager.saveImageToStorage(requireContext(), bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. Crea l'oggetto con il percorso
                val newPlant = SavedPlant(
                    commonName = plantName,
                    scientificName = result.species.scientificName,
                    accuracy = accuracy,
                    imagePath = savedImagePath
                )

                // 4. Aggiungi alla lista e SALVA IL JSON
                PlantManager.plants.add(0, newPlant)
                PlantManager.savePlants(requireContext())

                Toast.makeText(requireContext(), "Salvata in Le mie piante!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}