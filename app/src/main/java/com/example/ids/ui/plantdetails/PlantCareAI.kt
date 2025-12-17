package com.example.ids.ui.plantdetails

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlantCareAI {
    val model = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = com.example.ids.BuildConfig.GEMINI_API_KEY
    )

    suspend fun askForCareTips(plantName: String, weatherReport: String, currentDate: String): String {
        return withContext(Dispatchers.IO) {
            Log.d("VerdifyAI", "Richiesta partita per: $plantName")
            val prompt = """
                Agisci come un algoritmo di giardinaggio di precisione.
                
                PIANTA: "$plantName"
                DATA: $currentDate
                METEO ATTUALE: 
                $weatherReport
                
                COMPITO:
                Confronta i valori meteo (Temp, Umidità, Vento) con le tolleranze biologiche di questa specifica pianta.
                
                REGOLE DI RISPOSTA:
                - Se l'umidità è > 80%: Scrivi IMPERATIVO "NON INNAFFIARE" (rischio funghi).
                - Se la temperatura è sotto la tolleranza della pianta: Scrivi IMPERATIVO "PORTARE DENTRO" o "COPRIRE".
                - Se c'è vento forte (> 20km/h) e la pianta è alta/fragile: Scrivi "METTERE AL RIPARO".
                
                Genera una risposta strutturata così:
                📊 Diagnosi: [Confronto diretto Dati vs Pianta. Es: "Fa troppo freddo per una tropicale"]
                💧 Acqua: [Ordine secco. Es: "STOP ACQUA. Umidità atmosferica sufficiente. Indica ogni quanto annaffiare con le condizioni meteo attuali in giorni"]
                🛡️ Protezione: [Azione fisica. Es: "Sposta in casa, vento troppo forte. Spiegare il perchè"]
                ✂️ Potatura: [Verdetto basato sulla data di oggi]
                
                Sii breve, tecnico e diretto. Niente convenevoli.
            """.trimIndent()

            try {
                val response = model.generateContent(prompt)
                response.text?.trim() ?: "Analisi non disponibile."

            } catch (e: Exception) {
                Log.e("VerdifyAI", "CRASH AI: ${e.message}", e)

                "ERRORE TECNICO: ${e.javaClass.simpleName}\n${e.message}"
            }
        }
    }
}