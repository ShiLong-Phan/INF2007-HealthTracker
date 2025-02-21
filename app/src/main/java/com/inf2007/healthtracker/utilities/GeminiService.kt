package com.inf2007.healthtracker.utilities

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import java.io.ByteArrayOutputStream
import android.util.Base64
class GeminiService(private val apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey
    )

    private val foodRecognitionModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey,
        systemInstruction = Content(parts = listOf(TextPart("provide the result as Calories: <value> kcal"))),    )

    suspend fun generateMealPlan(
        age: Int,
        weight: Int,
        height: Int,
        activityLevel: String,
        dietaryPreference: String,
        calorieIntake: Int
    ): List<String> {
        return try {
            val prompt = """
                Generate a meal plan for a user with:
                - Age: $age
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

    suspend fun doFoodRecognition(image: Bitmap, foodName: String): List<String> {
        return try {
            // 1. Encode Bitmap to Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream) // Adjust quality as needed
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

            val prompt = """
                Determine the estimated caloric value of the provided image of $foodName.
                Image (Base64 encoded): $base64Image
                

                Provide the caloric value in kcal in the format "Calories: <value> kcal". DO NOT PROVIDE A RANGE
            """.trimIndent()

            val response = foodRecognitionModel.generateContent(
                Content(parts = listOf(TextPart(prompt)))
            )

            response.text?.split("\n") ?: listOf("No meal plan found.")
        } catch (e: Exception) {
            listOf("Error generating meal plan: ${e.message}")
        }
    }
}
