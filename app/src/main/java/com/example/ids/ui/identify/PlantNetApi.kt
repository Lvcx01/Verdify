package com.example.ids.ui.identify

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface PlantNetApi {

    @Multipart
    @POST("v2/identify/all") // Solo l'endpoint finale!
    suspend fun identifyPlant(
        @Query("api-key") apiKey: String, // Passala qui, non nell'URL!
        @Part image: MultipartBody.Part,  // L'immagine
        @Part("organs") organ: RequestBody, // FONDAMENTALE: foglia, fiore, ecc.
        @Query("include-related-images") includeRelatedImages: Boolean = false,
        @Query("no-reject") noReject: Boolean = false,
        @Query("lang") lang: String = "it"
    ): PlantNetResponse
}

data class PlantNetResponse(
    val results: List<PlantNetResult>
)

data class PlantNetResult(
    val score: Double,
    val species: PlantNetSpecies
)

data class PlantNetSpecies(
    val scientificName: String,
    val commonNames: List<String>
)