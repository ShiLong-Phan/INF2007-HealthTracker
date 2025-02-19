package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.inf2007.healthtracker.utilities.Business
import com.inf2007.healthtracker.utilities.YelpApi
import com.inf2007.healthtracker.BuildConfig
import com.inf2007.healthtracker.utilities.YelpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MealRecommendationScreen(
    navController: NavController
) {
    var recommendations by remember { mutableStateOf<List<Business>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val apiKey = BuildConfig.yelpApiKey
                val response = withContext(Dispatchers.IO) {
                    YelpApi.searchRestaurants(
                        location = "Singapore",
                        term = "vegetarian",
                        categories = "",
                        limit = 20,
                        apiKey = apiKey
                    )
                }

                if (response != null) {
                    val yelpResponse = Gson().fromJson(response, YelpResponse::class.java)
                    recommendations = yelpResponse.businesses
                } else {
                    errorMessage = "Failed to get recommendations"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMessage.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(recommendations) { business ->
                Column(modifier = Modifier.padding(8.dp)) {
                    val painter = rememberAsyncImagePainter(business.image_url)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = business.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Rating: ${business.rating}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = business.location.address1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}