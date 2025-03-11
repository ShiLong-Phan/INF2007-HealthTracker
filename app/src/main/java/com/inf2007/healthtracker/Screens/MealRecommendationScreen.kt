package com.inf2007.healthtracker.Screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.inf2007.healthtracker.utilities.*
import com.inf2007.healthtracker.BuildConfig
import com.inf2007.healthtracker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.content.Intent
import android.net.Uri
import android.location.Location
import android.util.Log
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionGranted: @Composable () -> Unit
) {
    val locationPermissionState = rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

    when {
        locationPermissionState.status.isGranted -> {
            // Permission is granted; display the content that needs location
            onPermissionGranted()
        }

        locationPermissionState.status.shouldShowRationale -> {
            // User denied once, but not permanently ("Don't ask again" not checked)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Location Permission",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Location permission is needed to show restaurant distances",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { locationPermissionState.launchPermissionRequest() },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Allow Access")
                }
            }
        }

        else -> {
            // Permission not yet requested OR permanently denied
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOff,
                    contentDescription = "Location Permission Needed",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "We need your location to show nearby restaurants with accurate distances",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { locationPermissionState.launchPermissionRequest() },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun EmptyRestaurantsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.NoMeals,
                contentDescription = "No Restaurants",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No restaurant recommendations available",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EnhancedRestaurantItem(business: Business, userLocation: Location?) {
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

    val ratingColor = when {
        business.rating >= 4.5 -> Color(0xFF4CAF50) // Excellent - Green
        business.rating >= 4.0 -> Color(0xFF8BC34A) // Very Good - Light Green
        business.rating >= 3.5 -> Color(0xFFFFEB3B) // Good - Yellow
        business.rating >= 3.0 -> Color(0xFFFFC107) // Average - Amber
        business.rating > 0 -> Color(0xFFFF9800) // Below Average - Orange
        else -> Color.Gray // No Rating
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Restaurant header with image and basic info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restaurant image
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(80.dp)
                ) {
                    AsyncImage(
                        model = if (business.image_url.isEmpty()) null else business.image_url,
                        placeholder = painterResource(id = R.drawable.default_restaurant),
                        error = painterResource(id = R.drawable.default_restaurant),
                        fallback = painterResource(id = R.drawable.default_restaurant),
                        contentDescription = business.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Restaurant details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = business.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Distance with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Distance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val distanceText = if (distanceKm != null) {
                            val formattedDistance = String.format(Locale.getDefault(), "%.1f", distanceKm)
                            "$formattedDistance km away"
                        } else {
                            "Distance unavailable"
                        }
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating with colored badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (business.rating > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ratingColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = business.rating.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            // Price indicator
                            if (!business.price.isNullOrBlank()) {
                                Text(
                                    text = business.price,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = "No rating available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Expand/collapse icon
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more"
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )

                    // Address
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = "Address",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = business.location.address1 ?: "Address not available",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Phone
                    if (!business.phone.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${business.phone}")
                                    }
                                    context.startActivity(intent)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Phone",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = business.phone,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Open in Maps button
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(business.name)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "Open in Maps"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in Google Maps")
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileSummaryCard(
    calorieIntake: Int,
    age: Int,
    weight: Int,
    height: Int,
    gender: String,
    activityLevel: String,
    dietaryPreference: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Your Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calorie goal highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalDining,
                            contentDescription = "Calorie Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            "Daily Calorie Goal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$calorieIntake calories",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile details in two columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileDetail(
                        icon = Icons.Filled.Cake,
                        label = "Age",
                        value = "$age years"
                    )

                    ProfileDetail(
                        icon = Icons.Filled.Scale,
                        label = "Weight",
                        value = "$weight kg"
                    )

                    ProfileDetail(
                        icon = Icons.Filled.Height,
                        label = "Height",
                        value = "$height cm"
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileDetail(
                        icon = Icons.Filled.Face,
                        label = "Gender",
                        value = gender
                    )

                    ProfileDetail(
                        icon = Icons.Filled.DirectionsRun,
                        label = "Activity",
                        value = activityLevel
                    )

                    ProfileDetail(
                        icon = Icons.Filled.SetMeal,
                        label = "Diet",
                        value = dietaryPreference
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetail(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun SaveButton(
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Saving...",
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = "Save Meal Plan"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Save Meal Plan",
                style = MaterialTheme.typography.titleMedium
            )
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
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firestore = FirebaseFirestore.getInstance()
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Store user's location
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

                // ✅ Extract keywords from each meal in the plan
                val searchTerms = aiMealPlan
                    .flatMap { meal ->
                        // Print the meal for debugging
                        println("DEBUG: Processing Meal = $meal")

                        // Split the meal into lines and extract dish names
                        val dishNames = meal.split("\n")
                            .filter { it.startsWith("-") } // Filter for ingredient lines
                            .map { it.substringBefore("(").trim() } // Extract dish names
                            .map { dishName ->
                                // Print the extracted dish name for debugging
                                println("DEBUG: Extracted Dish Name = $dishName")
                                dishName
                            }

                        // Map dish names to search terms
                        dishNames.map { getYelpSearchTerm(it) }
                    }
                    .filterNot { it == "Healthy food" } // Remove generic fallbacks
                    .distinct() // Remove duplicates
                    .joinToString(", ") // Combine into a single search term

                // Print the extracted keywords for debugging
                println("DEBUG: Extracted Keywords = $searchTerms")

                val finalSearchTerm = if (searchTerms.isEmpty()) "Restaurants" else searchTerms

                // Print the final search term for debugging
                println("DEBUG: Final Search Term = $finalSearchTerm")

                // ✅ Fetch Restaurant Recommendations from Yelp
                val apiKey = BuildConfig.yelpApiKey
                val yelpResponse1 = withContext(Dispatchers.IO) {
                    // First call: Search based on keywords
                    if (userLocation == null) {
                        YelpApi.searchRestaurants(
                            location = "Singapore",
                            term = finalSearchTerm, // Use combined search terms
                            categories = "", // No category restriction
                            limit = 10, // Limit to 10 results
                            apiKey = apiKey
                        )
                    } else {
                        YelpApi.searchRestaurants(
                            latitude = userLocation!!.latitude,
                            longitude = userLocation!!.longitude,
                            location = "Singapore",
                            term = finalSearchTerm, // Use combined search terms
                            categories = "", // No category restriction
                            limit = 10, // Limit to 10 results
                            apiKey = apiKey
                        )
                    }
                }

                val yelpResponse2 = withContext(Dispatchers.IO) {
                    // Second call: Fallback to healthy restaurants
                    if (userLocation == null) {
                        YelpApi.searchRestaurants(
                            location = "Singapore",
                            term = "Healthy restaurants", // Fallback search term
                            categories = "healthy", // Restrict to healthy restaurants
                            limit = 10, // Limit to 10 results
                            apiKey = apiKey
                        )
                    } else {
                        YelpApi.searchRestaurants(
                            latitude = userLocation!!.latitude,
                            longitude = userLocation!!.longitude,
                            location = "Singapore",
                            term = "Healthy restaurants", // Fallback search term
                            categories = "healthy", // Restrict to healthy restaurants
                            limit = 10, // Limit to 10 results
                            apiKey = apiKey
                        )
                    }
                }

                // Combine results from both calls
                val parsedYelpResponse1 = Gson().fromJson(yelpResponse1, YelpResponse::class.java)
                val parsedYelpResponse2 = Gson().fromJson(yelpResponse2, YelpResponse::class.java)

                // Step 1: Prioritize yelpResponse1 restaurants (up to 10)
                val prioritizedRestaurants = parsedYelpResponse1.businesses.take(10)

                // Step 2: If fewer than 10 restaurants, add from yelpResponse2
                val remainingSlots = 10 - prioritizedRestaurants.size
                val additionalRestaurants = if (remainingSlots > 0) {
                    parsedYelpResponse2.businesses
                        .filterNot { it in prioritizedRestaurants } // Avoid duplicates
                        .take(remainingSlots)
                } else {
                    emptyList()
                }

                // Step 3: Combine the lists
                val combinedRestaurants = prioritizedRestaurants + additionalRestaurants

                // Step 4: Sort the final 10 restaurants by distance
                restaurantRecommendations = combinedRestaurants.sortedBy { business ->
                    userLocation?.let { loc ->
                        business.coordinates?.let { coords ->
                            calculateDistance(loc, coords.latitude, coords.longitude)
                        } ?: Double.MAX_VALUE
                    } ?: Double.MAX_VALUE
                }

                println("DEBUG: Got ${restaurantRecommendations.size} restaurants from Yelp.")
                restaurantRecommendations.forEach { business ->
                    println("DEBUG: ${business.name} => coords=${business.coordinates}")
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
                age = document.getLong("age")?.toInt() ?: 0
                gender = document.getString("gender") ?: "Male"
                weight = document.getLong("weight")?.toInt() ?: 0
                height = document.getLong("height")?.toInt() ?: 0
                activityLevel = document.getString("activity_level") ?: "Sedentary"
                dietaryPreference = document.getString("dietary_preference") ?: "None"
                calorieIntake = document.getLong("calorie_intake")?.toInt() ?: 0
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
                Log.d("Location Update", "Live location update = $newLocation")
            }
        }
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "AI Meal Plan",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
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
                    }
                )
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Generating your personalized meal plan...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (errorMessage.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = ""
                            fetchMealAndRestaurants()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Try Again"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        } else {
            Box {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // User Profile Summary
                    item {
                        UserProfileSummaryCard(
                            calorieIntake = calorieIntake,
                            age = age,
                            weight = weight,
                            height = height,
                            gender = gender,
                            activityLevel = activityLevel,
                            dietaryPreference = dietaryPreference
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Meal Plan Section - UPDATED
                    item {
                        PersonalizedMealPlanCard(
                            aiMealPlan = aiMealPlan
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Nearby Restaurants Section
                    item {
                        RestaurantsCard(
                            restaurants = restaurantRecommendations,
                            userLocation = userLocation
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        SaveButton(
                            onClick = {
                                isSaving = true
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
                                                "price" to business.price,
                                                "coordinates" to business.coordinates?.let { coords ->
                                                    mapOf(
                                                        "latitude" to coords.latitude,
                                                        "longitude" to coords.longitude
                                                    )
                                                }
                                            )
                                        },
                                        "calorieGoal" to calorieIntake
                                    )
                                    firestore.collection("mealHistory")
                                        .add(mealHistoryData)
                                        .addOnSuccessListener {
                                            isSaving = false
                                            showSuccessMessage = true

                                            // Show success animation, then navigate after delay
                                            coroutineScope.launch {
                                                // Wait for animation to display
                                                delay(1500)
                                                // Navigate to history screen
                                                navController.navigate("meal_plan_history_screen")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isSaving = false
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Failed to save meal history: ${e.message}")
                                            }
                                        }
                                }
                            },
                            isLoading = isSaving
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Show scroll to top button when scrolled down
                // Success message overlay
                AnimatedVisibility(
                    visible = showSuccessMessage,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Meal plan saved successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalizedMealPlanCard(
    aiMealPlan: List<String>
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Meal Plan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            // Information note
            Text(
                "AI-generated meal plan based on your preferences",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Meal content - collapsible
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    if (aiMealPlan.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No meal plan generated yet. Try refreshing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        aiMealPlan.forEachIndexed { index, meal ->
                            if (index > 0) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }

                            MealContentDisplay(meal = meal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealContentDisplay(meal: String) {
    val paragraphs = meal.split("\n\n")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Process the meal text considering various formats
        if (meal.contains("\n")) {
            // Process multiline content
            val lines = meal.split("\n")
            lines.forEach { line ->
                if (line.trim().startsWith("-") || line.trim().startsWith("•") || line.matches(Regex("^\\d+\\..*"))) {
                    // This is a bullet or numbered list item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Extract the bullet or number and use it as the prefix
                        val bulletMatch = Regex("^(•|-|\\d+\\.)\\s*(.*)$").find(line.trim())
                        if (bulletMatch != null) {
                            val (bullet, content) = bulletMatch.destructured
                            Text(
                                text = bullet + " ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = line.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (line.length < 50 && (line.endsWith(":") || line.uppercase() == line)) {
                    // This looks like a heading or section title
                    Text(
                        text = line.trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                } else if (line.trim().isNotEmpty()) {
                    // Regular text line
                    Text(
                        text = line.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        } else {
            // Simple single paragraph
            Text(
                text = meal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RestaurantsCard(
    restaurants: List<Business>,
    userLocation: Location?
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = "Restaurants",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Nearby Restaurants",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            // Restaurant list content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    val validRestaurants = restaurants.filter { it.name.trim() != "-" }

                    if (validRestaurants.isEmpty()) {
                        EmptyRestaurantsMessage()
                    } else {
                        validRestaurants.forEach { business ->
                            EnhancedRestaurantItem(business, userLocation)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}