package com.example.mealrushapplication

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.core.content.edit

object UserSession {
    var currentUserId: String? = null
}

@Composable
fun AuthScreen(
    activity: Activity,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    navController: NavController
) {
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(color = Color.White, darkIcons = true)
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(activity.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, gso)

    val restaurantAccounts = listOf(
        mapOf("email" to "boscoffee@mealrush.com", "role" to "restaurant", "restaurantName" to "Bo's Coffee", "restaurantId" to "bos_coffee"),
        mapOf("email" to "manginasal@mealrush.com", "role" to "restaurant", "restaurantName" to "Mang Inasal", "restaurantId" to "mang_inasal"),
        mapOf("email" to "jollibee@mealrush.com", "role" to "restaurant", "restaurantName" to "Jollibee", "restaurantId" to "jollibee")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF9800))
    ) {
        Image(
            painter = painterResource(id = R.drawable.meal_rush_logo),
            contentDescription = "Meal Rush Logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
                .height(400.dp)
                .width(400.dp),
            contentScale = ContentScale.Fit
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sign up or Log in", style = MaterialTheme.typography.headlineMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Select your preferred method to continue", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.weight(1f))

                // Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            val signInIntent = googleSignInClient.signInIntent
                            isLoading = true
                            googleSignInLauncher.launch(signInIntent)
                        }
                    },
                    modifier = Modifier
                        .width(260.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continue with Google", color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email Button
                OutlinedButton(
                    onClick = { navController.navigate("email_login") },
                    modifier = Modifier
                        .width(260.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Email",
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_email),
                            contentDescription = "Email",
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(24.dp)
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "By continuing, you agree to our Terms and Conditions and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }

    // FirebaseAuth listener
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                isLoading = false
                val userEmail = user.email
                val prefs = navController.context.getSharedPreferences("MealRushPrefs", android.content.Context.MODE_PRIVATE)

                when {
                    // Rider account
                    userEmail == "rider@example.com" -> {
                        UserSession.currentUserId = "rider_001"
                        prefs.edit {
                            putBoolean("staySignedIn", true)   // ✅ keep session
                            putString("role", "rider")
                        }
                        navController.navigate("rider_home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }

                    // Restaurant accounts
                    restaurantAccounts.any { it["email"] == userEmail } -> {
                        val matchedRestaurant = restaurantAccounts.first { it["email"] == userEmail }
                        UserSession.currentUserId = matchedRestaurant["restaurantId"].toString()

                        val prefs = navController.context.getSharedPreferences("MealRushPrefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit {
                            putBoolean("staySignedIn", true)   // ✅ keep session
                            putString("role", "restaurant")
                            putString("restaurantId", matchedRestaurant["restaurantId"].toString())
                            putString("restaurantName", matchedRestaurant["restaurantName"].toString())
                        }

                        navController.navigate("restaurant_home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }


                    // Default: customer
                    else -> {
                        UserSession.currentUserId = user.uid
                        prefs.edit {
                            putBoolean("staySignedIn", true)   // ✅ keep session
                            putString("role", "customer")
                        }
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
}
