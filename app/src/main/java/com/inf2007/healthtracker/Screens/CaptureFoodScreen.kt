package com.inf2007.healthtracker.Screens

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.inf2007.healthtracker.BuildConfig
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.ui.theme.Unfocused
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.utilities.GeminiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFoodScreen(navController: NavController) {
    var foodName by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedFood by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var caloricValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val roundedShape = MaterialTheme.shapes.small

    // Initialize GeminiService
    val geminiService = remember { GeminiService(BuildConfig.geminiApiKey) }

    // Function to handle food recognition
    fun recognizeFood(image: Bitmap?, foodName: String) {
        if (image == null && foodName.isBlank()) {
            errorMessage = "Please enter food name and photo"
            return
        }

        coroutineScope.launch {
            try {
                val result = geminiService.doFoodRecognition(image, foodName)
                Log.i("CaptureFoodScreen", "Food recognition result: $result")

                val responseText = result.firstOrNull() ?: ""
                val estimatedCaloricValue = responseText.filter { it.isDigit() }.toIntOrNull() ?: 0
                Log.i("CaptureFoodScreen", "Food recognition caloric value: $estimatedCaloricValue")

                recognizedFood = result.firstOrNull()?.let { Pair(it, estimatedCaloricValue) }
                caloricValue = estimatedCaloricValue.toString()
            } catch (e: Exception) {
                errorMessage = "Error recognizing food: ${e.message}"
            }
        }
    }

    // Function to save food data to Firestore
    fun saveFoodData(foodName: String, caloricValue: Int) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val todayString = dateFormat.format(Date())
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val foodData = hashMapOf(
                "foodName" to foodName,
                "caloricValue" to caloricValue,
                "timestamp" to Date(),
                "userId" to it.uid,
                "dateString" to todayString
            )
            FirebaseFirestore.getInstance().collection("foodEntries")
                .add(foodData)
                .addOnSuccessListener {
                    Log.d("CaptureFoodScreen", "Food data saved successfully")
                    navController.popBackStack()
                }
                .addOnFailureListener { e ->
                    Log.w(
                        "CaptureFoodScreen",
                        "Error saving food data",
                        e
                    )
                }
        }
    }

    // Camera launcher
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            imageBitmap = bitmap
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Capture Food") },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        },
        bottomBar = { BottomNavigationBar(navController) }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), // Control vertical spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clickable box to capture image
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.Gray)
                    .clickable { cameraLauncher.launch(null) },
                contentAlignment = Alignment.Center
            ) {
                imageBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Text(
                    "Tap to Capture Image\n(Not Compulsory)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Food Name Text Field
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = "Food Icon"
                    )
                },
                shape = roundedShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Unfocused
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Recognize food button
            Button(
                onClick = {
                    recognizeFood(imageBitmap, foodName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = Primary),
                border = BorderStroke(1.dp, Primary),
                shape = roundedShape,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Calculate Estimated Caloric Value of: $foodName")
            }

            // Display recognized food
            recognizedFood?.let {
                caloricValue = it.second.toString()
                Log.i("CaptureFoodScreen", "Recognized food: ${it}")
                Text(
                    "Recognized Food: Based on the image and typical ingredients of $foodName",
                    style = MaterialTheme.typography.bodyLarge
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Text
                    Text(
                        "Estimated Calories in kcal:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    // Caloric Value Text Field
                    OutlinedTextField(
                        value = caloricValue,
                        onValueChange = { caloricValue = it },
                        label = { Text("Caloric Value") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Caloric Value Icon"
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

            // Save food data button
            Button(
                onClick = { recognizedFood?.let { saveFoodData(foodName, it.second) } },
                enabled = foodName.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = roundedShape,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Food Data")
            }

            // Error message display
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
