package com.inf2007.healthtracker

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.inf2007.healthtracker.Screens.LoginScreen
import com.inf2007.healthtracker.Screens.MainScreen
import com.inf2007.healthtracker.Screens.SignUpScreen
import com.google.firebase.auth.FirebaseUser
import com.inf2007.healthtracker.Screens.MealRecommendationScreen
import com.inf2007.healthtracker.Screens.ProfileScreen

@Composable
fun NavGraph(
    user: FirebaseUser?,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = if (user == null) "auth_graph" else "main_graph"
    ) {
        authGraph(navController)
        mainGraph(navController, user)
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(startDestination = "login_screen", route = "auth_graph") {
        composable("login_screen") { LoginScreen(navController) }
        composable("signup_screen") { SignUpScreen(navController) }
        composable("main_screen") { MainScreen(navController) }
    }
}

fun NavGraphBuilder.mainGraph(navController: NavHostController, user: FirebaseUser?) {
    navigation(startDestination = "main_screen", route = "main_graph") {
        composable("main_screen") {
            if (user != null) {
                MainScreen(navController)
            }
        }
        composable("meal_recommendation_screen") {
            MealRecommendationScreen(navController)
        }
        composable("profile_screen") {
            ProfileScreen(navController)
        }
    }
}