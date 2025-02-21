package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealHistoryDetailScreen(
    navController: NavController,
    uid: String,
    timestamp: Long
) {
    var mealHistory by remember { mutableStateOf<MealHistory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("mealHistory")
            .whereEqualTo("uid", uid)
            .whereGreaterThanOrEqualTo("date", Date(timestamp - 1000)) // Allow small variation
            .whereLessThanOrEqualTo("date", Date(timestamp + 1000))
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.let { doc ->
                    val meals = doc.get("meals") as? List<String> ?: emptyList()

                    val restaurantsRaw = doc.get("restaurants") as? List<Map<String, Any>> ?: emptyList()
                    println("🔥 Retrieved Restaurants: $restaurantsRaw") // ✅ Debugging

                    val restaurants = restaurantsRaw.mapNotNull { map ->
                        val name = map["name"] as? String ?: return@mapNotNull null
                        val imageUrl = map["imageUrl"] as? String ?: return@mapNotNull null // Ensure valid URL
                        println("✅ Parsed Restaurant: $name - $imageUrl") // ✅ Debugging
                        Restaurant(name = name, imageUrl = imageUrl)
                    }

                    mealHistory = MealHistory(
                        uid = uid,
                        date = doc.getDate("date") ?: Date(),
                        meals = meals,
                        restaurants = restaurants
                    )
                }
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error: ${exception.message}"
                println("❌ Firestore Error: ${exception.message}")
            }
            .addOnCompleteListener { isLoading = false }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal History Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            } else {
                mealHistory?.let { history ->
                    Text(
                        text = "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(history.date)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Meal List Section
                    Text("Meals:", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(history.meals) { meal ->
                            Text("- $meal", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🔥 Restaurant List Section with Images
                    Text("Nearby Restaurants:", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(history.restaurants) { restaurant ->
                            RestaurantItem(restaurant)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Meal History")
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantItem(restaurant: Restaurant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = restaurant.imageUrl,
            contentDescription = restaurant.name,
            modifier = Modifier
                .size(80.dp)
                .padding(4.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = restaurant.name, style = MaterialTheme.typography.bodyLarge)
    }
}

// Firestore Data Model for Meal History
data class MealHistory(
    val uid: String = "",
    val date: Date = Date(),
    val meals: List<String> = emptyList(),
    val restaurants: List<Restaurant> = emptyList() // ✅ Now a list of objects
)

// Firestore Data Model for Restaurants
data class Restaurant(
    val name: String = "",
    val imageUrl: String = "" // ✅ Ensure we store image URLs
)
