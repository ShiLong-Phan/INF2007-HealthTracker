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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import com.inf2007.healthtracker.ui.theme.Primary
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.Manifest
import com.google.android.gms.location.LocationRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionGranted: @Composable () -> Unit
) {
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    when {
        locationPermissionState.status.isGranted -> {
            // Permission is granted; display the content that needs location
            onPermissionGranted()
        }

        locationPermissionState.status.shouldShowRationale -> {
            // User denied once, but not permanently ("Don't ask again" not checked)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Location permission is needed to show distances. Please grant permission.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                    Text("Allow")
                }
            }
        }

        else -> {
            // Permission not yet requested OR permanently denied
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Grant location permission to see distance.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

/**
 * Continuously request location updates (live GPS) using a DisposableEffect.
 * [onLocationUpdated] will be called each time we receive a new location fix.
 */
@Composable
fun StartLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationUpdated: (Location) -> Unit
) {
    // Configure interval and priority
    val locationRequest = remember {
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // 5 seconds
        ).build()
    }

    // Callback to handle location updates
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { loc ->
                    onLocationUpdated(loc)
                }
            }
        }
    }

    // Start updates in a DisposableEffect so we can stop them automatically
    DisposableEffect(Unit) {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            android.os.Looper.getMainLooper()
        )

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealRecommendationScreen(
    navController: NavController,
    userId: String
) {
    var age by remember { mutableStateOf(0) }
    var gender by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(0) }
    var activityLevel by remember { mutableStateOf("") }
    var dietaryPreference by remember { mutableStateOf("") }
    var calorieIntake by remember { mutableStateOf(0) }
    var aiMealPlan by remember { mutableStateOf<List<String>>(emptyList()) }
    var restaurantRecommendations by remember { mutableStateOf<List<Business>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firestore = FirebaseFirestore.getInstance()

    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Store user’s location
    var userLocation by remember { mutableStateOf<Location?>(null) }


    // ✅ Use Gemini AI SDK
    val geminiService = remember { GeminiService(BuildConfig.geminiApiKey) }

    // Function to fetch data
    val fetchMealAndRestaurants = {
        coroutineScope.launch {
            try {
                // ✅ Generate Meal Plan Using AI
                aiMealPlan = geminiService.generateMealPlan(
                    age,
                    weight,
                    height,
                    gender,
                    activityLevel,
                    dietaryPreference,
                    calorieIntake
                )

                // ✅ Convert AI-generated meal to a Yelp-friendly search term
                val searchTerm = getYelpSearchTerm(aiMealPlan.firstOrNull())

                // ✅ Fetch Restaurant Recommendations from Yelp
                if (searchTerm.isNotEmpty()) {
                    val apiKey = BuildConfig.yelpApiKey
                    val yelpResponse = withContext(Dispatchers.IO) {
                        if (userLocation == null) {
                            YelpApi.searchRestaurants(
                                location = "Singapore",
                                term = searchTerm, // ✅ Use mapped search term
                                categories = "healthy,restaurants", // Restrict search to healthy restaurants
                                limit = 10,
                                apiKey = apiKey
                            )
                        } else{
                            YelpApi.searchRestaurants(
                                latitude = userLocation!!.latitude,
                                longitude = userLocation!!.longitude,
                                location = "Singapore",
                                term = searchTerm, // ✅ Use mapped search term
                                categories = "healthy,restaurants", // Restrict search to healthy restaurants
                                limit = 10,
                                apiKey = apiKey
                            )
                        }

                    }

                    if (yelpResponse != null) {
                        val parsedYelpResponse =
                            Gson().fromJson(yelpResponse, YelpResponse::class.java)
                        restaurantRecommendations = parsedYelpResponse.businesses
                        println("DEBUG: Got ${restaurantRecommendations.size} restaurants from Yelp.")
                        restaurantRecommendations.forEach { business ->
                            println("DEBUG: ${business.name} => coords=${business.coordinates}")
                        }
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

    // Run the fetch function once when the screen is first loaded
    LaunchedEffect(userId) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                age = (document.getString("age")?.toInt() ?: 23)
                gender = document.getString("gender") ?: "Male"
                weight = document.getLong("weight")?.toInt() ?: 70
                height = document.getLong("height")?.toInt() ?: 170
                activityLevel = document.getString("activity_level") ?: "Moderate"
                dietaryPreference = document.getString("dietary_preference") ?: "None"
                calorieIntake = document.getLong("calorie_intake")?.toInt() ?: 2000
                fetchMealAndRestaurants()
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching user data: ${exception.message}"
                isLoading = false
            }
    }

    // Request location permission and start live location updates if granted
    RequestLocationPermission(
        onPermissionGranted = {
            // Once permission is granted, start requesting continuous updates
            StartLocationUpdates(
                fusedLocationClient = fusedLocationClient
            ) { newLocation ->
                userLocation = newLocation
                println("DEBUG: Live location update = $newLocation")
            }
        }
    )

//    LaunchedEffect(showSuccessMessage) {
//        if (showSuccessMessage) {
//            delay(500) // Wait for 0.5 seconds
//            navController.navigate("main_screen") {
//                popUpTo("main_screen") { inclusive = true }
//            }
//        }
//    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("AI Meal Plan") },
                    modifier = Modifier.padding(horizontal = 24.dp),
                    actions = {
                        IconButton(onClick = { navController.navigate("meal_plan_history_screen") }) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "Past Saved Meal Plans"
                            )
                        }
                        IconButton(onClick = {
                            isLoading = true
                            fetchMealAndRestaurants()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    })
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        bottomBar = { BottomNavigationBar(navController) }

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
            Column(modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 40.dp, vertical = 16.dp)) {
                Text("Meal Plan", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).weight(1f)) {
                    items(aiMealPlan) { meal ->
                        Text("- $meal", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Nearby Restaurants", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).weight(1f)) {
                    items(restaurantRecommendations) { business ->
                        RestaurantItem(business, userLocation)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let {
                            val mealHistoryData = hashMapOf(
                                "uid" to it.uid,
                                "date" to Date(),
                                "meals" to aiMealPlan,
                                "restaurants" to restaurantRecommendations.map { business ->
                                    mapOf(
                                        "name" to business.name,
                                        "imageUrl" to business.image_url,
                                        "address" to business.location.address1,
                                        "rating" to business.rating,
                                        "phone" to business.phone,
                                        "price" to business.price
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Text("Save Meal History")
                }
            }
        }
    }
}

@Composable
fun RestaurantItem(business: Business, userLocation: Location?) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) } // Track expanded state

    // Calculate distance if both user location and restaurant coordinates are present
    val distanceKm = remember(userLocation, business.coordinates) {
        userLocation?.let { loc ->
            val lat = business.coordinates?.latitude
            val lng = business.coordinates?.longitude
            if (lat != null && lng != null) {
                calculateDistance(loc, lat, lng)
            } else null
        }
    }
    println("DEBUG: Business=${business.name}, userLoc=$userLocation, coords=${business.coordinates}, distance=$distanceKm")

    val expandedContent = @Composable {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Address: ${business.location.address1 ?: "Not Available"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = "Phone: ${business.phone?.takeIf { it.isNotBlank() } ?: "Not Available"}",
                style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Rating: ${if (business.rating == 0.0) "Not Available" else "${business.rating} / 5"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = "Price: ${business.price?.takeIf { it.isNotBlank() } ?: "Not Available"}",
                style = MaterialTheme.typography.bodyMedium)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { expanded = !expanded }, // Toggle expanded state when clicked
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
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
                // Display business name and distance
                val distanceText = if (distanceKm != null) {
                    val formattedDistance = String.format(Locale.getDefault(), "%.1f", distanceKm)
                    " ($formattedDistance km)"
                } else {
                    " (N/A)"  // Show "N/A" if distance is null
                }

                Text(
                    text = business.name + distanceText,
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
}


fun calculateDistance(
    userLocation: Location,
    restaurantLat: Double,
    restaurantLng: Double
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        userLocation.latitude,
        userLocation.longitude,
        restaurantLat,
        restaurantLng,
        results
    )
    // Convert meters to kilometers
    return results[0] / 1000.0
}
