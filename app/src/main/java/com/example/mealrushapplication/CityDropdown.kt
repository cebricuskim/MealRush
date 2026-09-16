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

@Composable
fun CityDropdown(
    cities: List<String>,
    selectedCity: String,
    onCitySelected: (String) -> Unit
) {
    var query by remember { mutableStateOf(selectedCity) }
    var expanded by remember { mutableStateOf(false) }

    // Keep query in sync with selectedCity
    LaunchedEffect(selectedCity) {
        query = selectedCity
    }

    val filteredCities = cities.filter {
        it.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text("City / Province") },
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
                        .padding(horizontal = 16.dp, vertical = 56.dp) // offset so it appears below the field
                ) {
                    LazyColumn {
                        if (filteredCities.isEmpty()) {
                            item {
                                Text("No city found", modifier = Modifier.padding(8.dp))
                            }
                        } else {
                            items(filteredCities) { city ->
                                Text(
                                    text = city,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable {
                                            val cleanCity = city.trim().lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                            query = cleanCity
                                            onCitySelected(cleanCity)
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
