@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mealrushapplication

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

@Composable
fun RiderProfilePage(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    var riderName by remember { mutableStateOf("") }
    var riderNumber by remember { mutableStateOf("") }
    var riderLocation by remember { mutableStateOf("") }
    var motorModel by remember { mutableStateOf("") }
    var riderSex by remember { mutableStateOf("") }

    var isEditingNumber by remember { mutableStateOf(false) }

    // Fetch latest number from Firestore whenever page loads
    LaunchedEffect(Unit) {
        db.collection("rider").document("rider_id")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    riderName = doc.getString("name") ?: ""
                    riderNumber = doc.getString("Phone_Number") ?: ""
                    riderLocation = "Borongan City, Eastern Samar"
                    motorModel = doc.getString("Vehicle_Type") ?: ""
                    riderSex = "Male"
                }
            }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.5f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        "Menu",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }

                Divider()

                OutlinedButton(
                    onClick = { navController.navigate("profile") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Profile", color = Color(0xFFFF9800))
                }

                OutlinedButton(
                    onClick = { navController.navigate("wallet") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Filled.Wallet, contentDescription = "Wallet", tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wallet", color = Color(0xFFFF9800))
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val context = navController.context
                        val prefs = context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Sign Out", tint = Color.Red)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", color = Color.Red)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color(0xFFFF9800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, bottom = 12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Profile", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileItem("Name", riderName)

                        // Editable Number with edit button beside it
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Number", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (isEditingNumber) {
                                    TextField(
                                        value = riderNumber,
                                        onValueChange = { riderNumber = it },
                                        singleLine = true
                                    )
                                } else {
                                    Text(riderNumber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = {
                                if (isEditingNumber) {
                                    // ✅ Update Firestore and refresh state
                                    db.collection("rider").document("rider_id")
                                        .update("Phone_Number", riderNumber)
                                        .addOnSuccessListener {
                                            db.collection("rider").document("rider_id")
                                                .get()
                                                .addOnSuccessListener { doc ->
                                                    riderNumber = doc.getString("Phone_Number") ?: riderNumber
                                                }
                                        }
                                }
                                isEditingNumber = !isEditingNumber
                            }) {
                                Icon(
                                    imageVector = if (isEditingNumber) Icons.Filled.Check else Icons.Filled.Edit,
                                    contentDescription = "Edit Number"
                                )
                            }
                        }

                        ProfileItem("Location", riderLocation)
                        ProfileItem("Plate Number", motorModel)
                        ProfileItem("Sex", riderSex)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
