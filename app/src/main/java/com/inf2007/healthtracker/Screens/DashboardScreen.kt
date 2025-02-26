package com.inf2007.healthtracker.Screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.utilities.StepCounter
import com.inf2007.healthtracker.utilities.syncStepsToFirestore
import kotlinx.coroutines.launch
import kotlin.math.min
import java.text.SimpleDateFormat
import java.util.*
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.ui.theme.Secondary
import com.inf2007.healthtracker.ui.theme.SecondaryContainer
import com.inf2007.healthtracker.ui.theme.Tertiary
import com.inf2007.healthtracker.ui.theme.Unfocused
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var steps by remember { mutableStateOf(0) }
    var calorieIntake by remember { mutableStateOf(0) }
    var desiredCalorieIntake by remember { mutableStateOf(0) }
    var hydration by remember { mutableStateOf(0) }
    var weight by remember { mutableStateOf(0) }
    var healthTips by remember { mutableStateOf("Fetching AI health tips...") }
    var weeklySteps by remember { mutableStateOf(listOf(1000, 1000, 1000, 1000, 1000, 1000, 1000)) }

    val dailyStepGoal = 10000
    val dailyCalorieGoal = desiredCalorieIntake
    val dailyHydrationGoal = 3200

    var foodEntries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    //moving of stepcounter stuff
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var stepCount by remember { mutableStateOf(0) } // Step count from StepCounter
    val firestore = FirebaseFirestore.getInstance()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date())
    val stepsRef = firestore.collection("steps").document("${user?.uid}_${formattedDate}")

    // Fetch user data from Firestore
    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            val calendar = Calendar.getInstance() // Uses TimeZone.getDefault()
            calendar.time = Date()

            // Set to the start of today (UTC+8):
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = com.google.firebase.Timestamp(calendar.time)

            // Set to the start of tomorrow (UTC+8):
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = com.google.firebase.Timestamp(calendar.time)

            android.util.Log.d("CalendarDebug", "Start of Day: ${calendar.time} | End of Day: $endOfDay")

            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val todayString = dateFormat.format(Date())

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
//            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//            val stepsDocId = "${user.uid}_$dateStr"
//            FirebaseFirestore.getInstance().collection("steps")
//                .document(stepsDocId)
//                .get()
//                .addOnSuccessListener { doc ->
//                    steps = doc.getLong("steps")?.toInt() ?: 0
//                }

            //Code for steps data only for the current day
            FirebaseFirestore.getInstance().collection("steps")
                .whereEqualTo("userId", user.uid) // ensure your steps documents include a "userId" field
                .whereEqualTo("dateString", todayString)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        steps = snapshot.documents.sumOf { doc ->
                            doc.getLong("steps")?.toInt() ?: 0
                        }
                    } else {
                        steps = 0
                    }
                }

            // Listen for changes to the `foodEntries` collection for the current day:
            FirebaseFirestore.getInstance().collection("foodEntries")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("dateString", todayString)
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
            TopAppBar(
                title = { Text("Dashboard", modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier.padding(horizontal = 24.dp),
                actions = {
                    IconButton(onClick = { navController.navigate("history_screen") }) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "History"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
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
            // Header
            Text(
                "Your Daily Summary",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = Primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Step Counter
                    StepCounter(user!!) { newStepCount ->
                        stepCount = newStepCount
                    }
                    // Calorie Intake
                    HealthStatCard("Calorie Intake", "$calorieIntake kcal")
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Water Intake
                    HealthStatCard("Water Intake", "$hydration ml")
                    // Current Weight
                    HealthStatCard("Current Weight", "$weight kg")
                }
            }

            DailyGoalProgress("Steps", steps, dailyStepGoal, "steps")
            DailyGoalProgress("Calories", calorieIntake, dailyCalorieGoal, "kcal")
            DailyGoalProgress("Hydration", hydration, dailyHydrationGoal, "ml")

            // Log Extra Water
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

            Spacer(modifier = Modifier.height(2.dp))

            // Sync Now Button
            SyncNowBtn(user!!, stepCount, stepsRef)

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Steps
            Text(
                text = "Weekly Steps",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            WeeklyStepsChart(weeklySteps)

            Spacer(modifier = Modifier.height(16.dp))

            // Food Eaten
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

            CaptureFoodBtn(navController = navController)

            Spacer(modifier = Modifier.height(16.dp))

            // AI Health Tips
            Text("AI Health Tips", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background, contentColor = Color.Black),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = healthTips,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "$statLabel: $currentValue / $goalValue $unit",
            style = MaterialTheme.typography.bodyLarge,  // Using bodyLarge for Normal font weight
            modifier = Modifier.fillMaxWidth()
        )
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = Secondary,  // The actual progress line
            trackColor = SecondaryContainer
        )
    }
}

/**
 * A very basic placeholder 'chart' for weekly steps.
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
                    .background(Secondary)
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Log Extra Water",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onResetWater,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Tertiary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = "Reset",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val interactionSource = remember { MutableInteractionSource() }

            OutlinedButton(
                onClick = { onLogWater(250) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Unfocused
                ),
                border = BorderStroke(1.dp, Unfocused),
                interactionSource = interactionSource
            ) {
                val isPressed = interactionSource.collectIsPressedAsState()
                Text(
                    "+250 ml",
                    color = if (isPressed.value) Primary else Unfocused
                )
            }

            OutlinedButton(
                onClick = { onLogWater(500) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Unfocused
                ),
                border = BorderStroke(1.dp, Unfocused),
                interactionSource = interactionSource
            ) {
                val isPressed = interactionSource.collectIsPressedAsState()
                Text(
                    "+500 ml",
                    color = if (isPressed.value) Primary else Unfocused
                )
            }

            OutlinedButton(
                onClick = { onLogWater(1000) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Unfocused
                ),
                border = BorderStroke(1.dp, Unfocused),
                interactionSource = interactionSource
            ) {
                val isPressed = interactionSource.collectIsPressedAsState()
                Text(
                    "+1000 ml",
                    color = if (isPressed.value) Primary else Unfocused
                )
            }
        }
    }
}

/**
 * Card to display a title and value.
 */
@Composable
fun HealthStatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Secondary),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, color = SecondaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, color = Color.White)
        }
    }
}

// New Sync Button (Styled like other buttons)
@Composable
fun SyncNowBtn(user: FirebaseUser, stepCount: Int, stepsRef: DocumentReference) {
    Button(
        onClick = { syncStepsToFirestore(user, stepCount.toLong(), stepsRef) },
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp)
    ) {
        Text("Sync Now")
    }
}

@Composable
fun CaptureFoodBtn(navController: NavController) {
    Button(
        onClick = {
            navController.navigate(
                "capture_food_screen"
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp)
    ) {
        Text("Add Food Data")
    }
}

/**
 * Simulated AI-generated health advice function.
 */
suspend fun fetchAIHealthTips(steps: Int, calorieIntake: Int, hydration: Int, weight: Int): String {
    return "Based on your activity level and nutrition, consider increasing your water intake by 500ml " +
            "to stay optimally hydrated. Aim for 10,000 steps daily for better cardiovascular health."
}