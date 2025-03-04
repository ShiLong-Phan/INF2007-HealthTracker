package com.inf2007.healthtracker.utilities

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.util.Log

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
        gender: String,
        activityLevel: String,
        dietaryPreference: String,
        calorieIntake: Int
    ): List<String> {
        return try {
            val prompt = """
                Generate a **structured and detailed** meal plan catered for South East Asians based on:
            - Age: $age
            - Weight: $weight kg
            - Height: $height cm
            - Gender: $gender
            - Activity Level: $activityLevel
            - Dietary Preference: $dietaryPreference
            - Daily Calorie Goal: $calorieIntake kcal
            
            **Meal Plan Structure:**
            - Include **Breakfast, Lunch, Dinner, and 1-2 Snacks if necessary**.
            - **Clearly state** the total calories per meal.
            - **List ingredients** with their **individual calorie count**.
            - Ensure **meals are balanced and easy to prepare**.
            - Use **natural, whole foods**.

            **FORMAT EXAMPLE:**
            ---
            **Breakfast (450 kcal)**
            - Scrambled eggs (2 eggs) - 150 kcal
            - Whole wheat toast (1 slice) - 80 kcal
            - Avocado (1/2) - 120 kcal
            - Black coffee (no sugar) - 0 kcal
            - Greek yogurt with honey (100g) - 100 kcal

            **Lunch (600 kcal)**
            - Grilled chicken breast (150g) - 250 kcal
            - Quinoa (100g) - 180 kcal
            - Steamed broccoli (1 cup) - 55 kcal
            - Olive oil dressing - 115 kcal

            **Dinner (700 kcal)**
            - Baked salmon (180g) - 350 kcal
            - Garlic mashed sweet potatoes (150g) - 220 kcal
            - Asparagus (1 cup) - 50 kcal
            - Butter (1 tsp) - 80 kcal

            **Snack (250 kcal)**
            - Almonds (20g) - 140 kcal
            - Apple (1 medium) - 110 kcal

            ---
            **Ensure the meal plan is nutritionally sound, varied, and well-balanced. No explanations. Just output the structured meal plan.**
            """.trimIndent()

            val response = generativeModel.generateContent(
                Content(parts = listOf(TextPart(prompt)))
            )

            response.text?.split("\n") ?: listOf("No meal plan found.")
        } catch (e: Exception) {
            listOf("Error generating meal plan: ${e.message} Please click on the Refresh icon to try again!")
        }
    }

    suspend fun doFoodRecognition(image: Bitmap?, foodName: String): List<String> {
        return try {
            val prompt = if (image != null) {
                // Encode Bitmap to Base64
                val byteArrayOutputStream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream) // Adjust quality as needed
                val byteArray = byteArrayOutputStream.toByteArray()
                val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)
                """
            Determine the estimated caloric value of the provided image of $foodName.
            Image (Base64 encoded): $base64Image

            Provide the caloric value in kcal in the format "Calories: <value> kcal". DO NOT PROVIDE A RANGE
            """.trimIndent()
            } else {
                """
            Determine the estimated caloric value of 1 serving of $foodName.

            Provide the caloric value in kcal in the format "Calories: <value> kcal". DO NOT PROVIDE A RANGE
            """.trimIndent()
            }

            val response = foodRecognitionModel.generateContent(
                Content(parts = listOf(TextPart(prompt)))
            )

            response.text?.split("\n") ?: listOf("No caloric value found.")
        } catch (e: Exception) {
            listOf("Error generating caloric value: ${e.message}")
        }
    }


    // 🔹 Predefined prompt for AI-generated health tips
    suspend fun fetchHealthTips(): String {
        return try {
            val prompt = """
               Give me 1 actionable health tips that improve daily well-being. 
               Keep them short and practical.
           """.trimIndent()

            val response = generativeModel.generateContent(
                Content(parts = listOf(TextPart(prompt)))
            )
            response.text ?: "No health tips found."
            } catch (e: Exception) {
                "Error fetching health tips: ${e.message}. Please try again!"
            }
        }



}
