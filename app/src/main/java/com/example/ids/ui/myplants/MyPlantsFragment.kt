package com.example.ids.ui.myplants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ids.databinding.FragmentMyPlantsBinding
import androidx.navigation.fragment.findNavController
import com.example.ids.R

class MyPlantsFragment : Fragment() {

    private var _binding: FragmentMyPlantsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPlantsBinding.inflate(inflater, container, false)

        // Setup della lista
        setupRecyclerView()

        return binding.root
    }

    private fun setupRecyclerView() {
        val adapter = PlantsAdapter(PlantManager.plants) { selectedPlant ->

            // 1. Prepariamo i dati come prima
            val bundle = Bundle()
            bundle.putString("commonName", selectedPlant.commonName)
            bundle.putString("scientificName", selectedPlant.scientificName)
            bundle.putString("imagePath", selectedPlant.imagePath)

            // 2. USIAMO IL NAVIGATOR INVECE DI BEGINTRANSACTION
            // R.id.action_show_plant_details deve corrispondere all'ID che hai messo nel file XML al passo 1
            try {
                findNavController().navigate(R.id.action_show_plant_details, bundle)
            } catch (e: Exception) {
                // Se crasha qui, significa che l'ID nell'XML è diverso da quello scritto qui
                e.printStackTrace()
                Toast.makeText(context, "Errore navigazione: controlla mobile_navigation.xml", Toast.LENGTH_LONG).show()
            }
        }

        binding.recyclerViewPlants.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPlants.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Ricarica nel caso siano cambiate cose
        PlantManager.loadPlants(requireContext())
        binding.recyclerViewPlants.adapter?.notifyDataSetChanged()

        // Gestione lista vuota (opzionale ma consigliata)
        if (PlantManager.plants.isEmpty()) {
            binding.textEmptyList.visibility = View.VISIBLE
            binding.recyclerViewPlants.visibility = View.GONE
        } else {
            binding.textEmptyList.visibility = View.GONE
            binding.recyclerViewPlants.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Carica i dati dal disco appena si apre la vista
        PlantManager.loadPlants(requireContext())
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}