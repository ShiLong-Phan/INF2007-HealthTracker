package com.inf2007.healthtracker.Screens

import android.graphics.Bitmap
import android.widget.Toast
import android.util.Log
import android.net.Uri
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
import com.inf2007.healthtracker.utilities.ImageUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.sp

// Food Database API Interface
interface FoodDatabaseApi {
    @GET("product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): FoodProductResponse
}

// Response models
data class FoodProductResponse(
    val status: Int,
    val product: FoodProduct?
)

data class FoodProduct(
    val product_name: String,
    val brands: String?,
    val nutriments: Nutriments?,
    val quantity: String?
)

data class Nutriments(
    val energy_value: Float?,
    val energy_kcal_100g: Float?,
    val energy_kcal: Float?,
    val energy_kcal_serving: Float?,
    val energy_kcal_value: Float?,
    val calories: Float?,
    val energy: Float?,               // Some products use this format for energy in kJ
    val energy_100g: Float?,          // Energy per 100g in kJ
    val energy_unit: String?,         // Can be "kJ" or "kcal"
    val serving_size: String?
) {
    override fun toString(): String {
        return "Nutriments(energy_value=$energy_value, energy_kcal_100g=$energy_kcal_100g, " +
                "energy_kcal=$energy_kcal, energy_kcal_serving=$energy_kcal_serving, " +
                "energy_kcal_value=$energy_kcal_value, calories=$calories, " +
                "energy=$energy, energy_100g=$energy_100g, energy_unit=$energy_unit, " +
                "serving_size=$serving_size)"
    }
}

// Food Database Service
object FoodDatabaseService {
    private const val BASE_URL = "https://world.openfoodfacts.org/api/v0/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val foodApi = retrofit.create(FoodDatabaseApi::class.java)

    suspend fun getProductByBarcode(barcode: String): ProductInfo? {
        return try {
            val response = foodApi.getProductByBarcode(barcode)
            if (response.status == 1 && response.product != null) {
                val product = response.product
                val nutriments = product.nutriments

                // Try to get serving-specific calories first, then fall back to per 100g values
                val isPerServing = nutriments?.energy_kcal_serving != null ||
                        nutriments?.energy_kcal_value != null

                // Per serving or per 100g calories - priority order for extracting calories
                // First, check if energy_unit is kcal and energy_value exists
                val perUnitCalories = if (nutriments?.energy_unit == "kcal" && nutriments.energy_value != null) {
                    // Direct kcal value
                    nutriments.energy_value
                } else {
                    // For package calculations, we prefer per 100g values
                    nutriments?.energy_kcal_100g // First try per 100g value
                        ?: nutriments?.calories // Generic calories field
                        ?: nutriments?.energy_kcal // Another variant of energy in kcal
                        ?: nutriments?.energy_kcal_value // Sometimes used for per 100g too
                        ?: nutriments?.energy_kcal_serving // Last resort - per serving
                        ?: if (nutriments?.energy_unit == "kJ" && nutriments.energy_value != null) {
                            // Convert kJ to kcal if energy_unit is explicitly "kJ"
                            nutriments.energy_value / 4.184f
                        } else if (nutriments?.energy != null) {
                            // Assuming energy is in kJ if no unit specified
                            nutriments.energy / 4.184f
                        } else if (nutriments?.energy_100g != null) {
                            // Assuming energy_100g is in kJ if no unit specified
                            nutriments.energy_100g / 4.184f
                        } else {
                            0f
                        }
                }

                // Debug info
                Log.d("FoodDatabaseService", "Nutriment data: ${nutriments.toString()}")
                Log.d("FoodDatabaseService", "Per unit calories (raw): $perUnitCalories")

                val servingSizeInfo = nutriments?.serving_size
                val productQuantity = product.quantity

                // Calculate total calories for the entire package
                var totalCalories: Int? = null

                // If we have the product quantity and it's in a parsable format
                if (!productQuantity.isNullOrBlank()) {
                    try {
                        // Parse the quantity - this is simplified and would need more robust parsing
                        // for all possible formats (g, kg, ml, L, etc.)
                        val quantityRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(g|ml|kg|l)", RegexOption.IGNORE_CASE)
                        val match = quantityRegex.find(productQuantity)

                        if (match != null) {
                            val amount = match.groupValues[1].toFloatOrNull() ?: 0f
                            val unit = match.groupValues[2].lowercase()

                            // Convert to base unit (g or ml)
                            val baseAmount = when (unit) {
                                "kg" -> amount * 1000
                                "l" -> amount * 1000
                                else -> amount
                            }

                            // Calculate total calories
                            totalCalories = if (isPerServing && !servingSizeInfo.isNullOrBlank()) {
                                // If we have per serving info, we need serving size and servings per package
                                // This is a very rough approximation and would need better parsing
                                val servingRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(g|ml)", RegexOption.IGNORE_CASE)
                                val servingMatch = servingRegex.find(servingSizeInfo)

                                if (servingMatch != null) {
                                    val servingAmount = servingMatch.groupValues[1].toFloatOrNull() ?: 0f
                                    val servingsPerPackage = baseAmount / servingAmount
                                    (perUnitCalories * servingsPerPackage).toInt()
                                } else null
                            } else {
                                // If per 100g/ml, calculate based on total weight/volume
                                val calculatedCalories = (perUnitCalories * baseAmount / 100).toInt()
                                Log.d("FoodDatabaseService", "Calculated total calories: $calculatedCalories (perUnit: $perUnitCalories × weight: $baseAmount ÷ 100)")
                                calculatedCalories
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FoodDatabaseService", "Error calculating total calories", e)
                    }
                } else if (nutriments?.energy_kcal != null) {
                    // Some products might directly store total calories in energy_kcal
                    totalCalories = nutriments.energy_kcal.toInt()
                }

                // Get product name with brand if available
                val name = if (product.product_name.contains(product.brands ?: "")) {
                    product.product_name
                } else {
                    val brand = product.brands
                    if (!brand.isNullOrBlank()) {
                        "${product.brands} - ${product.product_name}"
                    } else {
                        product.product_name
                    }
                }

                val finalCalories = totalCalories ?: perUnitCalories.toInt()

                Log.d("FoodDatabaseService", "Final product info - Name: $name, Calories: $finalCalories, Total Calories: $totalCalories")

                ProductInfo(
                    productName = name,
                    calories = finalCalories,
                    servingSize = servingSizeInfo,
                    isPerServing = isPerServing,
                    perUnitCalories = perUnitCalories.toInt(),
                    totalQuantity = productQuantity,
                    totalCalories = totalCalories
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FoodDatabaseService", "Error fetching product data", e)
            null
        }
    }

    // Backup method to use AI for getting nutrition info for unknown products
    suspend fun getProductInfoWithAI(
        barcode: String,
        productName: String?,
        geminiService: GeminiService,
        image: Bitmap?
    ): ProductInfo {
        return try {
            val name = productName ?: "Unknown Product (Barcode: $barcode)"

            // Use the existing GeminiService to estimate calories
            if (image != null) {
                val result = geminiService.doFoodRecognition(image, name)
                val responseText = result.firstOrNull() ?: ""

                // Extract calorie value using regex for more reliability
                val calorieRegex = Regex("Calories:\\s*(\\d+)\\s*kcal", RegexOption.IGNORE_CASE)
                val matchResult = calorieRegex.find(responseText)

                val estimatedCalories = if (matchResult != null) {
                    matchResult.groupValues[1].toIntOrNull() ?: 0
                } else {
                    responseText.filter { it.isDigit() }.toIntOrNull() ?: 0
                }

                ProductInfo(name, estimatedCalories, null, false, estimatedCalories, null, null)
            } else {
                // If no image, make a more generic request
                val result = geminiService.estimateCaloriesForFood(name)
                val calories = result.toIntOrNull() ?: 0
                ProductInfo(name, calories, null, false, calories, null, null)
            }
        } catch (e: Exception) {
            Log.e("FoodDatabaseService", "Error getting AI-based nutrition info", e)
            ProductInfo(productName ?: "Unknown Product", 0, null, false, 0, null, null)
        }
    }
}

data class ProductInfo(
    val productName: String,
    val calories: Int,
    val servingSize: String?,
    val isPerServing: Boolean,
    val perUnitCalories: Int,
    val totalQuantity: String?,
    val totalCalories: Int?
)

// Add this extension function to GeminiService
suspend fun GeminiService.estimateCaloriesForFood(foodName: String): String {
    // Implementation would depend on your existing GeminiService
    // This is a placeholder, you would need to implement this in your GeminiService class
    return "0"
}

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
    var showImageSourceOptions by remember { mutableStateOf(false) }

    // New state variables for barcode scanning
    var isBarcodeScannerActive by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var productSource by remember { mutableStateOf("") }

    // New state variables for package information
    var servingSize by remember { mutableStateOf<String?>(null) }
    var isPerServing by remember { mutableStateOf(false) }
    var totalQuantity by remember { mutableStateOf<String?>(null) }
    var totalCalories by remember { mutableStateOf<Int?>(null) }
    var perUnitCalories by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val roundedShape = RoundedCornerShape(12.dp)
    val cardShape = RoundedCornerShape(16.dp)

    // Initialize GeminiService
    val geminiService = remember { GeminiService(BuildConfig.geminiApiKey) }

    // Initialize ImageUtils for handling gallery images
    val imageUtils = remember { ImageUtils(context) }

    // Function to process barcode result
    fun processBarcodeResult(barcode: String, image: Bitmap?) {
        isProcessing = true
        scannedBarcode = barcode

        coroutineScope.launch {
            try {
                // First try the main database
                val productInfo = withContext(Dispatchers.IO) {
                    FoodDatabaseService.getProductByBarcode(barcode)
                }

                if (productInfo != null) {
                    // Product found in database
                    foodName = productInfo.productName
                    // Use total calories if available, otherwise use per unit calories
                    caloricValue = if (productInfo.totalCalories != null) {
                        productInfo.totalCalories.toString()
                    } else {
                        productInfo.calories.toString()
                    }

                    servingSize = productInfo.servingSize
                    isPerServing = productInfo.isPerServing
                    totalQuantity = productInfo.totalQuantity
                    totalCalories = productInfo.totalCalories
                    perUnitCalories = productInfo.perUnitCalories
                    productSource = "Food Database"

                    // Create descriptive text for calories
                    val calorieDesc = if (totalCalories != null) {
                        "${totalCalories} total calories" +
                                (if (totalQuantity != null) " in $totalQuantity" else "") +
                                (if (isPerServing) " (${perUnitCalories} per " +
                                        (servingSize ?: "serving") + ")" else "")
                    } else if (isPerServing) {
                        "${perUnitCalories} calories per " +
                                (servingSize ?: "serving")
                    } else {
                        "${perUnitCalories} calories per 100g/ml"
                    }

                    Log.d("CaptureFoodScreen", "Product info: $productInfo")
                    Log.d("CaptureFoodScreen", "Caloric value being used: $caloricValue")

                    Toast.makeText(
                        context,
                        "Product found: ${productInfo.productName} - $calorieDesc",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Product not found - simply show not found message without AI attempt
                    Log.i("CaptureFoodScreen", "Product not found in database")

                    foodName = ""
                    caloricValue = "0"
                    servingSize = null
                    isPerServing = false
                    totalQuantity = null
                    totalCalories = null
                    perUnitCalories = 0
                    productSource = "Not in Database"

                    Toast.makeText(
                        context,
                        "Product with barcode $barcode not found in database. Please enter details manually.",
                        Toast.LENGTH_LONG
                    ).show()

                    errorMessage = "Product not found. Please try again"
                }

                // Set recognized food to trigger the results display
                recognizedFood = Pair(foodName, caloricValue.toIntOrNull() ?: 0)
                isProcessing = false
            } catch (e: Exception) {
                Log.e("CaptureFoodScreen", "Error processing barcode", e)
                errorMessage = "Error processing barcode: ${e.message}"
                isProcessing = false
            }
        }
    }

    // Function to scan barcode from image
    fun scanBarcodeFromImage(bitmap: Bitmap) {
        isProcessing = true
        errorMessage = ""

        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        when (barcode.valueType) {
                            Barcode.TYPE_PRODUCT, Barcode.TYPE_TEXT -> {
                                barcode.rawValue?.let {
                                    Log.i("CaptureFoodScreen", "Barcode scanned: $it")
                                    processBarcodeResult(it, bitmap)
                                }
                            }
                            else -> {
                                errorMessage = "Unsupported barcode format"
                                isProcessing = false
                            }
                        }
                    } else {
                        errorMessage = "No barcode found in image. Try taking a clearer photo."
                        isProcessing = false
                    }
                }
                .addOnFailureListener {
                    Log.e("CaptureFoodScreen", "Barcode scanning failed", it)
                    errorMessage = "Failed to scan barcode: ${it.message}"
                    isProcessing = false
                }
        } catch (e: Exception) {
            Log.e("CaptureFoodScreen", "Error setting up barcode scanner", e)
            errorMessage = "Error setting up barcode scanner: ${e.message}"
            isProcessing = false
        }
    }

    // Function to identify food from image with improved error handling
    fun identifyFoodFromImage(image: Bitmap?) {
        if (image == null) {
            errorMessage = "Please provide an image to identify food"
            return
        }

        errorMessage = ""
        isProcessing = true

        coroutineScope.launch {
            try {
                // First identify the food from the image using our improved service
                Log.i("CaptureFoodScreen", "Starting food identification...")
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
                    Log.i("CaptureFoodScreen", "Starting calorie recognition for $foodName")
                    val result = geminiService.doFoodRecognition(image, foodName)
                    Log.i("CaptureFoodScreen", "Food recognition result: $result")

                    // Extract calorie value using regex for more reliability
                    val responseText = result.firstOrNull() ?: ""
                    val calorieRegex = Regex("Calories:\\s*(\\d+)\\s*kcal", RegexOption.IGNORE_CASE)
                    val matchResult = calorieRegex.find(responseText)

                    val estimatedCaloricValue = if (matchResult != null) {
                        matchResult.groupValues[1].toIntOrNull() ?: 0
                    } else {
                        responseText.filter { it.isDigit() }.toIntOrNull() ?: 0
                    }

                    Log.i("CaptureFoodScreen", "Food recognition caloric value: $estimatedCaloricValue")

                    recognizedFood = Pair(responseText, estimatedCaloricValue)
                    caloricValue = estimatedCaloricValue.toString()
                    productSource = "AI Food Recognition"
                } else {
                    errorMessage = "Could not identify food from image. Please enter food name manually."
                    Log.w("CaptureFoodScreen", "Food identification failed: $identifiedFoodName")
                }
                isProcessing = false
            } catch (e: Exception) {
                Log.e("CaptureFoodScreen", "Error in food identification process", e)
                errorMessage = "Error identifying food: ${e.message}"
                isProcessing = false
            }
        }
    }

    // Improved function to handle food recognition
    fun recognizeFood(image: Bitmap?, foodName: String) {
        if (image == null && foodName.isBlank()) {
            errorMessage = "Please enter food name or provide an image"
            return
        }

        errorMessage = ""
        isProcessing = true

        coroutineScope.launch {
            try {
                Log.i("CaptureFoodScreen", "Starting calorie recognition for: $foodName")
                val result = geminiService.doFoodRecognition(image, foodName)
                Log.i("CaptureFoodScreen", "Food recognition result: $result")

                val responseText = result.firstOrNull() ?: ""

                // Extract calorie value using regex for more reliability
                val calorieRegex = Regex("Calories:\\s*(\\d+)\\s*kcal", RegexOption.IGNORE_CASE)
                val matchResult = calorieRegex.find(responseText)

                val estimatedCaloricValue = if (matchResult != null) {
                    matchResult.groupValues[1].toIntOrNull() ?: 0
                } else {
                    responseText.filter { it.isDigit() }.toIntOrNull() ?: 0
                }

                Log.i("CaptureFoodScreen", "Food recognition caloric value: $estimatedCaloricValue")

                recognizedFood = Pair(responseText, estimatedCaloricValue)
                caloricValue = estimatedCaloricValue.toString()
                productSource = "AI Calorie Estimation"
                isProcessing = false
            } catch (e: Exception) {
                Log.e("CaptureFoodScreen", "Error recognizing food", e)
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
                "dateString" to todayString,
                "barcode" to scannedBarcode,
                "source" to productSource
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

            if (bitmap != null) {
                if (isBarcodeScannerActive) {
                    // Process as barcode image
                    scanBarcodeFromImage(bitmap)
                    isBarcodeScannerActive = false
                } else {
                    // Automatically identify the food after taking the photo
                    // Only if we're not in barcode mode, reset the barcode data
                    // to allow AI food identification
                    scannedBarcode = null
                    productSource = ""

                    if (autoIdentifyFood) {
                        identifyFoodFromImage(bitmap)
                    }
                }
            }
        }

    // Gallery launcher
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    // Convert URI to Bitmap
                    val bitmap = imageUtils.uriToBitmap(uri)
                    imageBitmap = bitmap
                    errorMessage = "" // Clear any previous errors

                    // Check if we're in barcode mode or regular mode
                    if (isBarcodeScannerActive) {
                        scanBarcodeFromImage(bitmap)
                        isBarcodeScannerActive = false
                    } else {
                        // If not in barcode mode, reset barcode data to allow AI food identification
                        scannedBarcode = null
                        productSource = ""

                        if (autoIdentifyFood) {
                            identifyFoodFromImage(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Error loading image: ${e.message}"
                    Log.e("CaptureFoodScreen", "Error loading image from gallery", e)
                }
            }
        }

    val BarcodeIcon = ImageVector.Builder(
        name = "Barcode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Barcode outer frame
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f
        ) {
            // Outer rectangular frame
            moveTo(2f, 4f)
            lineTo(22f, 4f)
            lineTo(22f, 20f)
            lineTo(2f, 20f)
            close()
        }

        // First line
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 6f)
            lineTo(4f, 6f)
            lineTo(4f, 18f)
            lineTo(3f, 18f)
            close()
        }

        // Second line
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 6f)
            lineTo(5.5f, 6f)
            lineTo(5.5f, 18f)
            lineTo(5f, 18f)
            close()
        }

        // Third line
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.5f, 6f)
            lineTo(7.5f, 6f)
            lineTo(7.5f, 18f)
            lineTo(6.5f, 18f)
            close()
        }

        // Fourth line (thick)
        path(fill = SolidColor(Color.Black)) {
            moveTo(8.5f, 6f)
            lineTo(10f, 6f)
            lineTo(10f, 18f)
            lineTo(8.5f, 18f)
            close()
        }

        // Fifth line
        path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 6f)
            lineTo(11.5f, 6f)
            lineTo(11.5f, 18f)
            lineTo(11f, 18f)
            close()
        }

        // Sixth line
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.5f, 6f)
            lineTo(13.5f, 6f)
            lineTo(13.5f, 18f)
            lineTo(12.5f, 18f)
            close()
        }

        // Seventh line (thick)
        path(fill = SolidColor(Color.Black)) {
            moveTo(14.5f, 6f)
            lineTo(16f, 6f)
            lineTo(16f, 18f)
            lineTo(14.5f, 18f)
            close()
        }

        // Eighth line
        path(fill = SolidColor(Color.Black)) {
            moveTo(17f, 6f)
            lineTo(17.5f, 6f)
            lineTo(17.5f, 18f)
            lineTo(17f, 18f)
            close()
        }

        // Ninth line (thick)
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.5f, 6f)
            lineTo(20f, 6f)
            lineTo(20f, 18f)
            lineTo(18.5f, 18f)
            close()
        }

        // Tenth line (edge)
        path(fill = SolidColor(Color.Black)) {
            moveTo(21f, 6f)
            lineTo(21.5f, 6f)
            lineTo(21.5f, 18f)
            lineTo(21f, 18f)
            close()
        }
    }.build()

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
                // Card for image capture/upload
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
                            .clickable { showImageSourceOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        imageBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Food image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Add an overlay button to change the image
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                FloatingActionButton(
                                    onClick = { showImageSourceOptions = true },
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    contentColor = Primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change Image"
                                    )
                                }
                            }
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
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Add Photo",
                                    tint = Primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Add photo for AI analysis",
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

                        // Barcode scan button
                        Button(
                            onClick = {
                                isBarcodeScannerActive = true
                                showImageSourceOptions = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = roundedShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = BarcodeIcon,
                                contentDescription = "Scan Barcode",
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Barcode")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "(Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        // Display scanned barcode if available
                        AnimatedVisibility(visible = scannedBarcode != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = BarcodeIcon,
                                    contentDescription = null,
                                    tint = Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Barcode: ${scannedBarcode ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Food Name Text Field with edit button - modified to never show refresh button when product is found by barcode
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
                            trailingIcon = if (imageBitmap != null && scannedBarcode == null) {
                                {
                                    IconButton(
                                        onClick = { identifyFoodFromImage(imageBitmap) }
                                    ) {
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

                        // Recognize food button (only show if not using barcode scanning)
                        if (scannedBarcode == null) {
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
                }

                // No product found card has been completely removed

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

                            // Show serving and package information if available
                            AnimatedVisibility(visible = productSource.contains("Database") && (servingSize != null || totalQuantity != null)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Show serving size if available
                                    if (servingSize != null && isPerServing) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FoodBank,
                                                contentDescription = null,
                                                tint = Primary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Per serving: $perUnitCalories calories ($servingSize)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    // Show package quantity if available
                                    if (totalQuantity != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().padding(top = if(servingSize != null) 4.dp else 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory,
                                                contentDescription = null,
                                                tint = Primary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Package: $totalQuantity" +
                                                        (if (totalCalories != null) " ($totalCalories total calories)" else ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Show data source
                            if (servingSize != null && isPerServing) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = Primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Serving size: $servingSize")
                                }
                            }

                            if (productSource.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = when {
                                            productSource == "Not in Database" -> Icons.Default.Error
                                            productSource.contains("Database") -> Icons.Default.DataObject
                                            else -> Icons.Default.Psychology
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            productSource == "Not in Database" -> MaterialTheme.colorScheme.error
                                            else -> Primary.copy(alpha = 0.8f)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Source: $productSource",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Text(
                                when {
                                    scannedBarcode != null && productSource == "Not in Database" ->
                                        "No product found with barcode $scannedBarcode"
                                    scannedBarcode != null && productSource.contains("Database") -> {
                                        if (totalCalories != null) {
                                            "Total package: $totalCalories calories" +
                                                    (if (totalQuantity != null) " in $totalQuantity" else "")
                                        } else if (isPerServing) {
                                            "Per serving: $perUnitCalories calories" +
                                                    (if (servingSize != null) " ($servingSize)" else "")
                                        } else {
                                            "Per 100g/ml: $perUnitCalories calories"
                                        }
                                    }
                                    scannedBarcode != null ->
                                        "Estimated nutrition for scanned product"
                                    imageBitmap != null ->
                                        "Based on the image and ${if (foodName.isNotEmpty()) "typical ingredients of $foodName" else "AI analysis"}"
                                    else ->
                                        "Based on typical ingredients of ${foodName.ifEmpty { "this food" }}"
                                },
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
                                enabled = foodName.isNotEmpty() &&
                                        !foodName.startsWith("Unknown Product") &&
                                        foodName != "No Product Found" &&
                                        caloricValue.isNotEmpty() &&
                                        !isProcessing,
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

            // Image source selection dialog
            if (showImageSourceOptions) {
                AlertDialog(
                    onDismissRequest = {
                        showImageSourceOptions = false
                        isBarcodeScannerActive = false
                    },
                    title = {
                        Text(if (isBarcodeScannerActive) "Scan Barcode" else "Add Food Image")
                    },
                    text = {
                        Text(if (isBarcodeScannerActive)
                            "Take a photo of the barcode on the product packaging"
                        else "Choose an image source"
                        )
                    },
                    confirmButton = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        showImageSourceOptions = false
                                        cameraLauncher.launch(null)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Camera",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = {
                                        showImageSourceOptions = false
                                        galleryLauncher.launch("image/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Gallery",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showImageSourceOptions = false
                                isBarcodeScannerActive = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
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
                                Text(
                                    if (isPerServing) "$caloricValue calories per serving"
                                    else "$caloricValue calories"
                                )
                            }

                            if (scannedBarcode != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = BarcodeIcon,
                                        contentDescription = null,
                                        tint = Primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Barcode: $scannedBarcode")
                                }
                            }

                            if (productSource.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            productSource == "Not in Database" -> Icons.Default.Error
                                            productSource.contains("Database") -> Icons.Default.DataObject
                                            else -> Icons.Default.Psychology
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            productSource == "Not in Database" -> MaterialTheme.colorScheme.error
                                            else -> Primary
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Source: $productSource")
                                }
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