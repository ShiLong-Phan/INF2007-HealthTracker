package com.inf2007.healthtracker.Screens

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    // Track if the app is in edit mode
    var isEditMode by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    // Obtain location updates for calculating distance.
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // Request location permission and start live location updates if granted
    RequestLocationPermission(
        onPermissionGranted = {
            // Once permission is granted, start requesting continuous updates
            MealDetailLocationUpdates(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Meal History Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (hasEdits && !isSaved) {
                                mealHistory?.let { history ->
                                    saveMeals(mealInputState, history) {
                                        isSaved = true
                                        isEditMode = false
                                    }
                                }
                            } else {
                                isEditMode = !isEditMode
                                isSaved = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (hasEdits && !isSaved) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (hasEdits && !isSaved) "Save" else "Edit"
                        )
                    }
                },
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading meal history...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            errorMessage.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            else -> {
                // Use LazyColumn as the root container
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxSize()
                ) {
                    // Date Section
                    item {
                        mealHistory?.let { history ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Date",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(history.date),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(history.date),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Meal Section
                    item {
                        mealHistory?.let { history ->
                            SectionCard(
                                title = "Meal Plan",
                                icon = Icons.Default.Restaurant,
                                expandable = true
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Information note
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Text(
                                            "AI-generated meal plan based on your preferences",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }

                                    // Actions for edit mode
                                    if (hasEdits && !isSaved) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    saveMeals(mealInputState, history) {
                                                        isSaved = true
                                                        isEditMode = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text("Save")
                                            }
                                        }
                                    } else if (!isEditMode) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = { isEditMode = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text("Edit")
                                            }
                                        }
                                    }

                                    // Direct display of meals without categorization
                                    updatedMeals.forEachIndexed { index, meal ->
                                        if (meal.isNotBlank()) {
                                            if (index > 0) {
                                                Divider(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                            }

                                            EditableMealItem(
                                                meal = meal,
                                                isEditMode = isEditMode,
                                                onMealChange = { updatedMeal ->
                                                    updatedMeals[index] = updatedMeal
                                                    mealInputState[index] = updatedMeal
                                                    isSaved = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Restaurant Section
                    item {
                        mealHistory?.let { history ->
                            if (history.restaurants.any { it.name.trim() != "-" }) {
                                SectionCard(
                                    title = "Nearby Restaurants",
                                    icon = Icons.Default.LocationOn,
                                    expandable = true
                                ) {
                                    // Show restaurants
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        history.restaurants.filter { it.name.trim() != "-" }.forEach { restaurant ->
                                            RestaurantItem(restaurant, userLocation)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expandable: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = expandable) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (expandable) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Content
            if (!expandable || expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun EditableMealItem(
    meal: String,
    isEditMode: Boolean,
    onMealChange: (String) -> Unit
) {
    var mealText by remember { mutableStateOf(meal) }

    if (isEditMode) {
        OutlinedTextField(
            value = mealText,
            onValueChange = {
                mealText = it
                onMealChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            maxLines = 10
        )
    } else {
        val paragraphs = meal.split("\n\n")
        Column(modifier = Modifier.fillMaxWidth()) {
            paragraphs.forEachIndexed { index, paragraph ->
                if (paragraph.isNotBlank()) {
                    if (paragraph.startsWith("•") || paragraph.startsWith("-") || paragraph.matches(Regex("^\\d+\\..*"))) {
                        // This looks like a list item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Extract the bullet or number and use it as the prefix
                            val bulletMatch = Regex("^(•|-|\\d+\\.)\\s*(.*)$").find(paragraph)
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
                                    text = paragraph,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else if (paragraph.length < 50 && (paragraph.endsWith(":") || paragraph.uppercase() == paragraph)) {
                        // This looks like a heading or section title
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    } else {
                        // Regular paragraph
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Restaurant header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restaurant image
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = if (restaurant.imageUrl.isEmpty()) null else restaurant.imageUrl,
                        placeholder = painterResource(id = R.drawable.default_restaurant),
                        error = painterResource(id = R.drawable.default_restaurant),
                        fallback = painterResource(id = R.drawable.default_restaurant),
                        contentDescription = restaurant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Append distance (if available) to the restaurant name.
                    val distanceText = if (distanceKm != null) " (${String.format("%.1f", distanceKm)} km)" else ""
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (distanceKm != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Distance",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", distanceKm)} km away",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }

                    // Rating display if available
                    if (restaurant.rating > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${restaurant.rating}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Expanded content
            if (expanded) {
                Divider(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    // Address
                    RestaurantDetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Address",
                        value = restaurant.address.ifEmpty { "Not Available" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phone
                    RestaurantDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = restaurant.phone.takeIf { it.isNotBlank() } ?: "Not Available"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price
                    RestaurantDetailRow(
                        icon = Icons.Default.AttachMoney,
                        label = "Price",
                        value = restaurant.price.takeIf { it.isNotBlank() } ?: "Not Available"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Maps button
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(restaurant.name)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open in Maps",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Google Maps")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ExpandableCard(title: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            // Content
            if (expanded) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun MealDetailLocationUpdates(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationUpdate: (Location) -> Unit
) {
    val locationCallback = remember {
        object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                for (location in locationResult.locations) {
                    onLocationUpdate(location)
                }
            }
        }
    }

    DisposableEffect(fusedLocationClient) {
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("Location", "Error requesting location updates: ${e.message}")
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}