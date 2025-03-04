package com.inf2007.healthtracker.utilities

import android.location.Location

fun calculateDistance(
    userLocation: Location,
    restaurantLat: Double,
    restaurantLng: Double
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude,
        userLocation.longitude,
        restaurantLat,
        restaurantLng,
        results
    )
    // Convert meters to kilometers
    return results[0] / 1000.0
}
