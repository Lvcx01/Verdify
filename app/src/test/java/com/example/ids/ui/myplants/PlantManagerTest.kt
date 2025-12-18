package com.example.ids.ui.myplants

import android.content.Context
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.FileOutputStream

class PlantManagerTest {

    private lateinit var context: Context
    private lateinit var mockFileOutputStream: FileOutputStream

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockFileOutputStream = mockk(relaxed = true)

        every { context.openFileOutput(any(), any()) } returns mockFileOutputStream

        PlantManager.plants.clear()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `aggiuntaPianta_IncrementaLista`() {
        val nuovaPianta = SavedPlant(
            commonName = "Rosa",
            scientificName = "Rosa rubiginosa",
            accuracy = "95%"
        )

        PlantManager.plants.add(nuovaPianta)

        PlantManager.savePlants(context)

        assertEquals("La lista dovrebbe contenere 1 pianta", 1, PlantManager.plants.size)
        assertEquals("Il nome della pianta deve essere Rosa", "Rosa", PlantManager.plants[0].commonName)

        verify { context.openFileOutput("my_plants.json", Context.MODE_PRIVATE) }
    }

    @Test
    fun `rimozionePianta_RimuoveDallaLista`() {
        val pianta1 = SavedPlant("Basilico", "Ocimum", "90%")
        val pianta2 = SavedPlant("Menta", "Mentha", "80%")

        PlantManager.plants.add(pianta1)
        PlantManager.plants.add(pianta2)
        assertEquals(2, PlantManager.plants.size)

        PlantManager.deletePlant(context, "Basilico", "Ocimum", null)
        assertEquals("La lista dovrebbe contenere solo 1 pianta", 1, PlantManager.plants.size)
        assertEquals("La pianta rimasta dovrebbe essere Menta", "Menta", PlantManager.plants[0].commonName)
    }

    @Test
    fun `ricercaPianta_TrovaQuellaGiusta`() {
        val p1 = SavedPlant("Cactus", "Cactaceae", "99%")
        PlantManager.plants.add(p1)

        val trovata = PlantManager.plants.find { it.commonName == "Cactus" }
        assertNotNull("Dovrebbe trovare la pianta", trovata)
        assertEquals("Cactaceae", trovata?.scientificName)
    }
}