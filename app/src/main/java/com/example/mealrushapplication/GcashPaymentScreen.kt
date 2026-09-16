package com.example.mealrushapplication

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color   // ✅ use Compose Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun GCashPaymentScreen(orderId: String, totalAmount: Double, navController: NavController) {
    val context = navController.context
    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    var showMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Scan to Pay with GCash", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        val qrContent = "gcash://pay?orderId=$orderId&amount=$totalAmount"
        val qrBitmap = remember { generateQrCode(qrContent) }

        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "GCash QR",
            modifier = Modifier.size(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(qrContent)
            }
            context.startActivity(intent)
        }) {
            Text("Open GCash App")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Done button: update order and show success message
        Button(onClick = {
            if (user == null) {
                return@Button
            }

            val orderData = mapOf(
                "status" to "pending", // 👈 match OrdersViewModel filter
                "paymentMethod" to "GCash",
                "timestamp" to FieldValue.serverTimestamp(),
                "userId" to user.uid
            )

            firestore.collection("orders").document(orderId).update(orderData)
                .addOnSuccessListener {
                    showMessage = true
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("GCashPayment", "Error updating order", e)
                }
        }) {
            Text("Done")
        }
    }

    // ✅ Success message overlay
    if (showMessage) {
        LaunchedEffect(Unit) {
            delay(2000)
            showMessage = false
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.padding(32.dp).size(width = 260.dp, height = 200.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFFFF9800),   // ✅ Compose Color
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Order Placed",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Helper function to generate QR bitmap
fun generateQrCode(content: String): Bitmap {
    val size = 512
    val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bmp
}
