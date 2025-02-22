package com.inf2007.healthtracker.Screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var foodEntries by remember { mutableStateOf<List<FoodEntry2>>(emptyList()) }
    var stepsHistory by remember { mutableStateOf<List<StepsEntry>>(emptyList()) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("History") })
        },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        content = { paddingValues ->
            if (currentUser == null) {
                // Display a message if no user is logged in
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text("User not logged in.", style = MaterialTheme.typography.bodyLarge)
                }
                return@Scaffold
            }

            // Listen to Firestore snapshots safely
            LaunchedEffect(Unit) {
                // Food Entries History
                FirebaseFirestore.getInstance().collection("foodEntries")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("HistoryScreen", "Error fetching food entries: ${error.message}")
                            return@addSnapshotListener
                        }
                        snapshot?.let { snap ->
                            foodEntries = snap.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(FoodEntry2::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    Log.e("HistoryScreen", "Error parsing food entry: ${e.message}")
                                    null
                                }
                            }
                        }
                    }

                // Steps History
                FirebaseFirestore.getInstance().collection("steps")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
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
                                    Log.e(
                                        "HistoryScreen",
                                        "Error parsing steps entry: ${e.message}"
                                    )
                                    null
                                }
                            }
                        }
                    }
            }

            // UI: Display the history in a LazyColumn for smoother scrolling
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Food Entries History", style = MaterialTheme.typography.headlineSmall)
                }
                if (foodEntries.isEmpty()) {
                    item {
                        Text("No food entries found.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    items(foodEntries) { entry ->
                        FoodEntryHistoryCard(entry = entry)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Steps History", style = MaterialTheme.typography.headlineSmall)
                }
                if (stepsHistory.isEmpty()) {
                    item {
                        Text("No steps data found.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    items(stepsHistory) { entry ->
                        StepsHistoryCard(entry = entry)
                    }
                }
            }
        }
    )
}

@Composable
fun FoodEntryHistoryCard(entry: FoodEntry2) {
    val sdf = SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault())
    val dateString = entry.timestamp?.toDate()?.let { sdf.format(it) } ?: "No date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Food: ${entry.foodName}", style = MaterialTheme.typography.titleMedium)
            Text("Calories: ${entry.caloricValue}", style = MaterialTheme.typography.bodyMedium)
            Text("Date: $dateString", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StepsHistoryCard(entry: StepsEntry) {
    val sdf = SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault())
    val dateString = entry.timestamp?.toDate()?.let { sdf.format(it) } ?: "No date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Steps: ${entry.steps}", style = MaterialTheme.typography.titleMedium)
            Text("Date: $dateString", style = MaterialTheme.typography.bodySmall)
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
