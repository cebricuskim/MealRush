package com.example.mealrushapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import java.util.TimeZone
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersPage(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val orders = remember { mutableStateListOf<Pair<String, Map<String, Any>>>() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "On Delivery", "Completed")

    LaunchedEffect(selectedTab) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val tabs = listOf("pending", "On Delivery", "Completed")
            val statusFilter = tabs[selectedTab]

            FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", statusFilter)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("OrdersPage", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        orders.clear()
                        snapshot.documents.forEach { doc ->
                            doc.data?.let { data ->
                                orders.add(doc.id to data)
                            }
                        }
                    }
                }
        } else {
            Log.e("OrdersPage", "No user logged in!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFFF9800),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = Color.White) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter orders by tab
            val filteredOrders = orders.filter { order ->
                val status = order.second["status"] as? String ?: "Pending"
                when (selectedTab) {
                    0 -> status.equals("Pending", ignoreCase = true)
                    1 -> status.equals("On Delivery", ignoreCase = true)
                    2 -> status.equals("Completed", ignoreCase = true)
                    else -> false
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredOrders.isEmpty()) {
                    Text("No ${tabs[selectedTab]} orders.")
                } else {
                    filteredOrders.forEach { (orderId, order) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Order ID: $orderId", style = MaterialTheme.typography.titleMedium)
                                val recipient = (order["recipient"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                                val customerName = "${recipient["firstName"] ?: ""} ${recipient["surname"] ?: ""}".trim()
                                Text("Customer: ${customerName.ifBlank { "N/A" }}")
                                Text("Total: ₱${order["totalPrice"] ?: "N/A"}")

                                // 🔹 Show order date (date only, no time)
                                val timestamp = order["timestamp"]
                                if (timestamp is com.google.firebase.Timestamp) {
                                    val date = timestamp.toDate()
                                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    formatter.timeZone = TimeZone.getTimeZone("Asia/Manila") // 🔹 force PH timezone
                                    Text(formatter.format(date))
                                }


                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "View Full Details",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.clickable {
                                            navController.navigate("orderDetail/$orderId")
                                            navController.currentBackStackEntry?.savedStateHandle?.set("orderData", order)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailPage(navController: NavController, orderId: String, order: Map<String, Any>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Order ID: $orderId", style = MaterialTheme.typography.titleMedium)

            val recipient = (order["recipient"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val customerName = "${recipient["firstName"] ?: ""} ${recipient["surname"] ?: ""}".trim()
            val deliveryAddress = "${recipient["street"] ?: ""}, ${recipient["city"] ?: ""}".trim()
            val phone = recipient["phone"]?.toString() ?: ""

            Text("Customer: ${customerName.ifBlank { "N/A" }}")
            Text("Address: ${deliveryAddress.ifBlank { "N/A" }}")
            Text("Phone: ${phone.ifBlank { "N/A" }}")

            val status = order["status"]?.toString() ?: "N/A"
            Text("Status: $status")
            Text("Payment: ${order["paymentMethod"] ?: "N/A"}")

            // 🔹 Ordered items card
            val items = (order["items"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            var showTotals by remember { mutableStateOf(false) }

            if (items.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ordered Items", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val imageUrl = item["imageUrl"]?.toString()
                                    ?: "https://via.placeholder.com/80"

                                Image(
                                    painter = rememberAsyncImagePainter(model = imageUrl),
                                    contentDescription = item["name"]?.toString(),
                                    modifier = Modifier.size(64.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(item["name"]?.toString() ?: "Unknown",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                    Text("x${item["quantity"] ?: "0"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black
                                    )
                                    Text("₱${item["price"] ?: "0"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedVisibility(
                            visible = showTotals,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Order Subtotal:", style = MaterialTheme.typography.bodyMedium)
                                    Text("₱${order["subtotal"] ?: "0"}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Delivery Fee:", style = MaterialTheme.typography.bodyMedium)
                                    Text("₱${order["deliveryFee"] ?: "0"}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Order: ₱${order["totalPrice"] ?: "0"}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showTotals = !showTotals }) {
                                Icon(
                                    imageVector = if (showTotals) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (showTotals) "Hide details" else "Show details"
                                )
                            }
                        }
                    }
                }
            }

            val timestamp = order["timestamp"]
            if (timestamp is Timestamp) {
                val date = timestamp.toDate()
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("Asia/Manila")
                Text(formatter.format(date))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Hide Cancel button if status is "On Delivery" or "Completed"
            if (status != "On Delivery" && status != "Completed") {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance().collection("orders").document(orderId)
                            .delete()
                            .addOnSuccessListener { navController.popBackStack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Order")
                }
            }
        }
    }
}




