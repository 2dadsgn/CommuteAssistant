package com.commuteassistant.data

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// --- Routing ---

data class TomTomRoutingResponse(
    @Json(name = "routes") val routes: List<TomTomRoute>?
)

data class TomTomRoute(
    @Json(name = "summary") val summary: TomTomSummary?
)

data class TomTomSummary(
    @Json(name = "lengthInMeters") val lengthInMeters: Int,
    @Json(name = "travelTimeInSeconds") val travelTimeInSeconds: Int,
    @Json(name = "trafficDelayInSeconds") val trafficDelayInSeconds: Int,
    @Json(name = "departureTime") val departureTime: String?,
    @Json(name = "arrivalTime") val arrivalTime: String?
)

// --- Search ---

data class TomTomSearchResponse(
    @Json(name = "results") val results: List<TomTomSearchResult>?
)

data class TomTomSearchResult(
    @Json(name = "id") val id: String,
    @Json(name = "address") val address: TomTomAddress?,
    @Json(name = "position") val position: TomTomPosition?
)

data class TomTomAddress(
    @Json(name = "freeformAddress") val freeformAddress: String
)

data class TomTomPosition(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double
)

// Legacy compatibility wrapper for Prediction UI model
data class Prediction(
    val description: String,
    val placeId: String,
    val lat: Double? = null,
    val lng: Double? = null
)

interface TomTomApiService {
    @GET("routing/1/calculateRoute/{locations}/json")
    suspend fun calculateRoute(
        @Path("locations") locations: String, // format: "lat,lon:lat,lon"
        @Query("key") apiKey: String,
        @Query("traffic") traffic: Boolean = true,
        @Query("departureTime") departureTime: String? = null,
        @Query("arriveAt") arriveAt: String? = null
    ): TomTomRoutingResponse

    @GET("search/2/search/{query}.json")
    suspend fun fuzzySearch(
        @Path("query") query: String,
        @Query("key") apiKey: String,
        @Query("typeahead") typeahead: Boolean = true,
        @Query("limit") limit: Int = 10
    ): TomTomSearchResponse
}
