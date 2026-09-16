package com.example.mealrushapplication

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsPage(navController: NavController, orderId: String) {
    val firestore = FirebaseFirestore.getInstance()
    var order by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val riderUid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(orderId) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Log.d("OrderDetailsPage", "Order data: ${doc.data}")
                    order = doc.data
                } else {
                    Log.e("OrderDetailsPage", "Order $orderId not found")
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("OrderDetailsPage", "Error loading order", e)
                isLoading = false
            }
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
                            "Order Details",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            when (order?.get("status")) {
                "pending" -> {
                    Button(
                        onClick = {
                            val riderId = "rider_001"
                            val riderDetails = mapOf(
                                "firstName" to "Darren",
                                "surname" to "Uy",
                                "phone" to "09203476955",
                                "vehicle" to "Honda Click 125i"
                            )

                            val timestamp = com.google.firebase.Timestamp.now()
                            val firestore = FirebaseFirestore.getInstance()

                            firestore.collection("orders").document(orderId)
                                .update(
                                    mapOf(
                                        "status" to "On Delivery",
                                        "timestamp" to timestamp,
                                        "riderId" to riderId,
                                        "rider" to riderDetails,
                                        "remitted" to false
                                    )
                                )
                                .addOnSuccessListener {
                                    order = order?.toMutableMap()?.apply {
                                        put("status", "On Delivery")
                                        put("timestamp", timestamp)
                                        put("riderId", riderId)
                                        put("rider", riderDetails)
                                        put("remitted", false)
                                    }

                                    // Initialize chat document
                                    val chatInit = mapOf(
                                        "orderId" to orderId,
                                        "createdAt" to timestamp,
                                        "customerName" to (order?.get("recipient") as? Map<*, *>)?.get("firstName"),
                                        "riderName" to riderDetails["firstName"]
                                    )
                                    firestore.collection("chats").document(orderId).set(chatInit)

                                    // Add a system message (explicit type)
                                    val welcomeMessage = mapOf(
                                        "sender" to "system",
                                        "text" to "Chat started",
                                        "timestamp" to timestamp,
                                        "type" to "text"
                                    )
                                    firestore.collection("chats")
                                        .document(orderId)
                                        .collection("messages")
                                        .add(welcomeMessage)

                                    // Fetch order doc and send orderDetails message
                                    firestore.collection("orders").document(orderId).get()
                                        .addOnSuccessListener { doc ->
                                            if (doc.exists()) {
                                                val orderDetailsMessage = mapOf(
                                                    "sender" to riderId,
                                                    "type" to "orderDetails",
                                                    "orderId" to orderId,
                                                    "items" to doc.get("items"),
                                                    "totalPrice" to doc.getDouble("totalPrice"),
                                                    "deliveryFee" to doc.getDouble("deliveryFee"),
                                                    "timestamp" to timestamp
                                                )
                                                firestore.collection("chats")
                                                    .document(orderId)
                                                    .collection("messages")
                                                    .add(orderDetailsMessage)
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("OrderDetailsPage", "Error accepting order", e)
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Accept Order", color = Color.White, fontSize = 16.sp)
                    }
                }
                "On Delivery" -> {
                    Button(
                        onClick = {
                            firestore.collection("orders").document(orderId)
                                .update("status", "Completed")
                                .addOnSuccessListener {
                                    firestore.collection("orders").document(orderId)
                                        .get()
                                        .addOnSuccessListener { doc ->
                                            val paymentMethod = doc.getString("paymentMethod")
                                            val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                                            val riderId = "rider_001"

                                            val walletRef = firestore.collection("wallet").document(riderId)

                                            if (paymentMethod == "GCash") {
                                                // Only update wallet for GCash
                                                walletRef.get().addOnSuccessListener { walletDoc ->
                                                    if (!walletDoc.exists()) {
                                                        walletRef.set(
                                                            mapOf(
                                                                "balance" to 0.0,
                                                                "lastUpdated" to Timestamp.now()
                                                            )
                                                        )
                                                    }

                                                    walletRef.update(
                                                        "balance", FieldValue.increment(totalPrice),
                                                        "lastUpdated", Timestamp.now()
                                                    )
                                                        .addOnSuccessListener {
                                                            Log.d("WalletPage", "Wallet updated successfully")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("WalletPage", "Error updating wallet", e)
                                                        }

                                                    // Log transaction for Recent Transactions
                                                    walletRef.collection("transactions").add(
                                                        mapOf(
                                                            "type" to "GCash Payment",
                                                            "amount" to totalPrice,
                                                            "timestamp" to Timestamp.now(),
                                                            "orderId" to orderId
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    navController.popBackStack()
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Mark as Delivered", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (order == null) {
            Text("Order not found.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
            ) {
                item {
                    Text("Order ID: $orderId", style = MaterialTheme.typography.titleMedium)

                    val recipient = order?.get("recipient") as? Map<*, *>
                    Text("Recipient: ${recipient?.get("firstName") ?: ""} ${recipient?.get("surname") ?: ""}")
                    Text("Address: ${recipient?.get("street") ?: ""}, ${recipient?.get("city") ?: ""}")
                    Text("Contact: ${recipient?.get("phone") ?: "N/A"}")
                    Text("Payment: ${order?.get("paymentMethod") ?: "N/A"}")
                    Text("Status: ${order?.get("status") ?: "N/A"}")

                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            val items = order?.get("items") as? List<Map<String, Any>>
                            items?.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item["imageUrl"] ?: "",
                                        contentDescription = item["name"]?.toString() ?: "Menu Item",
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item["name"]?.toString() ?: "Unknown", fontWeight = FontWeight.Bold)
                                        Text("x${item["quantity"] ?: "0"}")
                                        Text("₱${item["price"] ?: "0.0"}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Totals
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Subtotal: ₱${order?.get("subtotal") ?: "0.0"}", fontSize = 16.sp)
                                Text("Delivery Fee: ₱${order?.get("deliveryFee") ?: "0.0"}", fontSize = 16.sp)
                                Text(
                                    "Total: ₱${order?.get("totalPrice") ?: "0.0"}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val timestamp = order?.get("timestamp") as? com.google.firebase.Timestamp
                            val formattedDate = timestamp?.toDate()?.let { date ->
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
                                sdf.format(date)
                            } ?: "N/A"

                            Text(
                                "Date: $formattedDate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Start)
                            )

                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
