package com.inf2007.healthtracker.Screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.ui.theme.Secondary
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.utilities.MealHistory
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MealPlanHistoryScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var mealHistory by remember { mutableStateOf<List<MealHistory>>(emptyList()) }
    var filteredMealHistory by remember { mutableStateOf<List<MealHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    // Track the item pending deletion confirmation.
    var pendingDeleteItem by remember { mutableStateOf<MealHistory?>(null) }

    // For searching
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) } // Track search state
    var isDateRangeSearch by remember { mutableStateOf(false) } // Track date range search state

    // Expanded states for items
    var expandedItems by remember { mutableStateOf(setOf<String>()) }

    // Date range picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Initialize with today and a week ago
    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val weekAgo = calendar.timeInMillis

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = null, initialDisplayMode = DisplayMode.Picker)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = null, initialDisplayMode = DisplayMode.Picker)

    // Convert date picker states to actual dates
    val startDate = startDatePickerState.selectedDateMillis?.let { Date(it) }
    val endDate = endDatePickerState.selectedDateMillis?.let {
        // Set end date to end of day (23:59:59)
        val cal = Calendar.getInstance()
        cal.timeInMillis = it
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        Date(cal.timeInMillis)
    }

    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        user?.let {
            firestore.collection("mealHistory")
                .whereEqualTo("uid", it.uid)
                .orderBy("date")
                .get()
                .addOnSuccessListener { historySnapshot ->
                    mealHistory = historySnapshot.documents.mapNotNull { doc ->
                        // Map Firestore document into MealHistory and capture the document ID.
                        doc.toObject(MealHistory::class.java)?.copy(documentId = doc.id)
                    }
                    // Initially set the filtered list as the entire list
                    filteredMealHistory = mealHistory
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Failed to retrieve meal history: ${exception.message}"
                }
                .addOnCompleteListener { isLoading = false }
        }
    }

    fun filterMealHistory() {
        if (isDateRangeSearch) {
            val startDate = startDatePickerState.selectedDateMillis?.let { Date(it) }
            val endDate = endDatePickerState.selectedDateMillis?.let {
                // Set end date to the end of the day (23:59:59)
                val cal = Calendar.getInstance()
                cal.timeInMillis = it
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Date(cal.timeInMillis)
            }

            if (startDate != null && endDate != null) {
                filteredMealHistory = mealHistory.filter {
                    val historyDate = it.date
                    historyDate != null && historyDate >= startDate && historyDate <= endDate
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            val lowerCaseQuery = searchQuery.lowercase()
            filteredMealHistory = mealHistory.filter {
                // Check for matching date in various formats
                val formattedDates = listOf(
                    SimpleDateFormat("MMM d yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("d MMM, yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("d MMM", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("MMMM d yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it.date).lowercase(),
                    SimpleDateFormat("d MMMM, yyyy", Locale.getDefault()).format(it.date).lowercase()
                )

                // Return true if any formatted date matches the search query
                formattedDates.any { formattedDate -> formattedDate.contains(lowerCaseQuery) }
            }
        } else {
            filteredMealHistory = mealHistory
        }
    }

    // Clear Date Range Filter
    fun clearDateRangeFilter() {
        startDatePickerState.selectedDateMillis = null
        endDatePickerState.selectedDateMillis = null
        isDateRangeSearch = false
        searchQuery = ""
        filteredMealHistory = mealHistory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan History", fontWeight = FontWeight.Bold) },
                modifier = Modifier.padding(horizontal = 24.dp),
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search"
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }

    ) { paddingValues ->
        Column(modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            if (isSearchActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !isDateRangeSearch,
                            onClick = {
                                isDateRangeSearch = false
                                filterMealHistory()
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("Single Date") }

                        SegmentedButton(
                            selected = isDateRangeSearch,
                            onClick = {
                                isDateRangeSearch = true
                                filterMealHistory()
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("Date Range") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDateRangeSearch) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = startDate?.let { dateFormatter.format(it) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Start Date") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = { showStartDatePicker = true }) {
                                        Icon(Icons.Filled.CalendarMonth, "Select start date")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedTextField(
                                value = endDate?.let { dateFormatter.format(it) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("End Date") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = { showEndDatePicker = true }) {
                                        Icon(Icons.Filled.CalendarMonth, "Select end date")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { filterMealHistory() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Apply Filter") }

                            Button(
                                onClick = { clearDateRangeFilter() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) { Text("Clear Filter") }
                        }
                    } else {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                filterMealHistory()
                            },
                            label = { Text("Search by Date") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        filterMealHistory()
                                    }) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
                                }
                            }
                        )
                    }
                }
            }

            // Show date picker dialogs
            if (showStartDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showStartDatePicker = false
                            filterMealHistory()
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = startDatePickerState)
                }
            }

            if (showEndDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showEndDatePicker = false
                            filterMealHistory()
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = endDatePickerState)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    }
                }
            } else if (filteredMealHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.NoMeals,
                            contentDescription = "No Meals",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No meal plans found.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Summary card for the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    SummaryCard(filteredMealHistory)
                }

                // Date range info when filter is active
                if (isDateRangeSearch && startDate != null && endDate != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = "Date Range",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Date Range: ${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Your Meal Plans",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    items(
                        items = filteredMealHistory,
                        key = { history -> history.documentId } // Use documentId as the unique key.
                    ) { history ->
                        // Each item has its own dismiss state.
                        val dismissState = rememberDismissState(
                            confirmStateChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    // Instead of immediately deleting, store the item pending confirmation.
                                    pendingDeleteItem = history
                                }
                                // Return false to prevent auto-dismiss.
                                false
                            }
                        )

                        val isExpanded = expandedItems.contains(history.documentId)

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                val color = if (dismissState.targetValue == DismissValue.Default)
                                    MaterialTheme.colorScheme.surface
                                else
                                    MaterialTheme.colorScheme.error

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(color)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            },
                            dismissContent = {
                                Column {
                                    EnhancedMealHistoryItem(
                                        navController = navController,
                                        history = history,
                                        isExpanded = isExpanded,
                                        onToggleExpand = {
                                            expandedItems = if (isExpanded) {
                                                expandedItems - history.documentId
                                            } else {
                                                expandedItems + history.documentId
                                            }
                                        }
                                    )

                                    // Expanded content
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(),
                                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically()
                                    ) {
                                        MealHistoryExpandedContent(
                                            history = history,
                                            navController = navController
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Display a confirmation dialog if an item is pending deletion.
            pendingDeleteItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { pendingDeleteItem = null },
                    title = { Text("Delete Meal History") },
                    text = { Text("Are you sure you want to delete this meal history?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                firestore.collection("mealHistory")
                                    .document(item.documentId)
                                    .delete()
                                    .addOnSuccessListener {
                                        // Remove from local state on successful deletion.
                                        mealHistory =
                                            mealHistory.filter { it.documentId != item.documentId }
                                        // Also update the filtered list.
                                        filteredMealHistory =
                                            filteredMealHistory.filter { it.documentId != item.documentId }
                                    }
                                    .addOnFailureListener { exception ->
                                        errorMessage = "Deletion failed: ${exception.message}"
                                    }
                                pendingDeleteItem = null
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteItem = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(mealHistory: List<MealHistory>) {
    val totalPlans = mealHistory.size
    val avgCalorieGoal = if (mealHistory.isNotEmpty()) {
        mealHistory.sumOf { it.calorieGoal } / mealHistory.size
    } else 0

    val totalRestaurants = mealHistory.flatMap { it.restaurants }
        .filter { it.name.trim() != "-" }
        .size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Reduced elevation
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp), // Reduced padding
        ) {
            Text(
                "Meal Plan Summary",
                style = MaterialTheme.typography.titleMedium.copy( // Smaller title
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Total Plans - in column format
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp) // Smaller icon
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = "Meal Plans",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp) // Smaller icon
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$totalPlans",
                        style = MaterialTheme.typography.titleSmall.copy( // Smaller text
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Plans",
                        style = MaterialTheme.typography.bodySmall, // Smaller text
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                )

                // Average Calorie Goal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp) // Smaller icon
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalDining,
                            contentDescription = "Calories",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp) // Smaller icon
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$avgCalorieGoal",
                        style = MaterialTheme.typography.titleSmall.copy( // Smaller text
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Avg Calories",
                        style = MaterialTheme.typography.bodySmall, // Smaller text
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                )

                // Total Restaurants
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp) // Smaller icon
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = "Restaurants",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp) // Smaller icon
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$totalRestaurants",
                        style = MaterialTheme.typography.titleSmall.copy( // Smaller text
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Restaurants",
                        style = MaterialTheme.typography.bodySmall, // Smaller text
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedMealHistoryItem(
    navController: NavController,
    history: MealHistory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Determine color based on calorie goal
    val calorieColor = when {
        history.calorieGoal > 2500 -> Color(0xFFE57373) // High calories - reddish
        history.calorieGoal > 1800 -> Color(0xFFFFB74D) // Medium calories - orange
        else -> Color(0xFF81C784) // Low calories - greenish
    }

    // Count valid restaurants
    val validRestaurants = history.restaurants.filter { it.name.trim() != "-" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = calorieColor.copy(alpha = 0.1f)
            )
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date and Time Row with Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    calorieColor.copy(alpha = 0.7f),
                                    calorieColor.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "Meal Plan",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateFormat.format(history.date),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = timeFormat.format(history.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Main content row - using Column instead of Row to stack items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Calories section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.LocalDining,
                                contentDescription = "Calories",
                                tint = calorieColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("${history.calorieGoal}")
                                    }
                                    append(" calories")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Restaurants section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Restaurant,
                                contentDescription = "Restaurants",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("${validRestaurants.size}")
                                    }
                                    append(" restaurants")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Expand/collapse indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MealHistoryExpandedContent(
    history: MealHistory,
    navController: NavController
) {
    val validRestaurants = history.restaurants.filter { it.name.trim() != "-" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Restaurants in this plan",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (validRestaurants.isEmpty()) {
                Text(
                    "No restaurants added to this plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                validRestaurants.take(3).forEachIndexed { index, restaurant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Show indicator if there are more restaurants
                if (validRestaurants.size > 3) {
                    Text(
                        text = "+ ${validRestaurants.size - 3} more restaurants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View details button
            Button(
                onClick = { navController.navigate("meal_plan_history_detail/${history.uid}/${history.date.time}") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Full Details")
            }
        }
    }
}