package com.inf2007.healthtracker.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val navBarColor = ContextCompat.getColor(context, android.R.color.white) // Adjust this to match your desired color

    BottomNavigation(
        modifier = Modifier
            .navigationBarsPadding()
            .background(Color(navBarColor)) // Set the background color
    ) {
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = currentRoute == "dashboard_screen",
            onClick = {
                if (currentRoute != "dashboard_screen") {
                    navController.navigate("dashboard_screen") {
                        popUpTo("dashboard_screen") { inclusive = true }
                    }
                }
            }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = "Meal Recommendation") },
            label = { Text("Meal Recs") },
            selected = currentRoute == "meal_recommendation_screen",
            onClick = {
                if (currentRoute != "meal_recommendation_screen") {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    navController.navigate("meal_recommendation_screen/$userId") {
                        popUpTo("dashboard_screen") { inclusive = false }
                    }
                }
            }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile_screen",
            onClick = {
                if (currentRoute != "profile_screen") {
                    navController.navigate("profile_screen") {
                        popUpTo("dashboard_screen") { inclusive = false }
                    }
                }
            }
        )
    }

}