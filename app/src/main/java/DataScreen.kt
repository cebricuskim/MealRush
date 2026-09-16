package com.example.mealrushapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    val prefs = context.getSharedPreferences("MealRushPrefs", Context.MODE_PRIVATE)
    val restaurantName = prefs.getString("restaurantName", "Restaurant") ?: "Restaurant"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restaurant Information") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Restaurant Name:", style = MaterialTheme.typography.titleMedium)
            Text(restaurantName, style = MaterialTheme.typography.bodyLarge)

            Divider()

            Text("Contract:", style = MaterialTheme.typography.titleMedium)
            Text("Start Date: May 5, 2026", style = MaterialTheme.typography.bodyLarge)
            Text("Expiration Date: May 5, 2028", style = MaterialTheme.typography.bodyLarge)

            // ✅ Clickable link to contract image
            Text(
                text = "View Contract Paper",
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://drive.google.com/file/d/1N0T3Cwmo2n4CTZ6b7EdxLJAGdxj1-djg/view?usp=sharing")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}
