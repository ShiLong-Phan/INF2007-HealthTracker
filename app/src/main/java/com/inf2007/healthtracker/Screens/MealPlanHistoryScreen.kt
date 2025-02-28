package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.graphics.Color
import com.inf2007.healthtracker.ui.theme.Secondary
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.ui.theme.Unfocused
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.utilities.MealHistory
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip

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

    // Function to filter the meal history by date
    fun filterMealHistory(query: String) {
        if (query.isEmpty()) {
            filteredMealHistory = mealHistory
        } else {
            val lowerCaseQuery = query.lowercase()
            filteredMealHistory = mealHistory.filter {
                val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(it.date).lowercase()
                formattedDate.contains(lowerCaseQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan History") },
                modifier = Modifier.padding(horizontal = 24.dp),
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }

    ) { paddingValues ->
        Column(modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            // Search Bar
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        filterMealHistory(query)
                    },
                    label = { Text("Search by Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            } else if (filteredMealHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No meal plan found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            },
                            dismissContent = {
                                MealHistoryItem(navController, history)
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
fun MealHistoryItem(navController: NavController, history: MealHistory) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) // Formats the date
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Formats the time
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { navController.navigate("meal_plan_history_detail/${history.uid}/${history.date.time}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Secondary, contentColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column (
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ){
                // Date and Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateFormat.format(history.date),
                        style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.secondaryContainer)
                    )

                    Text(
                        text = timeFormat.format(history.date),
                        style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.secondaryContainer)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Meals
                Text(
                    text = "Meals: ${history.meals.size} items",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Restaurants
                Text(
                    text = "Restaurants: ${history.restaurants.size} places",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}