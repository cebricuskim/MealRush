package com.example.mealrushapplication

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri

data class CustomerChatPreview(
    val userId: String,
    val riderId: String,
    val name: String,
    val lastMessage: String,
    val time: Long,
    val orderId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerChatListScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: ""   // ✅ automatically get logged-in user ID

    var chatList by remember { mutableStateOf<List<CustomerChatPreview>>(emptyList()) }
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }

    val activity = LocalContext.current as Activity
    val statusBarColor = MaterialTheme.colorScheme.primary.toArgb()
    LaunchedEffect(statusBarColor) {
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = statusBarColor
    }

    // ✅ Listen for orders belonging to this user with status On Delivery or Completed
    LaunchedEffect(currentUserId) {
        listener?.remove()
        if (currentUserId.isNotBlank()) {
            listener = firestore.collection("orders")
                .whereEqualTo("userId", currentUserId) // isolate per user
                .whereIn("status", listOf("On Delivery", "Completed"))
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val grouped = mutableMapOf<String, CustomerChatPreview>()

                        snapshot.documents.forEach { doc ->
                            val riderId = doc.getString("riderId") ?: return@forEach
                            val rider = doc.get("rider") as? Map<*, *>
                            val firstName = rider?.get("firstName") as? String ?: ""
                            val surname = rider?.get("surname") as? String ?: ""
                            val name = "$firstName $surname".trim()

                            val userId = doc.getString("userId") ?: ""
                            val orderId = doc.id
                            val orderTimestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L

                            var lastMessage = "(no message yet)"
                            var lastMessageTime = orderTimestamp

                            // Fetch latest message from chats/{userId}/messages
                            firestore.collection("chats")
                                .document(userId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { messageSnap ->
                                    val msgDoc = messageSnap.documents.firstOrNull()
                                    if (msgDoc != null) {
                                        lastMessage = msgDoc.getString("text") ?: lastMessage
                                        lastMessageTime = msgDoc.getTimestamp("timestamp")?.toDate()?.time ?: lastMessageTime
                                    }

                                    val preview = CustomerChatPreview(
                                        userId = userId,
                                        riderId = riderId,
                                        name = name,
                                        lastMessage = lastMessage,
                                        time = lastMessageTime,
                                        orderId = orderId
                                    )

                                    grouped[userId] = preview

                                    // Update chatList after fetching message
                                    chatList = grouped.values.sortedByDescending { it.time }
                                }
                        }
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose { listener?.remove() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chats") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (chatList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No orders yet")
                    }
                }
            } else {
                items(chatList) { chat ->
                    CustomerChatListItem(chat) {
                        if (chat.userId.isNotBlank()) {
                            val safeName = Uri.encode(chat.name.ifBlank { "Unknown Rider" })
                            navController.navigate("chat/${chat.userId}/$safeName")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerChatListItem(chat: CustomerChatPreview, onClick: () -> Unit) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val formattedTime = formatter.format(Date(chat.time))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.name, style = MaterialTheme.typography.bodyLarge)
            Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        Text(formattedTime, style = MaterialTheme.typography.bodySmall)
    }
}
