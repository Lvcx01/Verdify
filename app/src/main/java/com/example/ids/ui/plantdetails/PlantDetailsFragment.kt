package com.example.ids.ui.plantdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
// IMPORTANTE: Questo serve per far funzionare la navigazione
import androidx.navigation.fragment.findNavController
import com.example.ids.databinding.FragmentPlantDetailsBinding
// IMPORTANTE: Questo serve per chiamare la funzione di eliminazione
import com.example.ids.ui.myplants.PlantManager

class PlantDetailsFragment : Fragment() {

    private var _binding: FragmentPlantDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlantDetailsBinding.inflate(inflater, container, false)

        // RECUPERA I DATI DAL BUNDLE
        val commonName = arguments?.getString("commonName") ?: "Sconosciuta"
        val scientificName = arguments?.getString("scientificName") ?: "Sconosciuta"
        val imagePath = arguments?.getString("imagePath")

        // Codice per caricare l'immagine
        if (imagePath != null) {
            val imgFile = java.io.File(imagePath)
            if (imgFile.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.detailImage.setImageBitmap(bitmap)
            }
        }
        // POPOLA LA VISTA
        binding.detailCommonName.text = commonName
        binding.detailScientificName.text = scientificName

        // --- NUOVO CODICE PER IL TASTO ELIMINA ---
        binding.btnDeletePlant.setOnClickListener {
            // Chiama la funzione di eliminazione nel Manager
            PlantManager.deletePlant(
                requireContext(),
                commonName,
                scientificName,
                imagePath
            )

            // Messaggio di conferma
            Toast.makeText(requireContext(), "Pianta eliminata", Toast.LENGTH_SHORT).show()

            // Torna indietro alla schermata precedente (la lista)
            findNavController().navigateUp()
        }
        // -----------------------------------------

        // TODO: Qui chiameremo Gemini per farci dare il trattamento
        // askGeminiForTreatment(scientificName)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}