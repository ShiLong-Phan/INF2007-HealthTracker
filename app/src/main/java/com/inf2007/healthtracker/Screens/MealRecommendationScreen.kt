package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.inf2007.healthtracker.utilities.*
import com.inf2007.healthtracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MealRecommendationScreen(
    navController: NavController,
    weight: Int,
    height: Int,
    activityLevel: String,
    dietaryPreference: String,
    calorieIntake: Int
) {
    var aiMealPlan by remember { mutableStateOf<List<String>>(emptyList()) }
    var restaurantRecommendations by remember { mutableStateOf<List<Business>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // ✅ Use Gemini AI SDK
    val geminiService = remember { GeminiService(BuildConfig.geminiApiKey) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // ✅ Generate Meal Plan Using AI
                aiMealPlan = geminiService.generateMealPlan(weight, height, activityLevel, dietaryPreference, calorieIntake)

                // ✅ Convert AI-generated meal to a Yelp-friendly search term
                val searchTerm = getYelpSearchTerm(aiMealPlan.firstOrNull())

                // ✅ Fetch Restaurant Recommendations from Yelp
                if (searchTerm.isNotEmpty()) {
                    val apiKey = BuildConfig.yelpApiKey
                    val yelpResponse = withContext(Dispatchers.IO) {
                        YelpApi.searchRestaurants(
                            location = "Singapore",
                            term = searchTerm, // ✅ Use mapped search term
                            categories = "healthy,restaurants", // Restrict search to healthy restaurants
                            limit = 10,
                            apiKey = apiKey
                        )
                    }

                    if (yelpResponse != null) {
                        val parsedYelpResponse = Gson().fromJson(yelpResponse, YelpResponse::class.java)
                        restaurantRecommendations = parsedYelpResponse.businesses
                    } else {
                        errorMessage = "Failed to get restaurant recommendations"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMessage.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text("AI-Powered Meal Plan", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(aiMealPlan) { meal ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = meal, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nearby Restaurants", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(restaurantRecommendations) { business ->
                Column(modifier = Modifier.padding(8.dp)) {
                    val painter = rememberAsyncImagePainter(business.image_url)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = business.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Rating: ${business.rating}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = business.location.address1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
