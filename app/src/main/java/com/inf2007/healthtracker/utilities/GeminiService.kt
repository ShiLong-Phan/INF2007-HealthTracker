package com.inf2007.healthtracker.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory // Needed to decode byte array back to Bitmap for ImagePart
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.ImagePart // <-- Import ImagePart
import com.google.ai.client.generativeai.type.content // <-- Import content builder
import com.google.ai.client.generativeai.type.generationConfig
import java.io.ByteArrayOutputStream
import android.util.Base64 // Base64 might still be useful elsewhere, but not for sending image content here
import android.util.Log
import java.io.BufferedOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(private val apiKey: String) {

    // --- Model Initializations (Consider changing model names as discussed previously) ---
    // --- e.g., use "gemini-1.5-pro-latest" or "gemini-1.5-flash-latest" ---

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp", // Example: updated model name
        apiKey = apiKey
    )

    private val foodIdentificationModel = GenerativeModel(
        modelName = "gemini-1.5-pro-latest", // Example: updated model name for better vision
        apiKey = apiKey,
        systemInstruction = Content(parts = listOf(TextPart(
            "You are a food identification expert. Identify the exact food item in the image with high precision. " +
                    "Respond with just the name of the food in a single line. " +
                    "Be as specific as possible (e.g., 'Grilled Chicken Breast' instead of just 'Chicken'). " +
                    "If multiple items are visible, identify the main dish.")))
    )

    private val foodRecognitionModel = GenerativeModel(
        modelName = "gemini-1.5-pro-latest", // Example: updated model name for better vision
        apiKey = apiKey,
        systemInstruction = Content(parts = listOf(TextPart(
            "You are a food nutrition expert. Analyze the image and provide the exact caloric value. " +
                    "Respond only with 'Calories: X kcal' where X is a precise number, not a range.")))
    )

    // --- optimizeImageForGemini function remains unchanged ---
    private suspend fun optimizeImageForGemini(bitmap: Bitmap): ByteArray = withContext(Dispatchers.IO) {
        val width = bitmap.width
        val height = bitmap.height
        val maxDimension = 1024
        var targetWidth = width
        var targetHeight = height

        if (width > maxDimension || height > maxDimension) {
            if (width > height) {
                targetWidth = maxDimension
                targetHeight = (height * (maxDimension.toFloat() / width)).toInt()
            } else {
                targetHeight = maxDimension
                targetWidth = (width * (maxDimension.toFloat() / height)).toInt()
            }
        }

        val optimizedBitmap = if (targetWidth != width || targetHeight != height) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        val bufferedOutputStream = BufferedOutputStream(byteArrayOutputStream)
        optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, bufferedOutputStream)
        bufferedOutputStream.flush()

        if (optimizedBitmap != bitmap) {
            optimizedBitmap.recycle()
        }

        return@withContext byteArrayOutputStream.toByteArray()
    }

    // --- generateMealPlan function remains unchanged ---
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
                Generate a **structured and detailed** meal plan catered for Asians based on:
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
            Log.e("GeminiService", "Error generating meal plan: ${e.message}", e) // Log error
            listOf("Error generating meal plan: ${e.message} Please click on the Refresh icon to try again!")
        }
    }

    // --- CORRECTED Food identification using proper multimodal content ---
    suspend fun identifyFood(image: Bitmap): List<String> {
        return try {
            Log.d("GeminiService", "Starting food identification")

            // Optimize image for the API
            val imageBytes = optimizeImageForGemini(image)

            // Create Bitmap from optimized bytes for ImagePart
            val optimizedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // *** CORRECTED MULTIMODAL CONTENT ***
            // Use the content builder to combine image and text parts
            val inputContent = content {
                image(optimizedBitmap) // Pass the Bitmap object
                text("What specific food is this? Provide ONLY the name of the food in a single line. Be very specific (e.g., \"Chicken Tikka Masala\" instead of just \"Curry\"). If multiple items are visible, identify the main dish.")
            }

            // Generate content using the structured input
            val response = foodIdentificationModel.generateContent(inputContent)

            // Clean the response to extract just the food name
            val foodName = response.text?.trim()?.replace(Regex("^\"(.*)\"$"), "$1") ?: "Unknown food"
            Log.d("GeminiService", "Food identified as: $foodName")

            listOf(foodName)

        } catch (e: Exception) {
            Log.e("GeminiService", "Error identifying food: ${e.message}", e)
            listOf("Error identifying food: ${e.message}")
        }
    }

    // --- CORRECTED Calorie recognition function using proper multimodal content ---
    suspend fun doFoodRecognition(image: Bitmap?, foodName: String): List<String> {
        return try {
            if (image != null) {
                Log.d("GeminiService", "Starting calorie recognition with image for: $foodName")

                // Optimize image
                val imageBytes = optimizeImageForGemini(image)

                // Create Bitmap from optimized bytes
                val optimizedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // *** CORRECTED MULTIMODAL CONTENT ***
                // Use the content builder for image and text
                val inputContent = content {
                    image(optimizedBitmap) // Pass the Bitmap object
                    text("I need the exact caloric value of this $foodName. Provide ONLY the caloric value in the format \"Calories: X kcal\", where X is a precise number. DO NOT provide a range or explanation, just the format \"Calories: X kcal\".")
                }

                // Generate content using structured input
                val response = foodRecognitionModel.generateContent(inputContent)

                val responseText = response.text ?: ""
                Log.d("GeminiService", "Raw calorie response: $responseText")

                // Extract calorie value with a more reliable regex
                val calorieRegex = Regex("Calories:\\s*(\\d+)\\s*kcal", RegexOption.IGNORE_CASE)
                val matchResult = calorieRegex.find(responseText)

                if (matchResult != null) {
                    val calorieValue = matchResult.groupValues[1]
                    Log.d("GeminiService", "Extracted calorie value: $calorieValue")
                    listOf("Calories: $calorieValue kcal")
                } else {
                    // Fallback using filter - less reliable but better than nothing
                    val estimatedCaloricValue = responseText.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
                    Log.w("GeminiService", "Regex failed for calorie extraction, fallback value: $estimatedCaloricValue")
                    listOf("Calories: $estimatedCaloricValue kcal (estimated)") // Indicate fallback
                }
            } else {
                // --- Text-only query remains the same ---
                Log.d("GeminiService", "Starting text-only calorie recognition for: $foodName")

                val prompt = "What is the average calorie content of one serving of $foodName? Provide only the number followed by kcal in the format 'Calories: X kcal'."

                // Text-only uses TextPart directly in Content
                val response = foodRecognitionModel.generateContent(
                    Content(parts = listOf(TextPart(prompt)))
                )

                val responseText = response.text ?: ""
                Log.d("GeminiService", "Raw text-only calorie response: $responseText")

                // Extract calorie value
                val calorieRegex = Regex("Calories:\\s*(\\d+)\\s*kcal", RegexOption.IGNORE_CASE)
                val matchResult = calorieRegex.find(responseText)

                if (matchResult != null) {
                    val calorieValue = matchResult.groupValues[1]
                    Log.d("GeminiService", "Extracted text-only calorie value: $calorieValue")
                    listOf("Calories: $calorieValue kcal")
                } else {
                    // Fallback using filter
                    val estimatedCaloricValue = responseText.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
                    Log.w("GeminiService", "Regex failed for text-only calorie extraction, fallback value: $estimatedCaloricValue")
                    listOf("Calories: $estimatedCaloricValue kcal (estimated)") // Indicate fallback
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in food recognition: ${e.message}", e)
            listOf("Error generating caloric value: ${e.message}")
        }
    }

    // --- fetchHealthTips function remains unchanged ---
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
            Log.e("GeminiService", "Error fetching health tips: ${e.message}", e) // Log error
            "Error fetching health tips: ${e.message}. Please try again!"
        }
    }
}