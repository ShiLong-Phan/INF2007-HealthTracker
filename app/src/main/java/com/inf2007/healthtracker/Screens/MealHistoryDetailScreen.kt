package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.utilities.MealHistory
import com.inf2007.healthtracker.utilities.Restaurant
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

                    val restaurants = restaurantsRaw.mapNotNull { map ->
                        val name = map["name"] as? String ?: return@mapNotNull null
                        val imageUrl = map["imageUrl"] as? String ?: return@mapNotNull null // Ensure valid URL
                        val address = map["address"] as? String ?: "Not Available"
                        val rating = map["rating"] as? Double ?: 0.0
                        val phone = map["phone"] as? String ?: "Not Available"
                        val price = map["price"] as? String ?: "Not Available"

                        Restaurant(name = name, imageUrl = imageUrl, address = address, rating = rating, price = price, phone = phone)
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
                println("Firestore Error: ${exception.message}")
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
    var expanded by remember { mutableStateOf(false) } // Track expanded state

    val expandedContent = @Composable {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Address: ${restaurant.address ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Phone: ${restaurant.phone?.takeIf { it.isNotBlank() } ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Rating: ${if(restaurant.rating == 0.0) "Not Available" else "${restaurant.rating} / 5"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Price: ${restaurant.price?.takeIf { it.isNotBlank() } ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
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
                model = restaurant.imageUrl,
                contentDescription = restaurant.name,
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = restaurant.name,
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


