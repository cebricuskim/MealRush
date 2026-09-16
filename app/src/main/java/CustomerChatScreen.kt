package com.example.mealrushapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class CustomerMessage(val sender: String, val text: String, val time: String, val isMe: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerChatScreen(
    userId: String,
    riderName: String,
    navController: NavController
) {
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<CustomerMessage>>(emptyList()) }
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }
    var orderStatus by remember { mutableStateOf("") }

    val firestore = FirebaseFirestore.getInstance()

    // Listen for messages (persistent chat per customer)
    LaunchedEffect(userId) {
        listener?.remove()
        listener = firestore.collection("chats")
            .document(userId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        val sender = doc.getString("sender") ?: "Unknown"
                        val text = doc.getString("text") ?: ""
                        val time = doc.getTimestamp("timestamp")?.toDate()?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                        } ?: ""
                        val isMe = sender == "customer"
                        CustomerMessage(sender, text, time, isMe)
                    }
                }
            }
    }

    // Listen for latest order status for this customer
    LaunchedEffect(userId) {
        firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                orderStatus = snapshot?.documents?.firstOrNull()?.getString("status") ?: ""
            }
    }

    DisposableEffect(Unit) {
        onDispose { listener?.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with $riderName", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (orderStatus == "Completed") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Order Completed", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* attach */ }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach")
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type message") }
                    )
                    IconButton(onClick = { /* mic */ }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice Note")
                    }
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            val timestamp = com.google.firebase.Timestamp.now()
                            val message = mapOf(
                                "sender" to "customer",
                                "text" to inputText,
                                "timestamp" to timestamp
                            )

                            firestore.collection("chats")
                                .document(userId)
                                .collection("messages")
                                .add(message)

                            // 🔹 Update lastMessage in latest order
                            firestore.collection("orders")
                                .whereEqualTo("userId", userId)
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { snap ->
                                    val latestOrder = snap.documents.firstOrNull()
                                    latestOrder?.reference?.update(
                                        mapOf(
                                            "lastMessage" to inputText,
                                            "timestamp" to timestamp
                                        )
                                    )
                                }
                            inputText = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                CustomerChatBubble(msg)
            }
        }
    }
}

@Composable
fun CustomerChatBubble(message: CustomerMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isMe) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isMe) Color.White else Color.Black
                )
            }
        }
        Text(
            message.time,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            textAlign = if (message.isMe) TextAlign.End else TextAlign.Start
        )
    }
}
