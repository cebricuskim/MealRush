package com.example.mealrushapplication

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedOrdersPage(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var orders by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }

    // Live listener for completed orders, sorted newest first
    LaunchedEffect(Unit) {
        listener = firestore.collection("orders")
            .whereEqualTo("status", "Completed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CompletedOrdersPage", "Listen failed.", e)
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    orders = snapshot.documents.map { doc ->
                        doc.id to (doc.data ?: emptyMap())
                    }
                }
                isLoading = false
            }
    }

    DisposableEffect(Unit) {
        onDispose { listener?.remove() }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFFFF9800),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, bottom = 12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Completed Orders",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                orders.isEmpty() -> Text("No Completed Orders", modifier = Modifier.padding(16.dp))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(orders) { (orderId, order) ->
                        val recipient = order["recipient"] as? Map<*, *>
                        val timestamp = order["timestamp"] as? Timestamp
                        val dateString = timestamp?.toDate()?.let { date ->
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
                            sdf.format(date)
                        } ?: "N/A"


                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { navController.navigate("order_details/$orderId") },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Order ID: $orderId", style = MaterialTheme.typography.titleMedium)

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Recipient:", fontWeight = FontWeight.Bold)
                                Text("Name: ${recipient?.get("firstName") ?: ""} ${recipient?.get("surname") ?: ""}")
                                Text("Address: ${recipient?.get("street") ?: ""}, ${recipient?.get("city") ?: ""}")
                                Text(
                                    "Status: ${(order["status"]?.toString() ?: "N/A").replaceFirstChar { it.uppercase() }}",
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        "Total: ₱${order["totalPrice"] ?: "0.0"}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "Date: $dateString",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
