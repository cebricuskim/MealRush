package com.example.mealrushapplication

data class MenuItem(
    val firestoreId: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val restaurantName: String = "",
    val imageUrl: String = "",
    val quantity: Int = 0,
    val restaurantLat: Double = 0.0,
    val restaurantLng: Double = 0.0,
    val restaurantId: String = ""   // 🔑 Firestore doc ID for the restaurant
)