package com.example.mealrushapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.edit


@Composable
fun EmailLoginScreen(navController: NavController) {
    var isSignUpMode by remember { mutableStateOf(false) }

    // Signup states
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }
    var signupGmail by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    // Login states
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = {
                if (isSignUpMode) {
                    isSignUpMode = false
                    signupName = ""; signupEmail = ""; signupPassword = ""; signupPhone = ""; signupGmail = ""
                } else {
                    navController.popBackStack()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = (-250).dp)
                .padding(start = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isSignUpMode) "Sign Up" else "LOGIN",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSignUpMode) {
                OutlinedTextField(value = signupName, onValueChange = { signupName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                OutlinedTextField(value = signupEmail, onValueChange = { signupEmail = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                OutlinedTextField(
                    value = signupPassword,
                    onValueChange = { signupPassword = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (signupPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (signupPasswordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    }
                )
                OutlinedTextField(value = signupPhone, onValueChange = { signupPhone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                OutlinedTextField(value = signupGmail, onValueChange = { signupGmail = it }, label = { Text("Gmail (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        val userId = firestore.collection("users").document().id
                        val userData = hashMapOf(
                            "name" to signupName,
                            "email" to signupEmail,
                            "password" to signupPassword,
                            "phone" to signupPhone,
                            "gmail" to signupGmail,
                            "role" to "customer"
                        )
                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                isLoading = false
                                signupName = ""; signupEmail = ""; signupPassword = ""; signupPhone = ""; signupGmail = ""
                                isSignUpMode = false
                                errorMessage = "Account created successfully. Please log in."
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = e.message
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Sign up", color = Color.White)
                }
            } else {
                OutlinedTextField(value = loginEmail, onValueChange = { loginEmail = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (loginPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (loginPasswordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true

                        // Hard-coded rider account
                        val riderEmail = "rider@mealrush.com"
                        val riderPassword = "rider123"

                        // Hard-coded restaurant accounts
                        val restaurantAccounts = listOf(
                            mapOf(
                                "email" to "boscoffee@mealrush.com",
                                "password" to "boscoffee123",
                                "role" to "restaurantOwner",
                                "restaurantName" to "Bo's Coffee",
                                "restaurantId" to "bos_coffee"
                            ),
                            mapOf(
                                "email" to "manginasal@mealrush.com",
                                "password" to "inasal123",
                                "role" to "restaurantOwner",
                                "restaurantName" to "Mang Inasal",
                                "restaurantId" to "mang_inasal"
                            ),
                            mapOf(
                                "email" to "jollibee@mealrush.com",
                                "password" to "jollibee123",
                                "role" to "restaurantOwner",
                                "restaurantName" to "Jollibee",
                                "restaurantId" to "jollibee"
                            )
                        )

                        if (loginEmail == riderEmail && loginPassword == riderPassword) {
                            isLoading = false
                            val prefs = navController.context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                            prefs.edit { putString("role", "rider") }

                            navController.navigate("rider_home") {
                                popUpTo("auth") { inclusive = true }
                            }
                        } else {
                            // Check against hard-coded restaurant accounts
                            val matchedRestaurant = restaurantAccounts.find {
                                it["email"] == loginEmail && it["password"] == loginPassword
                            }

                            if (matchedRestaurant != null) {
                                isLoading = false
                                val prefs = navController.context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                                prefs.edit {
                                    putBoolean("staySignedIn", true)   // ✅ keep session
                                    putString("role", "restaurant")
                                    putString("restaurantId", matchedRestaurant["restaurantId"].toString())
                                    putString("restaurantName", matchedRestaurant["restaurantName"].toString())
                                }

                                navController.navigate("restaurant_home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            } else {
                                // Fallback to Firestore users collection for customers
                                firestore.collection("users")
                                    .whereEqualTo("email", loginEmail)
                                    .whereEqualTo("password", loginPassword)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        isLoading = false
                                        if (!result.isEmpty) {
                                            val userDoc = result.documents.first()
                                            val role = userDoc.getString("role") ?: "customer"

                                            val prefs = navController.context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                                            prefs.edit { putString("role", role) }

                                            if (role == "rider") {
                                                navController.navigate("rider_home") {
                                                    popUpTo("auth") { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate("home") {
                                                    popUpTo("auth") { inclusive = true }
                                                }
                                            }
                                        } else {
                                            errorMessage = "Invalid username or password"
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = e.message
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Log in", color = Color.White)
                }


                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { isSignUpMode = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Sign up", color = Color.White)
                }
            }

            if (isLoading) {
                CircularProgressIndicator()
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

