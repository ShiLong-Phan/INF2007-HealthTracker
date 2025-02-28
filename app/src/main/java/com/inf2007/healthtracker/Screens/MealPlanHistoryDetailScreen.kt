package com.inf2007.healthtracker.Screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.utilities.BottomNavigationBar
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

    // Use mutableStateListOf so changes trigger recomposition.
    val updatedMeals = remember { mutableStateListOf<String>() }
    val originalMeals = remember { mutableStateListOf<String>() }
    // Track the input for each meal by its index.
    var mealInputState by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    // Track whether edits have been saved.
    var isSaved by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("mealHistory")
            .whereEqualTo("uid", uid)
            .whereGreaterThanOrEqualTo("date", Date(timestamp - 1000))
            .whereLessThanOrEqualTo("date", Date(timestamp + 1000))
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.let { doc ->
                    val meals = doc.get("meals") as? List<String> ?: emptyList()
                    val restaurantsRaw = doc.get("restaurants") as? List<Map<String, Any>> ?: emptyList()
                    val restaurants = restaurantsRaw.mapNotNull { map ->
                        val name = map["name"] as? String ?: return@mapNotNull null
                        val imageUrl = map["imageUrl"] as? String ?: return@mapNotNull null
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
                        documentId = doc.id
                    )
                    originalMeals.clear()
                    originalMeals.addAll(meals)
                    updatedMeals.clear()
                    updatedMeals.addAll(meals)
                    meals.forEachIndexed { index, meal ->
                        mealInputState[index] = meal
                    }
                }
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error: ${exception.message}"
                println("Firestore Error: ${exception.message}")
            }
            .addOnCompleteListener { isLoading = false }
    }

    // Derived state: Check if there are any unsaved edits.
    val hasEdits by remember {
        derivedStateOf {
            updatedMeals.indices.any { index ->
                updatedMeals[index] != originalMeals.getOrNull(index)
            }
        }
    }

    // Determine button text based on state.
    val buttonText = when {
        !hasEdits -> "Edit"
        hasEdits && !isSaved -> "Save"
        isSaved -> "Saved"
        else -> "Edit"
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
                },
                actions = { /* additional actions if needed */ },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage.isNotEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    mealHistory?.let { history ->
                        Text(
                            text = "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(history.date)}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Header Row: "Meals:" and the button on the far right.
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Meal Plan:", style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = {
                                    saveMeals(mealInputState, history) {
                                        isSaved = true
                                        // Also update originalMeals to reflect saved state.
                                        originalMeals.clear()
                                        originalMeals.addAll(updatedMeals)
                                    }
                                },
                                enabled = hasEdits && !isSaved,
                                modifier = Modifier.size(80.dp, 32.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Box(modifier = Modifier.fillMaxSize().offset(y = (0).dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = buttonText,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                    )
                                }
                            }
                        }

                        // Editable Meal List using itemsIndexed for stability.
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                                itemsIndexed(updatedMeals) { index, meal ->
                                    EditableMealItem(
                                        meal = meal,
                                        onMealChange = { updatedMeal ->
                                            updatedMeals[index] = updatedMeal
                                            mealInputState[index] = updatedMeal
                                            // Any change resets the saved state.
                                            isSaved = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Restaurant List Section with Images
                        Text("Nearby Restaurants:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 40.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 40.dp)) {
                            items(history.restaurants) { restaurant ->
                                RestaurantItem(restaurant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditableMealItem(
    meal: String,
    onMealChange: (String) -> Unit
) {
    var mealText by remember { mutableStateOf(meal) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = mealText,
            onValueChange = {
                mealText = it
                onMealChange(it)
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
        )
    }
}

// Modify saveMeals to accept a callback for onSuccess.
fun saveMeals(mealInputState: Map<Int, String>, history: MealHistory, onSuccess: () -> Unit) {
    val updatedMealData = hashMapOf(
        "meals" to mealInputState.toSortedMap().values.toList()
    ) as Map<String, Any>
    FirebaseFirestore.getInstance().collection("mealHistory")
        .document(history.documentId)
        .update(updatedMealData)
        .addOnSuccessListener {
            println("Meals updated successfully!")
            onSuccess()
        }
        .addOnFailureListener { e ->
            println("Failed to update meals: ${e.message}")
        }
}

@Composable
fun RestaurantItem(restaurant: Restaurant) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val expandedContent = @Composable {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Address: ${restaurant.address ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Phone: ${restaurant.phone?.takeIf { it.isNotBlank() } ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Rating: ${if(restaurant.rating == 0.0) "Not Available" else "${restaurant.rating} / 5"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Price: ${restaurant.price?.takeIf { it.isNotBlank() } ?: "Not Available"}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Click Here to Open in Google Maps",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(restaurant.name)}")
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
            .clickable { expanded = !expanded },
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        if (expanded) {
            expandedContent()
        }
    }
}
