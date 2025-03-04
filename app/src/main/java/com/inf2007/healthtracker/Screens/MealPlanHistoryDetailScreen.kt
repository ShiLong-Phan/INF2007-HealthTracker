package com.inf2007.healthtracker.Screens

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.R
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.utilities.Coordinates
import com.inf2007.healthtracker.utilities.MealHistory
import com.inf2007.healthtracker.utilities.Restaurant
import java.text.SimpleDateFormat
import java.util.*
import com.inf2007.healthtracker.utilities.calculateDistance
import com.inf2007.healthtracker.utilities.RequestLocationPermission


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

    // Obtain location updates for calculating distance.
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // Request location permission and start live location updates if granted
    RequestLocationPermission(
        onPermissionGranted = {
            // Once permission is granted, start requesting continuous updates
            StartLocationUpdates(
                fusedLocationClient = fusedLocationClient
            ) { newLocation ->
                userLocation = newLocation
                Log.d("Location Update", "Live location update = $newLocation")
            }
        }
    )

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
                        // Parse coordinates if available
                        val coordinatesMap = map["coordinates"] as? Map<String, Any>
                        val latitude = coordinatesMap?.get("latitude") as? Double
                        val longitude = coordinatesMap?.get("longitude") as? Double
                        val coordinates = if (latitude != null && longitude != null) Coordinates(latitude, longitude) else null
                        Restaurant(
                            name = name,
                            imageUrl = imageUrl,
                            address = address,
                            rating = rating,
                            price = price,
                            phone = phone,
                            coordinates = coordinates
                        )
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meals:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = {
                                    saveMeals(mealInputState, history) {
                                        isSaved = true
                                    }
                                },
                                enabled = hasEdits && !isSaved
                            ) {
                                Text(buttonText)
                            }
                        }

                        // Expandable Card for Meals
                        ExpandableCard(title = "Meals") {
                            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
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

                        // Expandable Card for Restaurants
                        ExpandableCard(title = "Nearby Restaurants") {
                            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                items(history.restaurants.filter { it.name.trim() != "-" }) { restaurant ->
                                    // Pass the current userLocation to RestaurantItem
                                    RestaurantItem(restaurant, userLocation)
                                }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
fun RestaurantItem(restaurant: Restaurant, userLocation: Location?) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // Compute the distance (in kilometers) if both userLocation and restaurant coordinates are available.
    val distanceKm = remember(userLocation, restaurant.coordinates) {
        userLocation?.let { loc ->
            restaurant.coordinates?.let { coords ->
                val d = calculateDistance(userLocation, coords.latitude, coords.longitude)
                println("DEBUG: Distance computed: $d km")
                d
            }
        }
    }

    val expandedContent = @Composable {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Address: ${restaurant.address.ifEmpty { "Not Available" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Phone: ${restaurant.phone.takeIf { it.isNotBlank() } ?: "Not Available"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Rating: ${if (restaurant.rating == 0.0) "Not Available" else "${restaurant.rating} / 5"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Price: ${restaurant.price.takeIf { it.isNotBlank() } ?: "Not Available"}",
                style = MaterialTheme.typography.bodyMedium
            )
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
                model = if (restaurant.imageUrl.isEmpty()) null else restaurant.imageUrl,
                placeholder = painterResource(id = R.drawable.default_restaurant),
                error = painterResource(id = R.drawable.default_restaurant),
                fallback = painterResource(id = R.drawable.default_restaurant),
                contentDescription = restaurant.name,
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Append distance (if available) to the restaurant name.
            val distanceText = if (distanceKm != null) " (${String.format("%.1f", distanceKm)} km)" else "(N/A)"
            Text(
                text = restaurant.name + distanceText,
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
