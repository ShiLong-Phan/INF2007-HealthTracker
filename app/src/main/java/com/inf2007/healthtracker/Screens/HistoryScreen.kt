package com.inf2007.healthtracker.Screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.inf2007.healthtracker.ui.theme.Secondary
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.material.icons.filled.LocalDining
import com.inf2007.healthtracker.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var foodEntriesHistory by remember { mutableStateOf<List<FoodEntry2>>(emptyList()) }
    var stepsHistory by remember { mutableStateOf<List<StepsEntry>>(emptyList()) }
    var filteredFoodEntries by remember { mutableStateOf<List<FoodEntry2>>(emptyList()) }
    var filteredStepsHistory by remember { mutableStateOf<List<StepsEntry>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) } // Track search state
    var pendingDeleteItem by remember { mutableStateOf<Any?>(null) } // For tracking the item to delete
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Date formatter to use for displaying and filtering dates
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val inputDateFormats = listOf(
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()),
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
        SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    )

    val totalCalories = filteredFoodEntries.sumOf { it.caloricValue }
    val totalSteps = filteredStepsHistory.sumOf { it.steps }

    LaunchedEffect(Unit) {
        currentUser?.let {
            // Fetch Food Entries from Firestore
            FirebaseFirestore.getInstance().collection("foodEntries")
                .whereEqualTo("userId", it.uid)
                .orderBy("dateString", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("HistoryScreen", "Error fetching food entries: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshot?.let { snap ->
                        foodEntriesHistory = snap.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(FoodEntry2::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("HistoryScreen", "Error parsing food entry: ${e.message}")
                                null
                            }
                        }
                        filteredFoodEntries = foodEntriesHistory // Initially set filtered list as the entire list
                    }
                }

            // Fetch Steps History from Firestore
            FirebaseFirestore.getInstance().collection("steps")
                .whereEqualTo("userId", it.uid)
                .orderBy("dateString", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("HistoryScreen", "Error fetching steps entries: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshot?.let { snap ->
                        stepsHistory = snap.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(StepsEntry::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("HistoryScreen", "Error parsing steps entry: ${e.message}")
                                null
                            }
                        }
                        filteredStepsHistory = stepsHistory // Initially set filtered list as the entire list
                    }
                }
        }
    }

    // Function to filter the history entries strictly by date
    fun filterHistoryEntries(query: String) {
        if (query.isEmpty()) {
            filteredFoodEntries = foodEntriesHistory
            filteredStepsHistory = stepsHistory
        } else {
            val lowerCaseQuery = query.lowercase()

            // Filter food entries by formatted date string
            filteredFoodEntries = foodEntriesHistory.filter { entry ->
                inputDateFormats.any { format ->
                    val date = entry.timestamp?.toDate()
                    val formattedDate = date?.let { format.format(it).lowercase() }
                    formattedDate?.contains(lowerCaseQuery) == true
                }
            }

            // Filter steps entries by formatted date string
            filteredStepsHistory = stepsHistory.filter { entry ->
                inputDateFormats.any { format ->
                    val date = entry.timestamp?.toDate()
                    val formattedDate = date?.let { format.format(it).lowercase() }
                    formattedDate?.contains(lowerCaseQuery) == true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                modifier = Modifier.padding(horizontal = 24.dp),
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        content = { paddingValues ->
            // Inside your Scaffold's content lambda:
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Show the search bar when active
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            filterHistoryEntries(query) // Immediately filter as the user types
                        },
                        label = { Text("Search by Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        singleLine = true
                    )
                }

                if (foodEntriesHistory.isEmpty() && stepsHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredFoodEntries.isEmpty() && filteredStepsHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No history found.")
                    }
                } else {
                    // Calculate the totals from the filtered lists
                    val totalCaloriesfiltered = filteredFoodEntries.sumOf { it.caloricValue }
                    val totalStepsfiltered = filteredStepsHistory.sumOf { it.steps }

                    // Group food entries by date
                    val groupedFoodEntries = filteredFoodEntries.groupBy { entry ->
                        entry.timestamp?.toDate()?.let { dateFormatter.format(it) } ?: "No date"
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Show the totals at the top of the list
                        item {
                            TotalCard(totalCaloriesfiltered, totalStepsfiltered)
                        }

                        item {
                            Text("Food Entries History", style = MaterialTheme.typography.titleLarge)
                        }
                        if (foodEntriesHistory.isEmpty()) {
                            item {
                                Text("No food entries found.", style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            groupedFoodEntries.forEach { (date, entries) ->
                                item {
                                    Text("Date: $date", style = MaterialTheme.typography.bodyLarge)
                                }
                                val totalCaloriesForDate = entries.sumOf { it.caloricValue }
                                item {
                                    Text("Total Calories: $totalCaloriesForDate", style = MaterialTheme.typography.bodyMedium)
                                }
                                items(entries) { entry ->
                                    val dismissState = rememberDismissState(
                                        confirmStateChange = { dismissValue ->
                                            if (dismissValue == DismissValue.DismissedToStart) {
                                                pendingDeleteItem = entry
                                            }
                                            false // Don't auto-dismiss
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
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.onError
                                                )
                                            }
                                        },
                                        dismissContent = {
                                            FoodEntryHistoryCard(entry = entry, dateFormatter = dateFormatter)
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(1.dp))
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Steps History", style = MaterialTheme.typography.titleLarge)
                        }
                        if (filteredStepsHistory.isEmpty()) {
                            item {
                                Text("No steps data found.", style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            items(filteredStepsHistory) { entry ->
                                val dismissState = rememberDismissState(
                                    confirmStateChange = { dismissValue ->
                                        if (dismissValue == DismissValue.DismissedToStart) {
                                            pendingDeleteItem = entry
                                        }
                                        false // Don't auto-dismiss
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
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onError
                                            )
                                        }
                                    },
                                    dismissContent = {
                                        StepsHistoryCard(entry = entry, dateFormatter = dateFormatter)
                                    }
                                )
                            }
                        }
                    }
                }

                // Confirmation dialog for deletion remains unchanged...
            }

            // Confirmation dialog for deletion
            pendingDeleteItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { pendingDeleteItem = null },
                    title = { Text("Delete History Entry") },
                    text = { Text("Are you sure you want to delete this entry?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (item is FoodEntry2) {
                                    FirebaseFirestore.getInstance().collection("foodEntries")
                                        .document(item.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            foodEntriesHistory = foodEntriesHistory.filter { it.id != item.id }
                                            filteredFoodEntries = filteredFoodEntries.filter { it.id != item.id }
                                        }
                                } else if (item is StepsEntry) {
                                    FirebaseFirestore.getInstance().collection("steps")
                                        .document(item.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            stepsHistory = stepsHistory.filter { it.id != item.id }
                                            filteredStepsHistory = filteredStepsHistory.filter { it.id != item.id }
                                        }
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
    )
}

@Composable
fun FoodEntryHistoryCard(entry: FoodEntry2, dateFormatter: SimpleDateFormat) {
    val dateString = entry.timestamp?.toDate()?.let { dateFormatter.format(it) } ?: "No date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Secondary, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalDining,
                    contentDescription = "Calories Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${entry.foodName}", style = MaterialTheme.typography.titleMedium)

            }

            Text("Calories: ${entry.caloricValue}", style = MaterialTheme.typography.bodyMedium)
            Text("Date: $dateString", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StepsHistoryCard(entry: StepsEntry, dateFormatter: SimpleDateFormat) {
    val dateString = entry.timestamp?.toDate()?.let { dateFormatter.format(it) } ?: "No date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Secondary, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = "Steps Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${entry.steps}", style = MaterialTheme.typography.titleMedium)

            }

            Text("Date: $dateString", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TotalCard(totalCalories: Int, totalSteps: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Primary, contentColor = MaterialTheme.colorScheme.onPrimary),

        ) {
        Column(
            modifier = Modifier.padding(16.dp),
            //horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row for Total Calories
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalDining,
                    contentDescription = "Calories Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Total Calories: $totalCalories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Row for Total Steps
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = "Steps Icon",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Total Steps: $totalSteps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

}

// Data classes for Firestore documents
data class FoodEntry2(
    val id: String = "",
    val foodName: String = "",
    val caloricValue: Int = 0,
    val timestamp: Timestamp? = null,
    val userId: String = ""
)

data class StepsEntry(
    val id: String = "",
    val steps: Int = 0,
    val timestamp: Timestamp? = null,
    val userId: String = ""
)