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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.ui.theme.Secondary
import com.inf2007.healthtracker.ui.theme.SecondaryContainer
import com.inf2007.healthtracker.ui.theme.Tertiary
import com.inf2007.healthtracker.ui.theme.Unfocused
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userGender by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Sedentary") }
    var dietaryPreference by remember { mutableStateOf("") }
    var calorieIntake by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    val roundedShape = MaterialTheme.shapes.small

    // Fetch user data from Firebase
    LaunchedEffect(Unit) {
        user?.let {
            FirebaseFirestore.getInstance().collection("users").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    userName = document.getString("name") ?: "Unknown User"
                    userEmail = document.getString("email") ?: "Unknown Email"
                    userGender = document.getString("gender") ?: ""
                    userAge = document.getString("age") ?: ""
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
        topBar = { TopAppBar(title = { Text("Profile") }) },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    if (!isEditing) {
                        // View Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Texts to the left
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$userName",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                                Text(
                                    text = "$userEmail",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal)
                                )
                            }

                            // Edit icon to the right
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = roundedShape,
                            colors = CardDefaults.cardColors(containerColor = Secondary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = MaterialTheme.typography.titleSmall.toSpanStyle()
                                                .copy(color = MaterialTheme.colorScheme.secondaryContainer)
                                        ) {
                                            append("Gender\n")
                                        }
                                        withStyle(
                                            style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                                                .copy(color = Color.White)
                                        ) {
                                            append("$userGender")
                                        }
                                    },
                                    textAlign = TextAlign.Center
                                )

                                VerticalDivider(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(1.dp),
                                    color = Color.White
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = MaterialTheme.typography.titleSmall.toSpanStyle()
                                                .copy(color = MaterialTheme.colorScheme.secondaryContainer)
                                        ) {
                                            append("Weight\n")
                                        }
                                        withStyle(
                                            style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                                                .copy(color = Color.White)
                                        ) {
                                            append("$weight kg")
                                        }
                                    },
                                    textAlign = TextAlign.Center
                                )

                                VerticalDivider(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(1.dp),
                                    color = Color.White
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = MaterialTheme.typography.titleSmall.toSpanStyle()
                                                .copy(color = MaterialTheme.colorScheme.secondaryContainer)
                                        ) {
                                            append("Height\n")
                                        }
                                        withStyle(
                                            style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                                                .copy(color = Color.White)
                                        ) {
                                            append("$height cm")
                                        }
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Activity Level Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = "Activity Level Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Activity Level",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.weight(1f))  // Pushes the value to the right

                                Text(
                                    text = "$activityLevel",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Dietary Preference Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "Dietary Preference",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Dietary Preference",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.weight(1f))  // Pushes the value to the right

                                Text(
                                    text = "$dietaryPreference",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Calorie Intake Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = "Calorie Intake Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Calorie Intake",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.weight(1f))  // Pushes the value to the right

                                Text(
                                    text = "$calorieIntake kcal",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Logout Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        FirebaseAuth.getInstance().signOut()
                                        navController.navigate("login_screen") {
                                            popUpTo("main_screen") { inclusive = true }
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Logout",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Red
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Logout",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Red
                                )
                            }
                        }

                    } else {
                        // Edit Mode
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
                            value = userAge,
                            onValueChange = { userAge = it },
                            label = { Text("Age") }
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

                        // Gender Radio Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, Color.Black)
                                .padding(8.dp)
                        ) {
                            Text("Gender:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = userGender == "Male",
                                    onClick = { userGender = "Male" }
                                )
                                Text("Male")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = userGender == "Female",
                                    onClick = { userGender = "Female" }
                                )
                                Text("Female")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Spacer(modifier = Modifier.height(8.dp))

                        // Activity Level Radio Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(0.5.dp, Color.Black)
                                .padding(16.dp)
                        ) {
                            Text("Activity Level")
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = activityLevel == "Sedentary",
                                        onClick = { activityLevel = "Sedentary" }
                                    )
                                    Text("Sedentary")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = activityLevel == "Moderate",
                                        onClick = { activityLevel = "Moderate" }
                                    )
                                    Text("Moderate")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = activityLevel == "Active",
                                        onClick = { activityLevel = "Active" }
                                    )
                                    Text("Active")
                                }
                            }
                        }
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
                                val weightValue = weight.toDoubleOrNull() ?: 0.0
                                val heightValue = height.toDoubleOrNull() ?: 0.0
                                val ageValue = userAge.toIntOrNull() ?: 0
                                val bmr =
                                    calculateBMR(weightValue, heightValue, ageValue, userGender)
                                calorieIntake = bmr.toInt().toString()
                                Toast.makeText(
                                    context,
                                    "Recommended Calorie Intake: $calorieIntake kcal",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text("Calculate Recommended Intake")
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isLoading = true

                                val updates: MutableMap<String, Any> = mutableMapOf(
                                    "name" to userName,
                                    "age" to userAge,
                                    "gender" to userGender,
                                    "email" to userEmail,
                                    "gender" to userGender,
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
                                        Toast.makeText(
                                            context,
                                            "Profile updated!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isEditing = false // Exit Edit Mode
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
                            onClick = { isEditing = false } // Cancel Editing
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

// Function to calculate BMR
fun calculateBMR(weight: Double, height: Double, age: Int, gender: String): Double {
    return if (gender == "Male") {
        13.397 * weight + 4.799 * height - 5.677 * age + 88.362
    } else {
        9.247 * weight + 3.098 * height - 4.330 * age + 447.593
    }
}



