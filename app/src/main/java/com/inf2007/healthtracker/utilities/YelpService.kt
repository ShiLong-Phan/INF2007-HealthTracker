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