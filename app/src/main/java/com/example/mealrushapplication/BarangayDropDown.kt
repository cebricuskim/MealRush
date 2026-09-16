package com.example.mealrushapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.heightIn

@Composable
fun BarangayDropdown(
    barangays: List<String>,
    selectedBarangay: String,
    onBarangaySelected: (String) -> Unit
) {
    var query by remember { mutableStateOf(selectedBarangay) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedBarangay) {
        query = selectedBarangay
    }

    val filteredBarangays = barangays.filter {
        it.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text("Street / Barangay") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true
        )

        if (expanded) {
            Popup(
                alignment = androidx.compose.ui.Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false)
            ) {
                Surface(
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 56.dp)
                        .heightIn(max = 300.dp)
                ) {
                    LazyColumn {
                        if (filteredBarangays.isEmpty()) {
                            item {
                                Text("No barangay found", modifier = Modifier.padding(8.dp))
                            }
                        } else {
                            items(filteredBarangays) { barangay ->
                                Text(
                                    text = barangay,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable {
                                            val cleanBarangay = barangay.trim()
                                            query = cleanBarangay
                                            onBarangaySelected(cleanBarangay)
                                            expanded = false
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

