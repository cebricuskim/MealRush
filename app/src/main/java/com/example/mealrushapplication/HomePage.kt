package com.example.mealrushapplication

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.material3.BottomAppBar
import androidx.compose.material.icons.filled.Chat
import com.google.firebase.firestore.GeoPoint

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val location: String = "",
    val rating: Int = 0,
    val geoPoint: GeoPoint? = null
)
@Composable
fun brandColor(name: String): Color {
    return when (name) {
        "Bo's Coffee" -> Color(0xFF6D4C41) // deep coffee brown
        "Mang Inasal" -> Color(0xFF2E7D32) // green
        "McDonald's" -> Color(0xFFD32F2F) // red
        "Jollibee" -> Color(0xFFE53935) // bright red
        else -> MaterialTheme.colorScheme.surface
    }
}

@Composable
fun HomePage(
    navController: NavController,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onRestaurantClick: (Restaurant) -> Unit
) {
    val context = LocalContext.current

    var restaurants by remember { mutableStateOf(listOf<Restaurant>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("restaurant")
            .get()
            .addOnSuccessListener { result ->
                restaurants = result.documents.mapNotNull { doc ->
                    doc.toObject(Restaurant::class.java)?.copy(id = doc.id)
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = "Failed to load restaurants: ${e.message}"
                isLoading = false
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(240.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xFFFF9800)) // orange background
                    ) {
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White, // white text
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 8.dp)
                        )
                    }

                    Divider(color = Color.LightGray)

                    // Centered Profile, Orders, Chats below header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile
                        Row(
                            modifier = Modifier
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("profile")
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROFILE", color = Color(0xFFFF9800))
                        }
                        Divider(color = Color.LightGray)

                        // Orders
                        Row(
                            modifier = Modifier
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("orders")
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Orders", tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ORDERS", color = Color(0xFFFF9800))
                        }
                        Divider(color = Color.LightGray)

                        // Chats
                        Row(
                            modifier = Modifier
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("chats")
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "Chats", tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHATS", color = Color(0xFFFF9800))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Divider(color = Color.LightGray)

                    // Bottom bar with Sign Out
                    BottomAppBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    showSignOutDialog = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGN OUT", color = Color.Red) // red text
                        }
                    }
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restaurants",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )

                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "User Options", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> CircularProgressIndicator()
                errorMessage.isNotEmpty() -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(restaurants.size) { index ->
                            val restaurant = restaurants[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val safeName = Uri.encode(restaurant.name)
                                        navController.navigate("menu/${restaurant.id}/$safeName")
                                    },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = brandColor(restaurant.name)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = restaurant.imageUrl),
                                        contentDescription = restaurant.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                    Text(restaurant.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text(restaurant.category, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    Text(restaurant.location, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    Text("Rating: ${restaurant.rating}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for sign out
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
                            // Sign out from FirebaseAuth
                            FirebaseAuth.getInstance().signOut()

                            // Sign out from Google
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut()

                            // ✅ Clear ALL local preferences, not just staySignedIn
                            val prefs = context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()

                            // Navigate back to Auth screen
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