package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import com.inf2007.healthtracker.utilities.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf<String?>("") }
    var weight by remember { mutableStateOf(70) } // Default weight
    var height by remember { mutableStateOf(170) } // Default height
    var activityLevel by remember { mutableStateOf("Moderate") } // Default activity level
    var dietaryPreference by remember { mutableStateOf("None") } // Default dietary preference
    var calorieIntake by remember { mutableStateOf(2000) } // Default calorie intake
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userName = document.getString("name") ?: "User" // Default to "User" if null
                        weight = document.getLong("weight")?.toInt() ?: 70 // Default to 70 if null
                        height = document.getLong("height")?.toInt() ?: 170 // Default to 170 if null
                        activityLevel = document.getString("activity_level") ?: "Moderate" // Default to "Moderate" if null
                        dietaryPreference = document.getString("dietary_preference") ?: "None" // Default to "None" if null
                        calorieIntake = document.getLong("calorie_intake")?.toInt() ?: 2000 // Default to 2000 if null
                    } else {
                        errorMessage = "No user data found"
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message ?: "Failed to retrieve user data"
                    isLoading = false
                }
        } else {
            errorMessage = "User not logged in"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Health Tracker") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile_screen") }) {
                        Text("Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome, ${userName}")

            // Step Counter
            StepCounter()

            // Pass Retrieved User Data to Meal Recommendation Screen
            MealRecButton(
                navController = navController,
                weight = weight,
                height = height,
                activityLevel = activityLevel,
                dietaryPreference = dietaryPreference,
                calorieIntake = calorieIntake
            )

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Logout")
            }
        }
    }
}

// Modified MealRecButton to Pass User Profile Data
@Composable
fun MealRecButton(
    navController: NavController,
    weight: Int,
    height: Int,
    activityLevel: String,
    dietaryPreference: String,
    calorieIntake: Int
) {
    Button(
        onClick = {
            navController.navigate(
                "meal_recommendation_screen/$weight/$height/$activityLevel/$dietaryPreference/$calorieIntake"
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text("Meal Recommendations")
    }
}
