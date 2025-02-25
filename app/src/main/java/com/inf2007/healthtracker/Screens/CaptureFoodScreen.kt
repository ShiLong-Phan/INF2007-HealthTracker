package com.inf2007.healthtracker.Screens

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
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
import com.inf2007.healthtracker.utilities.BottomNavigationBar
import com.inf2007.healthtracker.utilities.GeminiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFoodScreen(navController: NavController) {
    var foodName by mutableStateOf("")
    var imageBitmap by mutableStateOf<Bitmap?>(null)
    var recognizedFood by mutableStateOf<Pair<String, Int>?>(null)
    var caloricValue by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
                title = { Text(text = "Capture Food") }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
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

            TextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name*") }
            )

            // Recognize food button
            Button(
                onClick = {
                    recognizeFood(imageBitmap, foodName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
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
                Text(
                    "Estimated Calories in kcal:"
                )
                TextField(
                    value = caloricValue,
                    onValueChange = { caloricValue = it },
                    label = { Text("Caloric Value") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Save food data button
            Button(
                onClick = { recognizedFood?.let { saveFoodData(foodName, it.second) } },
                enabled = foodName.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
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