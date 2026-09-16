package com.example.mealrushapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MenuGrid(
    menuItems: List<MenuItem>,
    cart: MutableList<MenuItem>,
    db: FirebaseFirestore,
    onItemClick: (MenuItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(menuItems) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = item.imageUrl.ifEmpty { "https://via.placeholder.com/100" }),
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(item.code, style = MaterialTheme.typography.bodySmall)
                    Text(item.name, style = MaterialTheme.typography.titleMedium)

                    Text(
                        text = item.description.ifEmpty { "No description available" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text("₱${item.price}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                // Add to cart with lat/lng handled in MenuPage
                                onItemClick(item)

                                // Save order item to Firestore (optional)
                                val orderItemData = hashMapOf(
                                    "item_id" to item.code,
                                    "name" to item.name,
                                    "description" to item.description,
                                    "price" to item.price,
                                    "restaurantName" to item.restaurantName,
                                    "quantity" to 1,
                                    "subtotal" to item.price
                                )

                                db.collection("order_items")
                                    .add(orderItemData)
                                    .addOnSuccessListener { docRef ->
                                        println("Order item saved with id: ${docRef.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Error adding item: ${e.message}")
                                    }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Add Order")
                        }
                    }
                }
            }
        }
    }
}