package com.example.mealrushapplication

import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mealrushapplication.ui.theme.MealRushApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#FF9800")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val prefs = getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
        val staySignedIn = prefs.getBoolean("staySignedIn", false)

        // ✅ Removed forced sign‑out. Session persists until explicit logout.
        // if (!staySignedIn) {
        //     FirebaseAuth.getInstance().signOut()
        // }

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            prefs.edit { putBoolean("staySignedIn", true) }
                        }
                    }
            } catch (_: ApiException) {}
        }

        setContent {
            MealRushApplicationTheme {
                val navController = rememberNavController()
                val cart = remember { mutableStateListOf<MenuItem>() }

                val currentUser = FirebaseAuth.getInstance().currentUser
                val savedRole = prefs.getString("role", null)

                // ✅ Updated startDestination logic
                val startDestination = when {
                    currentUser != null -> {
                        when {
                            currentUser.email == "rider@example.com" -> "rider_home"
                            savedRole == "restaurant" -> "restaurant_home"
                            else -> "home"
                        }
                    }
                    savedRole != null -> {
                        when (savedRole) {
                            "rider" -> "rider_home"
                            "restaurant" -> "restaurant_home"
                            "customer" -> "home"
                            else -> "auth"
                        }
                    }
                    else -> "auth"
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomePage(
                                navController = navController,
                                onRestaurantClick = { restaurant ->
                                    val safeName = Uri.encode(restaurant.name)
                                    navController.navigate("menu/${restaurant.id}/$safeName")
                                }
                            )
                        }

                        composable("chats") {
                            CustomerChatListScreen(navController = navController)
                        }

                        composable("chat/{userId}/{riderName}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            val riderName = backStackEntry.arguments?.getString("riderName") ?: ""
                            CustomerChatScreen(userId, riderName, navController)
                        }

                        composable("menu/{restaurantId}/{restaurantName}") { backStackEntry ->
                            val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
                            val restaurantName = Uri.decode(backStackEntry.arguments?.getString("restaurantName") ?: "")
                            MenuPage(
                                restaurantId = restaurantId,
                                restaurantName = restaurantName,
                                cart = cart,
                                onCheckout = { navController.navigate("checkout") }
                            )
                        }

                        composable("checkout") {
                            CheckoutPage(
                                cartItems = cart,
                                onCheckout = { cartList, subtotal ->
                                    val restaurantId = cartList.firstOrNull()?.restaurantId ?: ""
                                    navController.navigate("payment/$subtotal/$restaurantId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("payment/{subtotal}/{restaurantId}") { backStackEntry ->
                            val subtotal = backStackEntry.arguments?.getString("subtotal")?.toDouble() ?: 0.0
                            val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: ""
                            PaymentPage(
                                cart = cart,
                                subtotal = subtotal,
                                restaurantId = restaurantId,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                onConfirmPayment = { navController.popBackStack("home", inclusive = false) }
                            )
                        }

                        composable("profile") { ProfilePage(navController = navController) }
                        composable("orders") { OrdersPage(navController = navController) }

                        composable("orderDetail/{orderId}") { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                            val order = backStackEntry.savedStateHandle.get<Map<String, Any>>("orderData") ?: emptyMap()
                            OrderDetailPage(navController, orderId, order)
                        }

                        composable("available_orders") {
                            AvailableOrdersPage(navController, isOnline = true)
                        }

                        composable(
                            "order_details/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                            OrderDetailsPage(navController, orderId)
                        }

                        composable("auth") {
                            AuthScreen(
                                activity = this@MainActivity,
                                googleSignInLauncher = googleSignInLauncher,
                                navController = navController
                            )
                        }

                        composable("email_login") { EmailLoginScreen(navController = navController) }
                        composable("rider_home") { RiderDashboardPage() }

                        composable("gcash_payment/{orderId}/{amount}") { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                            GCashPaymentScreen(orderId = orderId, totalAmount = amount, navController = navController)
                        }

                        composable("restaurant_home") {
                            RestaurantAccountScreen(
                                navController = navController,
                                context = LocalContext.current,
                                db = FirebaseFirestore.getInstance()
                            )
                        }
                        composable("data_screen") { DataScreen(navController) }

                    }
                }
            }
        }
    }
}
