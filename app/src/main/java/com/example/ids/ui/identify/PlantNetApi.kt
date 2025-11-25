package com.example.ids.ui.identify

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface PlantNetApi {

    @Multipart
    @POST("v2/identify/all")
    suspend fun identifyPlant(
        @Part image: MultipartBody.Part,
        @Query("api-key") apiKey: String = "2b10piJhPcJKsWwmDbuzSlap2"
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