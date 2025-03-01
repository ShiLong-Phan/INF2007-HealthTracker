package com.inf2007.healthtracker.utilities

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

object YelpApi {
    private const val BASE_URL = "https://api.yelp.com/v3/businesses/search"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    fun searchRestaurants(
        location: String,
        term: String,
        categories: String,
        limit: Int,
        apiKey: String
    ): String? {
        val url = "$BASE_URL?location=$location&term=$term&categories=$categories&sort_by=distance&limit=$limit"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json")
            .addHeader("authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        }
    }

    fun searchRestaurants(
        latitude: Double,
        longitude: Double,
        location: String,
        term: String,
        categories: String,
        limit: Int,
        apiKey: String
    ): String? {
        val url = "$BASE_URL?location=$location&latitude=$latitude&longitude=$longitude&term=$term&categories=$categories&radius=8000&sort_by=distance&limit=$limit"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json")
            .addHeader("authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        }
    }
}