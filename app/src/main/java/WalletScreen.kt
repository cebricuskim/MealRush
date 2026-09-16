@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mealrushapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.Query
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Date
import java.util.Calendar
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp

fun getTodayKey(): String {
    val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
    sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") // force PH timezone
    return sdf.format(java.util.Date())
}

@Composable
fun WalletScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var earnings by remember { mutableStateOf(0.0) }   // total earned (delivery fees)
    var balance by remember { mutableStateOf(0.0) }    // wallet balance (add/withdraw)
    var completedOrders by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var transactions by remember { mutableStateOf(listOf<Map<String, Any>>()) } // wallet transactions
    var selectedTab by remember { mutableStateOf(0) }
    var remittedToday by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var currentDayKey by remember { mutableStateOf(getTodayKey()) }

    var showDialog by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var addAmountText by remember { mutableStateOf("") }

    // Fetch orders whenever the day key changes
    LaunchedEffect(currentDayKey) {
        try {
            val (startOfDay, endOfDay) = getTodayRange()

            val snapshot = db.collection("orders")
                .whereEqualTo("status", "Completed")
                .whereEqualTo("remitted", false)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap<String, Any>()
                data + ("id" to doc.id)
            }

            completedOrders = orders
            earnings = orders.sumOf { (it["deliveryFee"] as? Number)?.toDouble() ?: 0.0 }
        } catch (e: Exception) {
            completedOrders = emptyList()
            earnings = 0.0
        }
    }

    LaunchedEffect(Unit) {
        val riderId = "rider_001"

        // Listen to wallet balance
        db.collection("wallet").document(riderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("WalletPage", "Wallet listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    balance = snapshot.getDouble("balance") ?: 0.0
                    Log.d("WalletPage", "Balance updated: $balance")
                }
            }

        // Listen to recent transactions (GCash, Add Money, Withdraw)
        db.collection("wallet").document("rider_001")
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    transactions = snapshot.documents.map { doc ->
                        mapOf(
                            "type" to (doc.getString("type") ?: ""),
                            "amount" to (doc.getDouble("amount") ?: 0.0),
                            "time" to (doc.getTimestamp("timestamp")?.toDate() ?: Date())
                        )
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tabs above the card
            val tabs = listOf("Earnings", "Balance")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val shape = when (index) {
                        0 -> RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 0.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                        1 -> RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 12.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                        else -> RoundedCornerShape(0.dp)
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .background(
                                if (isSelected) Color(0xFFFF9800) else Color(0xFFEEEEEE),
                                shape = shape
                            )
                            .fillMaxHeight()
                            .weight(1f),
                        text = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Current Earnings card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Current Earnings", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("₱$earnings", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Recent Completed Orders", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn {
                        items(completedOrders) { order ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val recipient = order["recipient"] as? Map<*, *>
                                        val customerName = "${recipient?.get("firstName") ?: ""} ${recipient?.get("surname") ?: ""}"
                                        val paymentMethod = order["paymentMethod"]?.toString() ?: "Unknown"

                                        val timestamp = order["timestamp"]
                                        val formattedTime = if (timestamp is com.google.firebase.Timestamp) {
                                            val date = timestamp.toDate()
                                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
                                        } else {
                                            timestamp?.toString() ?: ""
                                        }

                                        Text(customerName, style = MaterialTheme.typography.bodyLarge)
                                        Text("Payment: $paymentMethod", style = MaterialTheme.typography.bodyMedium)
                                        Text(formattedTime, style = MaterialTheme.typography.bodySmall)
                                    }

                                    val deliveryFee = (order["deliveryFee"] as? Number)?.toDouble() ?: 0.0
                                    Text("+ ₱$deliveryFee", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF388E3C))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Current Balance card with Remit button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Current Balance", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("₱$balance", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Remit + Add Money + Withdraw buttons in one row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (balance <= earnings) {
                                    Toast.makeText(context, "Balance not enough", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                db.collection("orders")
                                    .whereEqualTo("status", "Completed")
                                    .whereEqualTo("remitted", false)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        val batch = db.batch()
                                        var totalDeduction = 0.0

                                        snapshot.documents.forEach { doc ->
                                            val paymentMethod = doc.getString("paymentMethod")
                                            val deliveryFee = doc.getDouble("deliveryFee") ?: 0.0

                                            batch.update(doc.reference, "remitted", true)

                                            // Deduct 5% of delivery fee regardless of payment method
                                            totalDeduction += (deliveryFee * 0.05)
                                        }

                                        batch.commit()
                                            .addOnSuccessListener {
                                                Log.d("WalletPage", "Remit update succeeded")

                                                // Subtract only the 5% cut from balance
                                                balance -= totalDeduction

                                                // Record the remit transaction in local state
                                                transactions = transactions + mapOf(
                                                    "type" to "Remit",
                                                    "amount" to -totalDeduction,
                                                    "time" to Date()
                                                )

                                                // ✅ Update the wallet balance in Firestore
                                                val riderId = "rider_001" // replace with actual rider ID
                                                val walletRef = db.collection("wallet").document(riderId)
                                                walletRef.update(
                                                    "balance", balance,
                                                    "lastUpdated", com.google.firebase.Timestamp.now()
                                                )

                                                // ✅ Also record the remit transaction in Firestore
                                                walletRef.collection("transactions").add(
                                                    mapOf(
                                                        "type" to "Remit",
                                                        "amount" to -totalDeduction,
                                                        "timestamp" to com.google.firebase.Timestamp.now()
                                                    )
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("WalletPage", "Error remitting orders", e)
                                            }

                                    }
                            }
                        ) {
                            Text("Remit")
                        }

                        Button(
                            onClick = { showAddDialog = true }
                        ) {
                            Text("Add Money")
                        }

                        if (showAddDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showAddDialog = false
                                    addAmountText = "" // ✅ reset field when dismissed
                                },
                                title = { Text("Add Money") },
                                text = {
                                    Column {
                                        Text("Enter amount to add:")
                                        OutlinedTextField(
                                            value = addAmountText,
                                            onValueChange = { addAmountText = it },
                                            label = { Text("Amount") }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val amount = addAmountText.toDoubleOrNull() ?: 0.0
                                        val riderId = "rider_001"
                                        val walletRef = db.collection("wallet").document(riderId)

                                        walletRef.get().addOnSuccessListener { doc ->
                                            if (!doc.exists()) {
                                                walletRef.set(
                                                    mapOf(
                                                        "balance" to 0.0,
                                                        "lastUpdated" to Timestamp.now()
                                                    )
                                                )
                                            }

                                            if (amount > 0) {
                                                walletRef.update(
                                                    "balance", FieldValue.increment(amount),
                                                    "lastUpdated", Timestamp.now()
                                                )
                                                walletRef.collection("transactions").add(
                                                    mapOf(
                                                        "type" to "Add Money",
                                                        "amount" to amount,
                                                        "timestamp" to Timestamp.now()
                                                    )
                                                )
                                                Log.d("WalletPage", "Money added successfully")
                                            } else {
                                                Log.w("WalletPage", "Invalid amount entered")
                                            }
                                        }

                                        showAddDialog = false
                                        addAmountText = ""
                                    }) {
                                        Text("Confirm")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showAddDialog = false
                                        addAmountText = ""
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = { showDialog = true }
                        ) {
                            Text("Withdraw")
                        }

                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showDialog = false
                                    amountText = ""
                                },
                                title = { Text("Withdraw Funds") },
                                text = {
                                    Column {
                                        Text("Enter amount to withdraw:")
                                        OutlinedTextField(
                                            value = amountText,
                                            onValueChange = { amountText = it },
                                            label = { Text("Amount") }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val amount = amountText.toDoubleOrNull() ?: 0.0
                                        val riderId = "rider_001" // replace with logged-in rider’s ID
                                        val walletRef = db.collection("wallet").document(riderId)

                                        walletRef.get().addOnSuccessListener { doc ->
                                            val currentBalance = doc.getDouble("balance") ?: 0.0
                                            if (currentBalance >= amount && amount > 0) {
                                                walletRef.update(
                                                    "balance", FieldValue.increment(-amount),
                                                    "lastUpdated", Timestamp.now()
                                                )
                                                walletRef.collection("transactions").add(
                                                    mapOf(
                                                        "type" to "Withdraw",
                                                        "amount" to amount,
                                                        "timestamp" to Timestamp.now()
                                                    )
                                                )
                                                Log.d("WalletPage", "Withdraw successful")
                                            } else {
                                                Log.w("WalletPage", "Insufficient balance or invalid amount")
                                            }
                                        }

                                        showDialog = false
                                        amountText = ""
                                    }) {
                                        Text("Confirm")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showDialog = false
                                        amountText = ""
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    transactions.forEach { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(tx["type"].toString(), style = MaterialTheme.typography.bodyLarge)
                                Text("₱${tx["amount"]}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF388E3C))
                                val time = tx["time"] as? Date
                                if (time != null) {
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(time),
                                        style = MaterialTheme.typography.bodySmall
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

fun getTodayRange(): Pair<com.google.firebase.Timestamp, com.google.firebase.Timestamp> {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))

    // Start of day
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = com.google.firebase.Timestamp(calendar.time)

    // End of day
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val endOfDay = com.google.firebase.Timestamp(calendar.time)

    return startOfDay to endOfDay
}
