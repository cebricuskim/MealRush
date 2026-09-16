package com.example.mealrushapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MenuPage(
    restaurantId: String,
    restaurantName: String,
    cart: MutableList<MenuItem>,
    onCheckout: () -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    var menuItems by remember { mutableStateOf(listOf<MenuItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Fetch menu items from Firestore
    LaunchedEffect(restaurantId) {
        try {
            val result = db.collection("menu_item")
                .whereEqualTo("restaurantName", restaurantName)
                .get()
                .await()

            menuItems = result.documents.mapNotNull { doc ->
                MenuItem(
                    firestoreId = doc.id,
                    code = doc.getString("code") ?: "",
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    restaurantName = restaurantName,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    quantity = 1,
                    restaurantLat = 0.0,
                    restaurantLng = 0.0,
                    restaurantId = restaurantId
                )
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load menu: ${e.message}"
            isLoading = false
        }
    }

    // Add-to-cart logic with GeoPoint
    fun addToCart(menuItem: MenuItem) {
        scope.launch {
            val restaurantDoc = db.collection("restaurant")
                .document(menuItem.restaurantId)
                .get()
                .await()

            val geoPoint = restaurantDoc.getGeoPoint("geoPoint")

            val existingIndex = cart.indexOfFirst { it.firestoreId == menuItem.firestoreId }
            if (existingIndex != -1) {
                val existingItem = cart[existingIndex]
                cart[existingIndex] = existingItem.copy(
                    quantity = existingItem.quantity + 1,
                    restaurantLat = geoPoint?.latitude ?: 0.0,
                    restaurantLng = geoPoint?.longitude ?: 0.0
                )
            } else {
                cart.add(menuItem.copy(
                    quantity = 1,
                    restaurantLat = geoPoint?.latitude ?: 0.0,
                    restaurantLng = geoPoint?.longitude ?: 0.0
                ))
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
                    Text(
                        text = "$restaurantName Menu",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (cart.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF9800))
                        .navigationBarsPadding()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Items: ${cart.sumOf { it.quantity }}",
                            color = Color.White
                        )
                        Button(
                            onClick = { onCheckout() },
                            modifier = Modifier
                                .height(52.dp)
                                .width(150.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Add to Cart", color = Color.Black)
                        }
                    }
                }
            }
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> CircularProgressIndicator()
                errorMessage.isNotEmpty() -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
                else -> {
                    MenuGrid(
                        menuItems = menuItems,
                        cart = cart,
                        db = db,
                        onItemClick = { item: MenuItem -> addToCart(item) }
                    )
                }
            }
        }
    }
}