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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
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
    var stepsGoal by remember { mutableStateOf("") }
    var hydrationGoal by remember { mutableStateOf("") }
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
                    stepsGoal = document.get("steps_goal")?.toString() ?: ""
                    hydrationGoal = document.get("hydration_goal")?.toString() ?: ""
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message ?: "Failed to retrieve profile data"
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }, modifier = Modifier.padding(horizontal = 24.dp)) },
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
                    .padding(horizontal = 24.dp, vertical = 8.dp),
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
                            // Texts on the left
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

                            // Edit icon on the right
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
                            // User Details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Gender
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

                                // Weight
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

                                // Height
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

                            //Steps goal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = "Steps Goal Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Steps Goal",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.weight(1f))  // Pushes the value to the right

                                Text(
                                    text = "$stepsGoal steps",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            //Hydration Goal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = "Hydration Icon",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Hydration Goal",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.weight(1f))  // Pushes the value to the right

                                Text(
                                    text = "$hydrationGoal ml",
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
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Name Text Field
                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("Name") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Name Icon"
                                    )
                                },
                                shape = roundedShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Unfocused
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Email Text Field
                            OutlinedTextField(
                                value = userEmail,
                                onValueChange = { userEmail = it },
                                label = { Text("Email") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email Icon"
                                    )
                                },
                                shape = roundedShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Unfocused
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Age Text Field
                            OutlinedTextField(
                                value = userAge,
                                onValueChange = { userAge = it },
                                label = { Text("Age") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "Age Icon"
                                    )
                                },
                                shape = roundedShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Unfocused
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Dietary Preference and Calorie Intake Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f) // Makes this column take up half of the row's width
                                ) {
                                    // Dietary Preference Text Field
                                    OutlinedTextField(
                                        value = dietaryPreference,
                                        onValueChange = { dietaryPreference = it },
                                        label = { Text("Dietary Preference") }, // Label without the guide text
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Restaurant,
                                                contentDescription = "Dietary Preference Icon"
                                            )
                                        },
                                        shape = roundedShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = Unfocused
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Guide Text
                                    Text(
                                        text = "Vegan, Keto, etc.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Calorie Intake Text Field
                                    OutlinedTextField(
                                        value = calorieIntake,
                                        onValueChange = { calorieIntake = it },
                                        label = { Text("Calorie Intake (kcal)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = "Calorie Intake Icon"
                                            )
                                        },
                                        shape = roundedShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = Unfocused
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            //Steps and Hydration Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f) // Makes this column take up half of the row's width
                                ) {

                                    OutlinedTextField(
                                        value = stepsGoal,
                                        onValueChange = { stepsGoal = it },
                                        label = { Text("Steps") }, // Label without the guide text
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                                contentDescription = "Steps Goal Icon"
                                            )
                                        },
                                        shape = roundedShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = Unfocused
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )


                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = hydrationGoal,
                                        onValueChange = { hydrationGoal = it },
                                        label = { Text("Hydration") }, // Label without the guide text
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.LocalDrink,
                                                contentDescription = "Hydration Icon"
                                            )
                                        },
                                        shape = roundedShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = Unfocused
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                }
                            }

                            // Weight and Height Row
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ){
                                // Weight Text Field
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text("Weight (kg)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Scale,
                                            contentDescription = "Age Icon"
                                        )
                                    },
                                    shape = roundedShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Unfocused
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                // Height Text Field
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    label = { Text("Height (cm)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Height,
                                            contentDescription = "Age Icon"
                                        )
                                    },
                                    shape = roundedShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Unfocused
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Gender Radio Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                                    Text("Gender", style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RadioButton(
                                        selected = userGender == "Male",
                                        onClick = { userGender = "Male" }
                                    )
                                    Text("Male")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RadioButton(
                                        selected = userGender == "Female",
                                        onClick = { userGender = "Female" }
                                    )
                                    Text("Female")
                                }
                            }

                            // Activity Level Radio Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(0.dp)
                            ) {
                                Text("Activity Level", style = MaterialTheme.typography.bodyLarge)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    // Sedentary Button
                                    OutlinedButton(
                                        onClick = { activityLevel = "Sedentary" },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            contentColor = if (activityLevel == "Sedentary") Primary else Unfocused
                                        ),
                                        border = BorderStroke(1.dp, if (activityLevel == "Sedentary") Primary else Unfocused),
                                        modifier = Modifier.padding(end = 10.dp)
                                    ) {
                                        Text("Sedentary")
                                    }

                                    // Moderate Button
                                    OutlinedButton(
                                        onClick = { activityLevel = "Moderate" },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            contentColor = if (activityLevel == "Moderate") Primary else Unfocused
                                        ),
                                        border = BorderStroke(1.dp, if (activityLevel == "Moderate") Primary else Unfocused),
                                        modifier = Modifier.padding(end = 10.dp)
                                    ) {
                                        Text("Moderate")
                                    }

                                    // Active Button
                                    OutlinedButton(
                                        onClick = { activityLevel = "Active" },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            contentColor = if (activityLevel == "Active") Primary else Unfocused
                                        ),
                                        border = BorderStroke(1.dp, if (activityLevel == "Active") Primary else Unfocused)
                                    ) {
                                        Text("Active")
                                    }
                                }
                            }

                            // Calculate Recommended Intake Button
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
                                },
                                shape = roundedShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("Calculate Recommended Intake")
                            }

                            // CTA Buttons Row
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ){
                                // Cancel Changes Button
                                Button(
                                    onClick = { isEditing = false },
                                    shape = roundedShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = Primary, ),
                                    border = BorderStroke(1.dp, Primary),
                                    modifier = Modifier.height(56.dp).weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                // Save Changes Button
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
                                            "dietary_preference" to dietaryPreference,
                                            "steps_goal" to stepsGoal,
                                            "hydration_goal" to hydrationGoal
                                        )

                                        // Ensure weight and height are stored as Integers only if valid
                                        weight.toIntOrNull()?.let { updates["weight"] = it }
                                        height.toIntOrNull()?.let { updates["height"] = it }
                                        calorieIntake.toIntOrNull()?.let { updates["calorie_intake"] = it }
                                        stepsGoal.toIntOrNull()?.let { updates["steps_goal"] = it }
                                        hydrationGoal.toIntOrNull()?.let { updates["hydration_goal"] = it }

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
                                    },
                                    shape = roundedShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    modifier = Modifier.height(56.dp).weight(1f)
                                ) {
                                    Text("Save Changes")
                                }
                            }
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



