package com.inf2007.healthtracker.utilities

data class YelpResponse(
    val businesses: List<Business>
)

data class Business(
    val name: String,
    val rating: Double,
    val location: Location,
    val image_url: String,
    val phone: String,
    val price: String,
    val coordinates: Coordinates?
)

data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Location(
    val address1: String,
    val city: String,
    val state: String,
    val zip_code: String
)