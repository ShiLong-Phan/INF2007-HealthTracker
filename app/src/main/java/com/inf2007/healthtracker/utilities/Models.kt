package com.inf2007.healthtracker.utilities

import java.util.*

data class MealHistory(
    val documentId: String = "",
    val uid: String = "",
    val date: Date = Date(),
    val meals: List<String> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(),
    val calorieGoal: Int = 0
)

data class Restaurant(
    val name: String = "",
    val imageUrl: String = "",
    val address: String = "",
    val phone: String = "",
    val rating: Double = 0.0,
    val price: String = "",
    val coordinates: Coordinates? = null
)
