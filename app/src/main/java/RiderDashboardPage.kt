package com.example.mealrushapplication

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import java.util.TimeZone
import android.widget.Toast
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import androidx.compose.foundation.background
import androidx.navigation.NavType
import androidx.navigation.navArgument

class RiderViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("RiderVM", "Init: current user UID = ${currentUser?.uid}")

        // Firestore listener for rider status
        firestore.collection("rider").document("rider_id")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val online = snapshot.getBoolean("isOnline") ?: true
                    Log.d(
                        "RiderVM",
                        "snapshot fired: isOnline=$online, status=${snapshot.getString("status")}"
                    )
                    if (_isOnline.value != online) {
                        _isOnline.value = online
                    }
                } else {
                    Log.w("RiderVM", "snapshot fired: no document found")
                }
            }
    }

    fun updateStatus(online: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("RiderVM", "updateStatus called: $online, Current user UID = ${currentUser?.uid}")

        _isOnline.value = online

        firestore.collection("rider").document("rider_id")
            .set(
                mapOf(
                    "isOnline" to online,
                    "status" to if (online) "Online" else "Offline"
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d("RiderVM", "Firestore updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("RiderVM", "Failed to update status", e)

                _isOnline.value = !online
            }
    }
}

@Composable
fun RiderDashboardPage() {
    val navController = rememberNavController()

    val viewModel: RiderViewModel = viewModel(LocalContext.current as ComponentActivity)
    val isOnline by viewModel.isOnline.collectAsState()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            RiderDashboard(
                navController = navController,
                isOnline = isOnline,
                onStatusChange = { newStatus -> viewModel.updateStatus(newStatus) }
            )
        }
        composable("profile") { RiderProfilePage(navController) }
        composable("rider_orders") { AvailableOrdersPage(navController, isOnline) }

        // ✅ Accepted Orders route without navArgument
        composable("accepted_orders") {
            AcceptedOrdersPage(navController, isOnline)
        }

        composable("completed_orders") { CompletedOrdersPage(navController) }
        composable("order_details/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsPage(navController, orderId)
        }

        // ✅ Local chat routes added
        composable("chat_list") { ChatListScreen(navController) }
        composable(route = "chat/{customerId}/{customerName}") { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val customerName = backStackEntry.arguments?.getString("customerName") ?: ""

            ChatScreen(
                navController = navController,
                customerId = customerId,
                customerName = customerName
            )
        }

        composable("wallet") { WalletScreen(navController) }
    }
}

@Composable
fun RiderBanner(
    riderName: String,
    earnings: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(Color(0xFFFF9800))
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_rider_scooter),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Text(riderName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("YOUR EARNINGS TODAY", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text("₱${String.format("%.2f", earnings)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun RiderDashboard(
    navController: NavController,
    isOnline: Boolean,
    onStatusChange: (Boolean) -> Unit
) {
    var riderName by remember { mutableStateOf("Darren Uy") }
    var earnings by remember { mutableStateOf(0.0) }
    var recentTransactions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var availableOrdersCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var completedListener: ListenerRegistration? by remember { mutableStateOf(null) }

    var localOnline by remember { mutableStateOf(isOnline) }

    LaunchedEffect(isOnline) {
        localOnline = isOnline
    }


    // Load rider status from Firestore when dashboard opens
    LaunchedEffect(Unit) {
        firestore.collection("rider").document("rider_id")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    localOnline = doc.getBoolean("isOnline") ?: true
                }
                isLoading = false
            }
    }



    // Listen for pending orders only when online
    LaunchedEffect(isOnline) {
        pendingListener?.remove()
        pendingListener = null

        if (isOnline) {
            pendingListener = firestore.collection("orders")
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("RiderDashboard", "Error loading pending orders", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        availableOrdersCount = snapshot.size()
                        isLoading = false
                    }
                }
        } else {
            availableOrdersCount = 0
        }

        // Persist both isOnline and status
        user?.uid?.let { uid ->
            firestore.collection("rider").document(uid)
                .set(
                    mapOf(
                        "isOnline" to isOnline,
                        "status" to if (isOnline) "Online" else "Offline"
                    ),
                    SetOptions.merge()
                )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingListener?.remove()
        }
    }


    // Always listen for completed orders (earnings + recent transactions)
    LaunchedEffect(Unit) {
        val manilaTZ = TimeZone.getTimeZone("Asia/Manila")

        val startOfDay = Calendar.getInstance(manilaTZ).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance(manilaTZ).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        completedListener = firestore.collection("orders")
            .whereEqualTo("status", "Completed")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(startOfDay))
            .whereLessThanOrEqualTo("timestamp", Timestamp(endOfDay))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RiderDashboard", "Error loading completed orders", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val docs = snapshot.documents.mapNotNull { it.data }
                    // ✅ Only today’s transactions (Manila time)
                    recentTransactions = docs.take(5)
                    // ✅ Only today’s earnings (delivery fee only)
                    earnings = docs.sumOf { (it["deliveryFee"] as? Double) ?: 0.0 }
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingListener?.remove()
            completedListener?.remove()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { navController.navigate("dashboard") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Chat") }, // ✅ changed to Chat
                    label = { Text("Chat") },
                    selected = false,
                    onClick = { navController.navigate("chat_list") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Orders") },
                    label = { Text("Orders") },
                    selected = false,
                    onClick = { navController.navigate("accepted_orders") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Completed") },
                    label = { Text("Completed") },
                    selected = false,
                    onClick = { navController.navigate("completed_orders") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { navController.navigate("profile") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            RiderBanner(riderName = riderName, earnings = earnings)

            Spacer(modifier = Modifier.height(16.dp))

            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOnline) "Status: Online · Open to any delivery"
                        else "Status: Offline · Rider is offline",
                        color = if (isOnline) Color(0xFF4CAF50) else Color.Red
                    )
                    Switch(
                        checked = localOnline,
                        onCheckedChange = { checked ->
                            localOnline = checked
                            onStatusChange(checked)
                        }
                    )
                }
            }



            Spacer(modifier = Modifier.height(12.dp))

            // Rush hour + delivery orders card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // ✅ Rush hour warning based on Manila timezone
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    if (currentHour in 16..19) {
                        Text("Rush hour, be careful.", color = Color.Red)
                    }

                    // Show text depending on online/offline
                    val text = if (!isOnline) {
                        "No Delivery Orders (Offline)"
                    } else {
                        if (availableOrdersCount == 1) {
                            "$availableOrdersCount Delivery Order Found"
                        } else {
                            "$availableOrdersCount Delivery Orders Found"
                        }
                    }

                    Text(
                        text,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }




            Spacer(modifier = Modifier.height(12.dp))

            // Recent transactions card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Transactions", fontWeight = FontWeight.Bold)

                    if (recentTransactions.isEmpty()) {
                        Text("No completed deliveries yet.")
                    } else {
                        recentTransactions
                            .filter { (it["status"] as? String).equals("Completed", ignoreCase = true) }
                            .sortedByDescending { it["completedAt"] as? Long ?: 0L }
                            .take(2)
                            .forEach { tx ->
                                val recipient = tx["recipient"] as? Map<*, *>
                                val name = "${recipient?.get("firstName") ?: ""} ${recipient?.get("surname") ?: ""}"
                                val total = tx["totalPrice"] ?: "0.0"
                                val status = tx["status"] ?: "N/A"

                                Text("Name: $name")
                                Text("Total: ₱$total")
                                Text("Status: $status")
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                    }
                }
            }



            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = {
                        firestore.collection("orders")
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener { result ->
                                if (result.isEmpty) {
                                    Toast.makeText(
                                        navController.context,
                                        "No Available Order",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    for (doc in result.documents) {
                                        firestore.collection("orders")
                                            .document(doc.id)
                                            .update("status", "On Delivery") // ✅ change to On Delivery
                                    }
                                    navController.navigate("accepted_orders")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("RiderDashboard", "Error accepting all orders", e)
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Confirmed Delivery", color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { navController.navigate("rider_orders") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Designated Delivery", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
