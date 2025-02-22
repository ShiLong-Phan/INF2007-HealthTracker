package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.math.min
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var steps by remember { mutableStateOf(0) }

    /**
     * Now we derive `calorieIntake` from the sum of all food entries,
     * so we'll remove the line setting `calorieIntake` from `users` doc.
     */
    var calorieIntake by remember { mutableStateOf(0) }
    var desiredCalorieIntake by remember { mutableStateOf(0) }

    var hydration by remember { mutableStateOf(0) }
    var weight by remember { mutableStateOf(0) }
    var healthTips by remember { mutableStateOf("Fetching AI health tips...") }

    // Example data for new functionalities (like weekly steps)
    var weeklySteps by remember { mutableStateOf(listOf(1000, 1000, 1000, 1000, 1000, 1000, 1000)) }

    // 1) Change daily calorie goal to 1746
    val dailyStepGoal = 10000
    val dailyCalorieGoal = desiredCalorieIntake
    val dailyHydrationGoal = 3200

    // List of food entries for the current user
    var foodEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch user data from Firestore
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            // Fetch hydration, weight, and desiredCalorieIntake from `users`:
            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    hydration = document.getLong("hydration")?.toInt() ?: 0
                    weight = document.getLong("weight")?.toInt() ?: 0
                    desiredCalorieIntake = document.getLong("calorie_intake")?.toInt() ?: 0
                }

            // Fetch steps from the `steps` collection:
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val stepsDocId = "${user.uid}_$dateStr"
            FirebaseFirestore.getInstance().collection("steps")
                .document(stepsDocId)
                .get()
                .addOnSuccessListener { doc ->
                    steps = doc.getLong("steps")?.toInt() ?: 0
                }

            // Listen for changes to the `foodEntries` collection:
            FirebaseFirestore.getInstance().collection("foodEntries")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("date", dateStr)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FoodEntry::class.java)?.copy(id = doc.id)
                        }
                        foodEntries = items
                        calorieIntake = items.sumOf { it.caloricValue }
                    } else {
                        foodEntries = emptyList()
                        calorieIntake = 0
                    }
                }
        }
    }

    // Fetch AI health tips
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            healthTips = fetchAIHealthTips(steps, calorieIntake, hydration, weight)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dashboard", textAlign = TextAlign.Center) })
        }
    ) { paddingValues ->
        // Make content scrollable
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your Daily Summary",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Divider()

            HealthStatCard("Steps Taken", "$steps")
            // 3) Show total calorie intake from food entries
            HealthStatCard("Calorie Intake", "$calorieIntake kcal")
            HealthStatCard("Water Intake", "$hydration ml")
            HealthStatCard("Current Weight", "$weight kg")

            // Daily Goals vs. Actual
            DailyGoalProgress("Steps", steps, dailyStepGoal, "steps")
            DailyGoalProgress("Calories", calorieIntake, dailyCalorieGoal, "kcal")
            DailyGoalProgress("Hydration", hydration, dailyHydrationGoal, "ml")

            Divider()

            // Simple Weekly Steps Chart
            Text(
                text = "Weekly Steps",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            WeeklyStepsChart(weeklySteps)

            Divider()

            // Food Eaten Section
            Text("Food Eaten", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (foodEntries.isEmpty()) {
                Text("No entries yet!", style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    foodEntries.forEach { entry ->
                        FoodEntryCard(entry)
                    }
                }
            }

            Divider()

            // Quick water intake logging
            QuickWaterLogging(
                onLogWater = { amount ->
                    coroutineScope.launch {
                        currentUser?.let { user ->
                            hydration += amount
                            FirebaseFirestore.getInstance().collection("users")
                                .document(user.uid)
                                .update("hydration", hydration)
                        }
                    }
                },
                onResetWater = {
                    coroutineScope.launch {
                        currentUser?.let { user ->
                            hydration = 0
                            FirebaseFirestore.getInstance().collection("users")
                                .document(user.uid)
                                .update("hydration", hydration)
                        }
                    }
                }
            )

            Divider()

            Text("AI Health Tips", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = healthTips,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Data class for a food entry document in Firestore.
 */
data class FoodEntry(
    val id: String = "",
    val foodName: String = "",
    val caloricValue: Int = 0,
    val timestamp: Timestamp? = null,
    val userId: String = ""
)

/**
 * Composable to display a single FoodEntry.
 */
@Composable
fun FoodEntryCard(entry: FoodEntry) {
    val sdf = SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault())
    val dateString = entry.timestamp?.toDate()?.let { sdf.format(it) } ?: "No date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = entry.foodName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Calories: ${entry.caloricValue}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Date: $dateString", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Shows daily goal progress for a stat (e.g., steps, calories, or hydration).
 */
@Composable
fun DailyGoalProgress(statLabel: String, currentValue: Int, goalValue: Int, unit: String) {
    val progressFraction = min(currentValue.toFloat() / goalValue, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$statLabel: $currentValue / $goalValue $unit", style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(
            progress = progressFraction,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 4.dp)
        )
    }
}

/**
 * A very basic placeholder 'chart' for weekly steps.
 * Replace this with a real chart library if you want a more detailed graph.
 */
@Composable
fun WeeklyStepsChart(weeklyData: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weeklyData.forEach { daySteps ->
            val fraction = min(daySteps / 10000f, 1f)
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height((100 * fraction).dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Buttons for quick water intake logging: +250 ml, +500 ml, +1000 ml, and Reset.
 */
@Composable
fun QuickWaterLogging(
    onLogWater: (Int) -> Unit,
    onResetWater: () -> Unit
) {
    Text(
        text = "Log Extra Water",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { onLogWater(250) }) {
            Text("+250 ml")
        }
        Button(onClick = { onLogWater(500) }) {
            Text("+500 ml")
        }
        Button(onClick = { onLogWater(1000) }) {
            Text("+1000 ml")
        }

    }
    Button(onClick = onResetWater) {
        Text("Reset for testing purposes")
    }
}

/**
 * Card to display a title and value (unchanged from original).
 */
@Composable
fun HealthStatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Simulated AI-generated health advice function (unchanged).
 */
suspend fun fetchAIHealthTips(steps: Int, calorieIntake: Int, hydration: Int, weight: Int): String {
    return "Based on your activity level and nutrition, consider increasing your water intake by 500ml " +
            "to stay optimally hydrated. Aim for 10,000 steps daily for better cardiovascular health."
}
