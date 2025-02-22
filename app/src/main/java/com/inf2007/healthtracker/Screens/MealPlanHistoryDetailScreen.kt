package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.utilities.MealHistory
import com.inf2007.healthtracker.utilities.Restaurant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanHistoryDetailScreen(
    navController: NavController,
    uid: String,
    timestamp: Long
) {
    var mealHistory by remember { mutableStateOf<MealHistory?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var updatedMeals by remember { mutableStateOf(mutableListOf<String>()) }
    var mealInputState by remember { mutableStateOf(mutableMapOf<String, String>()) } // State to track each meal input separately

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
                        restaurants = restaurants,
                        documentId = doc.id // Save the document ID here
                    )
                    updatedMeals = meals.toMutableList() // Initialize with the fetched meals
                    mealInputState = meals.associateWith { it }.toMutableMap() // Initialize input state
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

                    // Editable Meal List Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // This aligns the items in the row with space between them
                    ) {
                        // Text "Meals:" and button in the same row
                        Text("Meals:", style = MaterialTheme.typography.titleMedium)

                        // Save button placed on the far-right side of the row
                        Button(
                            onClick = { saveMeals(mealInputState, history) },
                            modifier = Modifier
                                .size(80.dp, 30.dp) // Smaller button size
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = (-3).dp), // Adjust vertical offset to move text higher
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Save", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                            }
                        }

                    }

                    // Editable Meal List
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(updatedMeals) { meal ->
                                EditableMealItem(
                                    meal = meal,
                                    mealInputState = mealInputState,
                                    onMealChange = { updatedMeal ->
                                        mealInputState[meal] = updatedMeal // Update only the changed meal
                                    }
                                )
                            }
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
                }
            }
        }
    }
}

// Editable meal item
@Composable
fun EditableMealItem(
    meal: String,
    mealInputState: MutableMap<String, String>,
    onMealChange: (String) -> Unit
) {
    var mealText by remember { mutableStateOf(mealInputState[meal] ?: meal) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = mealText,
            onValueChange = {
                mealText = it
                onMealChange(it) // Call onChange with the new value
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
        )
    }
}

// Save the updated meal list to Firestore
fun saveMeals(mealInputState: Map<String, String>, history: MealHistory) {
    val updatedMealData = hashMapOf(
        "meals" to mealInputState.values.toList() // Updated meals list
    ) as Map<String, Any> // Cast to Map<String, Any> to match Firestore update requirements

    FirebaseFirestore.getInstance().collection("mealHistory")
        .document(history.documentId) // Assuming the documentId is available
        .update(updatedMealData)
        .addOnSuccessListener {
            // Show confirmation
            println("Meals updated successfully!")
        }
        .addOnFailureListener { e ->
            // Handle error
            println("Failed to update meals: ${e.message}")
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
