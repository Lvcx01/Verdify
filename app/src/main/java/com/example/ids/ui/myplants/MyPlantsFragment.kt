package com.example.ids.ui.myplants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ids.MainActivity
import com.example.ids.R
import com.example.ids.databinding.FragmentMyPlantsBinding

class MyPlantsFragment : Fragment() {

    private var _binding: FragmentMyPlantsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPlantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddPlant.setOnClickListener {
            findNavController().navigate(R.id.nav_identify)
            (activity as? MainActivity)?.binding?.navView?.selectedItemId = R.id.nav_identify
        }

        PlantManager.loadPlants(requireContext())
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(context, 2)
        binding.recyclerViewPlants.layoutManager = layoutManager

        val adapter = PlantsAdapter(PlantManager.plants) { selectedPlant ->
            val bundle = Bundle()
            bundle.putString("commonName", selectedPlant.commonName)
            bundle.putString("scientificName", selectedPlant.scientificName)
            bundle.putString("imagePath", selectedPlant.imagePath)
            findNavController().navigate(R.id.action_show_plant_details, bundle)
        }

        binding.recyclerViewPlants.adapter = adapter
        if (PlantManager.plants.isEmpty()) {
            binding.textEmptyList.visibility = View.VISIBLE
            binding.recyclerViewPlants.visibility = View.GONE
        } else {
            binding.textEmptyList.visibility = View.GONE
            binding.recyclerViewPlants.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        PlantManager.loadPlants(requireContext())
        binding.recyclerViewPlants.adapter?.notifyDataSetChanged()

        if (PlantManager.plants.isEmpty()) {
            binding.textEmptyList.visibility = View.VISIBLE
        } else {
            binding.textEmptyList.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}