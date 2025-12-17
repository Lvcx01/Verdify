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

    suspend fun askForCareTips(plantName: String, weatherReport: String, currentDate: String, daysSinceWatering: Int): String {
        return withContext(Dispatchers.IO) {
            Log.d("VerdifyAI", "Richiesta partita per: $plantName")

            // LOGICA CRITICA: Definiamo chiaramente lo stato per l'IA
            val waterContext = when {
                daysSinceWatering == 0 -> "L'utente ha cliccato 'HO ANNAFFIATO' OGGI STESSO. Il terreno è bagnato."
                daysSinceWatering > 1000 -> "Mai annaffiata finora nell'app."
                else -> "Non innaffiata da $daysSinceWatering giorni."
            }

            val prompt = """
                Ruolo: Botanico esperto. Stile: Telegrafico, Professionale, Schematizzato.
                Target: Utente app mobile (lettura rapida).
                
                PIANTA: "$plantName"
                DATA OGGI: $currentDate
                METEO: $weatherReport
                STATO ACQUA: $waterContext
                
                COMPITO SPECIALE - ANALISI TERMICA:
                Considera la temperatura indicata nel METEO come media della giornata.
                Confrontala con i limiti di tolleranza biologica della "$plantName".
                - Se T < Minima tollerata: Scrivi "ALLERTA FREDDO".
                - Se T > Massima tollerata: Scrivi "ALLERTA CALDO".
                - Altrimenti: "Temperatura ottimale".
                
                OUTPUT RICHIESTO (Segui rigorosamente):
                
                📊 DIAGNOSI
                [Max 1 frase. Sintesi generale salute pianta in questo clima]
                
                🌡️ CLIMA
                [Verdetto sulla temperatura: "Ideale", "Troppo Freddo" o "Troppo Caldo". Se c'è un'allerta, indica l'azione (es. "Portare dentro").]
                
                💧 IRRIGAZIONE
                [Se giorni=0: Scrivi IMPERATIVO "STOP ACQUA. Terreno già bagnato." e spiega brevemente.]
                [Se giorni>0: Istruzione diretta basata su meteo e temperatura. Es: "Innaffiare subito" o "Attendere".]
                
                ✂️ POTATURA
                [Verdetto basato sulla data di oggi. Se NON è il momento, SCRIVI ESPLICITAMENTE IL PERIODO IDEALE (Mese/Stagione).]
                
                🛡️ PROTEZIONE & CONSIGLI
                [Consiglio extra. Es: "Pulisci le foglie" o "Concimare ora".]
                
                [FREQ: X]
                
                REGOLE:
                - Nessuna introduzione o conclusione.
                - Tag [FREQ: X] obbligatorio alla fine (X = giorni stimati per la prossima innaffiatura).
                - Se annaffiata oggi, X deve essere il ciclo intero standard (es. 7).
            """.trimIndent()

            try {
                val response = model.generateContent(prompt)
                val text = response.text?.trim() ?: "Analisi non disponibile."
                Log.d("VerdifyAI", "Risposta ricevuta: $text")
                return@withContext text
            } catch (e: Exception) {
                Log.e("VerdifyAI", "CRASH AI: ${e.message}", e)
                "ERRORE TECNICO: ${e.javaClass.simpleName}\n${e.message}"
            }
        }
    }
}