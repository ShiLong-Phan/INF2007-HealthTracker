package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.inf2007.healthtracker.utilities.*
import com.inf2007.healthtracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealRecommendationScreen(
    navController: NavController,
    age: Int,
    gender: String,
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
    var showSuccessMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // ✅ Use Gemini AI SDK
    val geminiService = remember { GeminiService(BuildConfig.geminiApiKey) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // ✅ Generate Meal Plan Using AI
                aiMealPlan = geminiService.generateMealPlan(age, weight, height, gender, activityLevel, dietaryPreference, calorieIntake)

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

    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(500) // Wait for 0.5 seconds
            navController.navigate("main_screen") {
                popUpTo("main_screen") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("AI-Powered Meal Plan") })
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                // Meal Plan Section
                Text("Meal Plan:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(aiMealPlan) { meal ->
                        Text("- $meal", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Restaurant List Section
                Text("Nearby Restaurants:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(restaurantRecommendations) { business ->
                        RestaurantItem(business)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        user?.let {
                            val mealHistoryData = hashMapOf(
                                "uid" to it.uid,
                                "date" to Date(),
                                "meals" to aiMealPlan,
                                "restaurants" to restaurantRecommendations.map { business ->
                                    mapOf(
                                        "name" to business.name,
                                        "imageUrl" to business.image_url, //  Store image URL
                                        "address" to business.location.address1, // Add address
                                        "rating" to business.rating, // Add rating
                                        "phone" to business.phone, // Add phone number
                                        "price" to business.price // Add price
                                    )
                                }
                            )
                            firestore.collection("mealHistory")
                                .add(mealHistoryData)
                                .addOnSuccessListener {
                                    showSuccessMessage = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Meal history saved successfully!")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Failed to save meal history: ${e.message}")
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Meal History")
                }
            }
        }
    }
}

@Composable
fun RestaurantItem(business: Business) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) } // Track expanded state
    val expandedContent = @Composable {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Address: ${business.location.address1 ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Phone: ${business.phone?.takeIf { it.isNotBlank() } ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Rating: ${if (business.rating == 0.0) "Not Available" else "${business.rating} / 5"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Price: ${business.price?.takeIf { it.isNotBlank() } ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Click Here to Open in Google Maps",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Blue),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(business.name)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { expanded = !expanded }, // Toggle expanded state when clicked
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = business.image_url,
                contentDescription = business.name,
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = business.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        // Show more details if expanded
        if (expanded) {
            expandedContent()
        }
    }
}

