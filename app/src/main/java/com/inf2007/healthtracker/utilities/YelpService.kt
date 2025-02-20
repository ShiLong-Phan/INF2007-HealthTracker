package com.inf2007.healthtracker.utilities

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface YelpService {
    @GET("v3/businesses/search")
    fun searchRestaurants(
        @Header("Authorization") authHeader: String,
        @Query("term") term: String,
        @Query("location") location: String,
        @Query("limit") limit: Int
    ): Call<YelpResponse>
}

fun getYelpSearchTerm(meal: String?): String {
    return when {
        meal.isNullOrEmpty() -> "Healthy restaurants"
        meal.contains("oatmeal", ignoreCase = true) -> "Healthy breakfast"
        meal.contains("salmon", ignoreCase = true) -> "Seafood restaurant"
        meal.contains("grilled chicken", ignoreCase = true) -> "Grilled chicken restaurant"
        meal.contains("brown rice", ignoreCase = true) -> "Asian cuisine"
        meal.contains("sweet potato", ignoreCase = true) -> "Vegetarian restaurant"
        else -> "Healthy food"
    }
}