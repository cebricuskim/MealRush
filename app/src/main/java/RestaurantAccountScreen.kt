package com.example.mealrushapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantAccountScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var menuItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }

    var showDialog by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<String?>(null) }
    var nameField by remember { mutableStateOf("") }
    var priceField by remember { mutableStateOf("") }
    var descriptionField by remember { mutableStateOf("") }
    var imageUrlField by remember { mutableStateOf("") }

    val prefs = context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
    val restaurantName = prefs.getString("restaurantName", "Restaurant") ?: "Restaurant"

    LaunchedEffect(restaurantName) {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("menu_item")
            .whereEqualTo("restaurantName", restaurantName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    menuItems = snapshot.documents.map { doc ->
                        doc.data?.plus("id" to doc.id) ?: emptyMap()
                    }
                }
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Limit drawer width to half of the screen
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f) // ✅ half of the screen width
            ) {
                Text("Menu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    label = { Text("Information") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("data_screen") // replace with your route
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = {
                        // Firebase sign out
                        FirebaseAuth.getInstance().signOut()

                        // Clear local prefs
                        val prefs = context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()

                        // Close drawer
                        scope.launch { drawerState.close() }

                        // Navigate back to Auth screen and clear back stack
                        navController.navigate("auth") {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )

            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Manage Menu - $restaurantName") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editingItemId = null
                    nameField = ""
                    priceField = ""
                    descriptionField = ""
                    imageUrlField = ""
                    showDialog = true
                }) {
                    Text("+")
                }
            }
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = item["imageUrl"].toString(),
                                contentDescription = item["description"].toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item["name"].toString(), style = MaterialTheme.typography.titleMedium)
                            Text("₱${item["price"]}", style = MaterialTheme.typography.bodyMedium)

                            // ✅ Description with ellipsis
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(15.dp) // reserve space for ~2 lines
                            ) {
                                Text(
                                    text = item["description"].toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = {
                                    editingItemId = item["id"]?.toString()
                                    nameField = item["name"].toString()
                                    priceField = item["price"].toString()
                                    descriptionField = item["description"].toString()
                                    imageUrlField = item["imageUrl"].toString()
                                    showDialog = true
                                }) { Text("Edit") }
                                TextButton(onClick = {
                                    val id = item["id"]?.toString()
                                    if (id != null) {
                                        db.collection("menu_item").document(id).delete()
                                    }
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }

                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingItemId == null) "Add Menu Item" else "Edit Menu Item") },
            text = {
                Column {
                    OutlinedTextField(value = nameField, onValueChange = { nameField = it }, label = { Text("Name (String)") })
                    OutlinedTextField(value = priceField, onValueChange = { priceField = it }, label = { Text("Price (Double)") })
                    OutlinedTextField(value = descriptionField, onValueChange = { descriptionField = it }, label = { Text("Description") })
                    OutlinedTextField(value = imageUrlField, onValueChange = { imageUrlField = it }, label = { Text("Image URL") })
                    OutlinedTextField(value = restaurantName, onValueChange = {}, label = { Text("Restaurant Name") }, enabled = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val priceValue = priceField.toDoubleOrNull() ?: 0.0

                    val itemData = hashMapOf(
                        "name" to nameField.ifBlank { "" },
                        "price" to priceValue,
                        "description" to descriptionField.ifBlank { "" },
                        "restaurantName" to restaurantName,
                        "imageUrl" to imageUrlField.ifBlank { "" }
                    )

                    if (editingItemId == null) {
                        db.collection("menu_item")
                            .whereEqualTo("restaurantName", restaurantName)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val existingIds = snapshot.documents.map { it.id }
                                val prefix = restaurantName.lowercase().replace(" ", "")
                                val nextIndex = existingIds
                                    .mapNotNull {
                                        val regex = Regex("${prefix}(\\d+)_id")
                                        regex.find(it)?.groupValues?.get(1)?.toIntOrNull()
                                    }
                                    .maxOrNull()?.plus(1) ?: 1
                                val customId = "${prefix}${nextIndex}_id"
                                db.collection("menu_item").document(customId).set(itemData)
                            }
                    } else {
                        db.collection("menu_item").document(editingItemId!!).set(itemData)
                    }
                    showDialog = false
                }) {
                    Text(if (editingItemId == null) "Add" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
