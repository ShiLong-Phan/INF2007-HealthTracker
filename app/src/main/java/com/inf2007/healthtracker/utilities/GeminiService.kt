package com.inf2007.healthtracker.utilities

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart

class GeminiService(private val apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun generateMealPlan(
        weight: Int,
        height: Int,
        activityLevel: String,
        dietaryPreference: String,
        calorieIntake: Int
    ): List<String> {
        return try {
            val prompt = """
                Generate a meal plan for a user with:
                - Weight: $weight kg
                - Height: $height cm
                - Activity Level: $activityLevel
                - Dietary Preference: $dietaryPreference
                - Calorie Intake Goal: $calorieIntake kcal
                
                Provide structured meal recommendations in bullet points.
            """.trimIndent()

            val response = generativeModel.generateContent(
                Content(parts = listOf(TextPart(prompt)))
            )

            response.text?.split("\n") ?: listOf("No meal plan found.")
        } catch (e: Exception) {
            listOf("Error generating meal plan: ${e.message}")
        }
    }
}
