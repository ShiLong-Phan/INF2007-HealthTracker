//package com.inf2007.healthtracker.Screens
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.firestore.FirebaseFirestore
//import androidx.compose.ui.Alignment
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.TopAppBar
//import com.inf2007.healthtracker.utilities.*
//
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MainScreen(
//    navController: NavController,
//    modifier: Modifier = Modifier
//) {
//    var userName by remember { mutableStateOf<String?>("") }
//    var weight by remember { mutableStateOf(70) } // Default weight
//    var height by remember { mutableStateOf(170) } // Default height
//    var activityLevel by remember { mutableStateOf("Moderate") } // Default activity level
//    var dietaryPreference by remember { mutableStateOf("None") } // Default dietary preference
//    var calorieIntake by remember { mutableStateOf(2000) } // Default calorie intake
//    var isLoading by remember { mutableStateOf(true) }
//    var errorMessage by remember { mutableStateOf("") }
//    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
//
//    LaunchedEffect(Unit) {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        if (currentUser != null) {
//            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
//                .get()
//                .addOnSuccessListener { document ->
//                    if (document != null) {
//                        userName = document.getString("name") ?: "User" // Default to "User" if null
//                        weight = document.getLong("weight")?.toInt() ?: 70 // Default to 70 if null
//                        height = document.getLong("height")?.toInt() ?: 170 // Default to 170 if null
//                        activityLevel = document.getString("activity_level") ?: "Moderate" // Default to "Moderate" if null
//                        dietaryPreference = document.getString("dietary_preference") ?: "None" // Default to "None" if null
//                        calorieIntake = document.getLong("calorie_intake")?.toInt() ?: 2000 // Default to 2000 if null
//                    } else {
//                        errorMessage = "No user data found"
//                    }
//                    isLoading = false
//                }
//                .addOnFailureListener { exception ->
//                    errorMessage = exception.message ?: "Failed to retrieve user data"
//                    isLoading = false
//                }
//        } else {
//            errorMessage = "User not logged in"
//            isLoading = false
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(text = "Health Tracker") },
//                actions = {
//                    IconButton(onClick = { navController.navigate("profile_screen") }) {
//                        Text("Profile")
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(32.dp),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(text = "Welcome, ${userName}")
//
//            // Step Counter
//            StepCounter()
//
//            // Pass Retrieved User Data to Meal Recommendation Screen
//            MealRecButton(
//                navController = navController,
//                weight = weight,
//                height = height,
//                activityLevel = activityLevel,
//                dietaryPreference = dietaryPreference,
//                calorieIntake = calorieIntake
//            )
//
//            DashboardBut(navController = navController)
//
//            Button(
//                onClick = {
//                    FirebaseAuth.getInstance().signOut()
//                    navController.navigate("login_screen") {
//                        popUpTo("main_screen") { inclusive = true }
//                    }
//                },
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    contentColor = MaterialTheme.colorScheme.onPrimary
//                ),
//                modifier = Modifier.padding(top = 16.dp)
//            ) {
//                Text("Logout")
//            }
//        }
//    }
//}
//
//// Modified MealRecButton to Pass User Profile Data
//@Composable
//fun MealRecButton(
//    navController: NavController,
//    weight: Int,
//    height: Int,
//    activityLevel: String,
//    dietaryPreference: String,
//    calorieIntake: Int
//) {
//    Button(
//        onClick = {
//            navController.navigate(
//                "meal_recommendation_screen/$weight/$height/$activityLevel/$dietaryPreference/$calorieIntake"
//            )
//        },
//        colors = ButtonDefaults.buttonColors(
//            containerColor = MaterialTheme.colorScheme.primary,
//            contentColor = MaterialTheme.colorScheme.onPrimary
//        ),
//        modifier = Modifier.padding(top = 16.dp)
//    ) {
//        Text("Meal Recommendations")
//    }
//
//
//}
//
//@Composable
//fun DashboardBut(navController: NavController){
//    Button(
//        onClick = {
//            navController.navigate(
//                "dashboard_screen"
//            )
//        },
//        colors = ButtonDefaults.buttonColors(
//            containerColor = MaterialTheme.colorScheme.primary,
//            contentColor = MaterialTheme.colorScheme.onPrimary
//        ),
//        modifier = Modifier.padding(top = 16.dp)
//    ) {
//        Text("Dashboard")
//    }
//
//}
//
//
//

package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.utilities.StepCounter
import com.inf2007.healthtracker.utilities.getCurrentDate
import com.inf2007.healthtracker.utilities.syncStepsToFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf<String?>("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(68) }
    var height by remember { mutableStateOf(170) }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var dietaryPreference by remember { mutableStateOf("None") }
    var calorieIntake by remember { mutableStateOf(2000) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    //val coroutineScope = rememberCoroutineScope()
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var stepCount by remember { mutableStateOf(0) } // Step count from StepCounter
    val firestore = FirebaseFirestore.getInstance()
    val stepsRef = firestore.collection("steps").document("${user?.uid}_${getCurrentDate()}")


    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            FirebaseFirestore.getInstance().collection("users").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    userName = document.getString("name") ?: "User"
                    age = (document.getString("age") ?: 23).toString()
                    gender = document.getString("gender") ?: "Male"
                    weight = document.getLong("weight")?.toInt() ?: 70
                    height = document.getLong("height")?.toInt() ?: 170
                    activityLevel = document.getString("activity_level") ?: "Moderate"
                    dietaryPreference = document.getString("dietary_preference") ?: "None"
                    calorieIntake = document.getLong("calorie_intake")?.toInt() ?: 2000
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message ?: "Failed to retrieve user data"
                }
                .addOnCompleteListener {
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Health Tracker") },
                actions = {
                    Text(
                        text = "Profile",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { navController.navigate("profile_screen") },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome, $userName", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            HorizontalDivider()

            StepCounter(user!!) { newStepCount ->
                stepCount = newStepCount
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // New Sync Button
                SyncNowBtn(user!!, stepCount, stepsRef)

                // Pass Retrieved User Data to Meal Recommendation Screen
                MealRecBtn(
                    navController = navController,
                    age = age,
                    gender = gender,
                    weight = weight,
                    height = height,
                    activityLevel = activityLevel,
                    dietaryPreference = dietaryPreference,
                    calorieIntake = calorieIntake
                )
                ViewMealHistoryBtn(navController)
                DashboardBtn(navController = navController)
                CaptureFoodBtn(navController = navController)

                ActionButton("Logout") {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                }
            }
        }
    }
}

// Modified MealRecButton to Pass User Profile Data
@Composable
fun MealRecBtn(
    navController: NavController,
    age: String,
    gender: String,
    weight: Int,
    height: Int,
    activityLevel: String,
    dietaryPreference: String,
    calorieIntake: Int
) {
    Button(
        onClick = {
            navController.navigate(
                "meal_recommendation_screen/$age/$gender/$weight/$height/$activityLevel/$dietaryPreference/$calorieIntake"
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text("Meal Recommendations")
    }


}

// New Sync Button (Styled like other buttons)
@Composable
fun SyncNowBtn(user: FirebaseUser, stepCount: Int, stepsRef: DocumentReference) {
    Button(
        onClick = { syncStepsToFirestore(user, stepCount.toLong(), stepsRef) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text("Sync Now")
    }
}

@Composable
fun DashboardBtn(navController: NavController) {
    Button(
        onClick = {
            navController.navigate(
                "dashboard_screen"
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text("Dashboard")
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
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text("Save Food Data")
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
fun ViewMealHistoryBtn(navController: NavController) {
    Button(
        onClick = {
            navController.navigate("meal_history_screen")
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text("View Meal History")
    }
}

