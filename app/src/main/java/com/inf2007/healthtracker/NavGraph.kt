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
import com.inf2007.healthtracker.Screens.CaptureFoodScreen
import com.inf2007.healthtracker.Screens.DashboardScreen
import com.inf2007.healthtracker.Screens.MealPlanHistoryDetailScreen
import com.inf2007.healthtracker.Screens.MealPlanHistoryScreen
import com.inf2007.healthtracker.Screens.MealRecommendationScreen
import com.inf2007.healthtracker.Screens.ProfileScreen
import com.inf2007.healthtracker.Screens.HistoryScreen

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
        composable("dashboard_screen") { DashboardScreen(navController) }
    }
}

fun NavGraphBuilder.mainGraph(navController: NavHostController, user: FirebaseUser?) {
    navigation(startDestination = "dashboard_screen", route = "main_graph") {

        composable("profile_screen") { ProfileScreen(navController) }

        composable("dashboard_screen") { DashboardScreen(navController) }

        composable("history_screen") { HistoryScreen(navController) }

        composable("capture_food_screen") { CaptureFoodScreen(navController) }

        composable("meal_plan_history_screen") { MealPlanHistoryScreen(navController) }

        composable(
            route = "meal_recommendation_screen/{userId}",
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MealRecommendationScreen(navController = navController, userId = userId)
        }

        composable("meal_plan_history_detail/{uid}/{timestamp}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val timestamp = backStackEntry.arguments?.getString("timestamp")?.toLongOrNull() ?: 0L

            MealPlanHistoryDetailScreen(navController, uid, timestamp)
        }
    }
}