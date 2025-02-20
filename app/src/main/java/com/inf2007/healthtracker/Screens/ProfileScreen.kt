package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("") }
    var dietaryPreference by remember { mutableStateOf("") }
    var calorieIntake by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    // Fetch user data from Firebase
    LaunchedEffect(Unit) {
        user?.let {
            FirebaseFirestore.getInstance().collection("users").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    userName = document.getString("name") ?: "Unknown User"
                    userEmail = document.getString("email") ?: "Unknown Email"
                    weight = document.get("weight")?.toString() ?: ""
                    height = document.get("height")?.toString() ?: ""
                    activityLevel = document.getString("activity_level") ?: "Sedentary"
                    dietaryPreference = document.getString("dietary_preference") ?: "None"
                    calorieIntake = document.get("calorie_intake")?.toString() ?: ""
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message ?: "Failed to retrieve profile data"
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = Color.Red)
                } else {
                    if (!isEditing) {
                        // ✅ View Mode
                        Text("Name: $userName", style = MaterialTheme.typography.bodyLarge)
                        Text("Email: $userEmail", style = MaterialTheme.typography.bodyLarge)
                        Text("Weight: $weight kg", style = MaterialTheme.typography.bodyLarge)
                        Text("Height: $height cm", style = MaterialTheme.typography.bodyLarge)
                        Text("Activity Level: $activityLevel", style = MaterialTheme.typography.bodyLarge)
                        Text("Dietary Preference: $dietaryPreference", style = MaterialTheme.typography.bodyLarge)
                        Text("Calorie Intake: $calorieIntake kcal", style = MaterialTheme.typography.bodyLarge)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isEditing = true } // ✅ Enable Edit Mode
                        ) {
                            Text("Edit Profile")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                            )
                        ) {
                            Text("Logout")
                        }
                    } else {
                        // ✅ Edit Mode
                        TextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Name") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Email") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (kg)") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height (cm)") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = activityLevel,
                            onValueChange = { activityLevel = it },
                            label = { Text("Activity Level (Sedentary, Moderate, Active)") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = dietaryPreference,
                            onValueChange = { dietaryPreference = it },
                            label = { Text("Dietary Preference (Vegan, Keto, etc.)") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = calorieIntake,
                            onValueChange = { calorieIntake = it },
                            label = { Text("Desired Calorie Intake (kcal)") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isLoading = true

                                val updates: MutableMap<String, Any> = mutableMapOf(
                                    "name" to userName,
                                    "email" to userEmail,
                                    "activity_level" to activityLevel,
                                    "dietary_preference" to dietaryPreference
                                )

                                // Ensure weight and height are stored as Integers only if valid
                                weight.toIntOrNull()?.let { updates["weight"] = it }
                                height.toIntOrNull()?.let { updates["height"] = it }
                                calorieIntake.toIntOrNull()?.let { updates["calorie_intake"] = it }

                                FirebaseFirestore.getInstance().collection("users")
                                    .document(user!!.uid)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                        isEditing = false // ✅ Exit Edit Mode
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = exception.message ?: "Update failed"
                                    }
                            }
                        ) {
                            Text("Save Changes")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { isEditing = false } // ✅ Cancel Editing
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}





