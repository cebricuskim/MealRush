package com.example.mealrushapplication

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthScreens(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onLoginSuccess: () -> Unit // 👈 added callback
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🔑 Login Button
        Button(
            onClick = {
                val cleanEmail = email.trim()
                val cleanPassword = password.trim()

                if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
                    message = "Email and password cannot be empty."
                } else if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    message = "Please enter a valid email address."
                } else {
                    isLoading = true
                    auth.signInWithEmailAndPassword(cleanEmail, cleanPassword)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onLoginSuccess() // 👈 switch to HomePage
                            } else {
                                message = "Login failed: ${task.exception?.message}"
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Login") }

        Spacer(modifier = Modifier.height(8.dp))

        // 📝 Register Button
        Button(
            onClick = {
                val cleanEmail = email.trim()
                val cleanPassword = password.trim()

                if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
                    message = "Email and password cannot be empty."
                } else if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    message = "Please enter a valid email address."
                } else {
                    isLoading = true
                    auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    val userMap = hashMapOf(
                                        "uid" to userId,
                                        "email" to cleanEmail,
                                        "role" to "customer",
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )
                                    db.collection("users").document(userId).set(userMap)
                                    message = "Registration successful!"
                                    onLoginSuccess() // 👈 go to HomePage after register
                                }
                            } else {
                                message = "Registration failed: ${task.exception?.message}"
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Register") }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.startsWith("Login failed") || message.startsWith("Registration failed"))
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}