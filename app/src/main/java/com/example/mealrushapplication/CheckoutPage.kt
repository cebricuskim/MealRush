package com.example.mealrushapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutPage(
    cartItems: MutableList<MenuItem>,
    onCheckout: (List<MenuItem>, Double) -> Unit,
    onBack: () -> Unit
) {
    val cartState = cartItems
    val subtotal by remember { derivedStateOf { cartState.sumOf { it.price * it.quantity } } }

    val orangeButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFFF9800),
        contentColor = Color.White,
        disabledContainerColor = Color(0xFFFFCC80),
        disabledContentColor = Color.White
    )

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
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cart",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFB74D))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Subtotal: ₱$subtotal", style = MaterialTheme.typography.titleMedium, color = Color.Black)

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onCheckout(cartState, subtotal) }, // ✅ matches MainActivity
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Checkout", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(cartState) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        if (item.imageUrl.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = item.imageUrl),
                                contentDescription = item.name,
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(end = 12.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text("₱${item.price}", style = MaterialTheme.typography.bodyMedium)
                            if (item.description.isNotEmpty()) {
                                Text(item.description, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row {
                                    Button(
                                        onClick = {
                                            val index = cartState.indexOf(item)
                                            if (index != -1 && cartState[index].quantity > 1) {
                                                cartState[index] = cartState[index].copy(
                                                    quantity = cartState[index].quantity - 1
                                                )
                                            }
                                        },
                                        enabled = item.quantity > 1,
                                        colors = orangeButtonColors
                                    ) { Text("-") }

                                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))

                                    Button(
                                        onClick = {
                                            val index = cartState.indexOf(item)
                                            if (index != -1) {
                                                cartState[index] = cartState[index].copy(
                                                    quantity = cartState[index].quantity + 1
                                                )
                                            }
                                        },
                                        colors = orangeButtonColors
                                    ) { Text("+") }
                                }

                                Button(
                                    onClick = {
                                        cartState.remove(item)
                                        cartItems.remove(item)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
