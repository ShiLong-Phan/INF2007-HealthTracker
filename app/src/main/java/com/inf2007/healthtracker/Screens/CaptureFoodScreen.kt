package com.inf2007.healthtracker.Screens

import android.graphics.Bitmap
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var confirmSave by remember { mutableStateOf(false) }
    var autoIdentifyFood by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val roundedShape = RoundedCornerShape(12.dp)
    val cardShape = RoundedCornerShape(16.dp)

    // Initialize GeminiService
    val geminiService = remember { GeminiService(BuildConfig.geminiApiKey) }

    // Function to identify food from image
    fun identifyFoodFromImage(image: Bitmap?) {
        if (image == null) {
            errorMessage = "Please take a photo to identify food"
            return
        }

        errorMessage = ""
        isProcessing = true

        coroutineScope.launch {
            try {
                // First identify the food from the image
                val identificationResult = geminiService.identifyFood(image)
                Log.i("CaptureFoodScreen", "Food identification result: $identificationResult")

                val identifiedFoodName = identificationResult.firstOrNull() ?: ""
                if (identifiedFoodName.isNotBlank() && !identifiedFoodName.startsWith("Error")) {
                    // Update the food name field
                    foodName = identifiedFoodName

                    // Show a confirmation message
                    Toast.makeText(
                        context,
                        "Identified as: $identifiedFoodName",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Now get calories with the identified food
                    val result = geminiService.doFoodRecognition(image, foodName)
                    Log.i("CaptureFoodScreen", "Food recognition result: $result")

                    val responseText = result.firstOrNull() ?: ""
                    val estimatedCaloricValue = responseText.filter { it.isDigit() }.toIntOrNull() ?: 0
                    Log.i("CaptureFoodScreen", "Food recognition caloric value: $estimatedCaloricValue")

                    recognizedFood = result.firstOrNull()?.let { Pair(it, estimatedCaloricValue) }
                    caloricValue = estimatedCaloricValue.toString()
                } else {
                    errorMessage = "Could not identify food from image. Please enter food name manually."
                }
                isProcessing = false
            } catch (e: Exception) {
                errorMessage = "Error identifying food: ${e.message}"
                isProcessing = false
            }
        }
    }

    // Function to handle food recognition
    fun recognizeFood(image: Bitmap?, foodName: String) {
        if (image == null && foodName.isBlank()) {
            errorMessage = "Please enter food name or take a photo"
            return
        }

        errorMessage = ""
        isProcessing = true

        coroutineScope.launch {
            try {
                val result = geminiService.doFoodRecognition(image, foodName)
                Log.i("CaptureFoodScreen", "Food recognition result: $result")

                val responseText = result.firstOrNull() ?: ""
                val estimatedCaloricValue = responseText.filter { it.isDigit() }.toIntOrNull() ?: 0
                Log.i("CaptureFoodScreen", "Food recognition caloric value: $estimatedCaloricValue")

                recognizedFood = result.firstOrNull()?.let { Pair(it, estimatedCaloricValue) }
                caloricValue = estimatedCaloricValue.toString()
                isProcessing = false
            } catch (e: Exception) {
                errorMessage = "Error recognizing food: ${e.message}"
                isProcessing = false
            }
        }
    }

    // Function to save food data to Firestore
    fun saveFoodData(foodName: String, caloricValue: Int) {
        isProcessing = true
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
                    isProcessing = false
                    showSuccessMessage = true
                    // Show success message briefly before navigating back
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1500)
                        navController.popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("CaptureFoodScreen", "Error saving food data", e)
                    errorMessage = "Failed to save: ${e.message}"
                    isProcessing = false
                }
        }
    }

    // Camera launcher
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            imageBitmap = bitmap
            errorMessage = "" // Clear any previous errors

            // If we have an image and auto-identify is enabled
            if (bitmap != null && autoIdentifyFood) {
                // Automatically identify the food after taking the photo
                identifyFoodFromImage(bitmap)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Capture Food",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        },
        bottomBar = { BottomNavigationBar(navController) }

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card for image capture
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clickable { cameraLauncher.launch(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        imageBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Food image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = Primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Tap to take a photo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "(Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Food details section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Food Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Food Name Text Field with edit button
                        OutlinedTextField(
                            value = foodName,
                            onValueChange = {
                                foodName = it
                                errorMessage = "" // Clear error when user types
                            },
                            label = { Text("Food Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = "Food Icon",
                                    tint = Primary
                                )
                            },
                            trailingIcon = if (imageBitmap != null) {
                                {
                                    IconButton(onClick = { identifyFoodFromImage(imageBitmap) }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Re-identify",
                                            tint = Primary
                                        )
                                    }
                                }
                            } else null,
                            shape = roundedShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Unfocused,
                                focusedLabelColor = Primary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Recognize food button
                        Button(
                            onClick = { recognizeFood(imageBitmap, foodName) },
                            enabled = !isProcessing && (foodName.isNotEmpty() || imageBitmap != null),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = Primary,
                                disabledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                disabledContentColor = Primary.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, if (isProcessing) Primary.copy(alpha = 0.5f) else Primary),
                            shape = roundedShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...")
                            } else {
                                Text("Calculate Calories")
                            }
                        }
                    }
                }

                // Display recognized food results
                AnimatedVisibility(
                    visible = recognizedFood != null,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300)) { it / 2 },
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Analysis Result",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                "Based on ${if (imageBitmap != null) "the image and " else ""}typical ingredients of ${foodName.ifEmpty { "this food" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            // Caloric Value Text Field
                            OutlinedTextField(
                                value = caloricValue,
                                onValueChange = {
                                    // Only allow numeric input
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        caloricValue = it
                                    }
                                },
                                label = { Text("Calories (kcal)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Caloric Value Icon",
                                        tint = Primary
                                    )
                                },
                                shape = roundedShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Unfocused,
                                    focusedLabelColor = Primary
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Save food data button
                            Button(
                                onClick = { confirmSave = true },
                                enabled = foodName.isNotEmpty() && caloricValue.isNotEmpty() && !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = Primary.copy(alpha = 0.5f),
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                ),
                                shape = roundedShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Food Entry")
                            }
                        }
                    }
                }

                // Error message display
                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Success message overlay
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Food entry saved successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Confirmation dialog
            if (confirmSave) {
                AlertDialog(
                    onDismissRequest = { confirmSave = false },
                    title = { Text("Save Food Entry") },
                    text = {
                        Column {
                            Text("Are you sure you want to save the following entry?")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = null,
                                    tint = Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = foodName,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$caloricValue calories")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmSave = false
                                saveFoodData(foodName, caloricValue.toIntOrNull() ?: 0)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            )
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmSave = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Processing overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }
    }
}