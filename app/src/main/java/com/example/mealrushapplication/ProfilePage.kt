package com.example.mealrushapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    navController: NavController
) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    val recipients = remember { mutableStateListOf<Map<String, String>>() }
    val locations = remember { mutableStateListOf<Map<String, String>>() }

    var showSignOutDialog by remember { mutableStateOf(false) }

    // Load profile data safely
    LaunchedEffect(user) {
        user?.let {
            db.collection("user").document(it.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        firstName = doc.getString("firstName") ?: ""
                        surname = doc.getString("surname") ?: ""
                        phone = doc.getString("phone") ?: ""
                        street = doc.getString("street") ?: ""
                        city = doc.getString("city") ?: ""

                        val recs = doc.get("recipients") as? List<Map<String, Any>>
                        recs?.let { saved ->
                            recipients.clear()
                            recipients.addAll(saved.map { r ->
                                mapOf(
                                    "firstName" to (r["firstName"]?.toString() ?: ""),
                                    "surname" to (r["surname"]?.toString() ?: ""),
                                    "phone" to (r["phone"]?.toString() ?: ""),
                                    "street" to (r["street"]?.toString() ?: ""),
                                    "city" to (r["city"]?.toString() ?: "")
                                )
                            })
                        }

                        val locationDocs = doc.get("locations") as? List<Map<String, Any>>
                        locationDocs?.let { saved ->
                            locations.clear()
                            locations.addAll(saved.map { l ->
                                mapOf(
                                    "street" to (l["street"]?.toString() ?: ""),
                                    "city" to (l["city"]?.toString() ?: "")
                                )
                            })
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF9800),
                                    Color(0xFFFFB74D)
                                )
                            )
                        )
                ) {
                    // Back button + title side by side at bottom-left
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }

                    // Sign Out action at bottom-right
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSignOutDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", color = Color.White)
                        }
                    }
                }
            }
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //Default info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${firstName.ifBlank { "N/A" }} ${surname.ifBlank { "N/A" }}")
                    Text("Email: ${email.ifBlank { "N/A" }}")
                    Text("Phone: ${phone.ifBlank { "N/A" }}")
                    Text("Default Location: ${street.ifBlank { "N/A" }}, ${city.ifBlank { "N/A" }}")
                }
            }

            // Additional recipients
            if (recipients.isNotEmpty()) {
                Text("Additional Recipients:", style = MaterialTheme.typography.titleMedium)
                recipients.forEach { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Name: ${rec["firstName"]} ${rec["surname"]}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Phone: ${rec["phone"]}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Location: ${rec["street"]}, ${rec["city"]}", style = MaterialTheme.typography.bodyLarge)
                                }

                                IconButton(
                                    onClick = {
                                        user?.let {
                                            db.collection("user").document(it.uid)
                                                .update("recipients", FieldValue.arrayRemove(rec))
                                            recipients.remove(rec)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove recipient",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                }
            }

            // Additional locations
            if (locations.isNotEmpty()) {
                Text("Additional Locations:", style = MaterialTheme.typography.titleMedium)
                locations.forEach { loc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Name: ${firstName.ifBlank { "N/A" }} ${surname.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyLarge)
                            Text("Phone: ${phone.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyLarge)
                            Text("Location: ${loc["street"]}, ${loc["city"]}", style = MaterialTheme.typography.bodyLarge)

                            Button(
                                onClick = {
                                    user?.let {
                                        db.collection("user").document(it.uid)
                                            .update("locations", FieldValue.arrayRemove(loc))
                                        locations.remove(loc)
                                    }
                                }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for sign out with pill-shaped buttons
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Confirm Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showSignOutDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            //Sign out from Firebase
                            FirebaseAuth.getInstance().signOut()

                            //Sign out from Google account
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut()

                            //Clear local preference
                            val prefs = context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                            prefs.edit { putBoolean("staySignedIn", false) }

                            //Navigate back to login
                            navController.navigate("auth") {
                                popUpTo("home") { inclusive = true }
                            }
                            showSignOutDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 8.dp)
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        )
    }
}