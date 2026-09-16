package com.example.mealrushapplication

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.net.Uri

@Composable
fun CustomerDashboardPage(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "customerDashboard") {

        // Homepage route
        composable("customerDashboard") {
            HomePage(
                navController = navController,
                onRestaurantClick = { restaurant ->
                    val safeName = Uri.encode(restaurant.name)
                    navController.navigate("menu/${restaurant.id}/$safeName")
                }
            )
        }

        // Chat list route
        composable("chats") {
            CustomerChatListScreen(navController = navController)
        }

        // Chat screen route with riderId, riderName, and orderId
        composable("chat/{userId}/{riderName}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val riderName = backStackEntry.arguments?.getString("riderName") ?: ""

            CustomerChatScreen(
                userId = userId,   // pass userId directly
                riderName = riderName,
                navController = navController
            )
        }
    }
}
