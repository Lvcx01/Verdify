package com.example.ids.ui.identify

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface PlantNetApi {

    @Multipart
    @POST("v2/identify/all")
    suspend fun identifyPlant(
        @Query("api-key") apiKey: String,
        @Part image: MultipartBody.Part,
        @Part("organs") organ: RequestBody,
        @Query("include-related-images") includeRelatedImages: Boolean = true,
        @Query("no-reject") noReject: Boolean = false,
        @Query("lang") lang: String = "it"
    ): PlantNetResponse
}

data class PlantNetResponse(
    val results: List<PlantNetResult>
)

data class PlantNetResult(
    val score: Double,
    val species: PlantNetSpecies,
    val images: List<PlantNetImage>?
)

data class PlantNetSpecies(
    val scientificName: String,
    val commonNames: List<String>?
)

data class PlantNetImage(
    val url: PlantNetImageUrl
)

data class PlantNetImageUrl(
    val m: String,
    val s: String
)