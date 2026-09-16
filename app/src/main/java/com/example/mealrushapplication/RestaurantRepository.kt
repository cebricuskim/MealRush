package com.example.mealrushapplication

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

object RestaurantRepository {
    // Fetch restaurant coordinates from Firestore
    suspend fun getRestaurantCoordinates(restaurantId: String): Pair<Double, Double>? {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("restaurant").document(restaurantId).get().await()
        val geoPoint: GeoPoint? = doc.getGeoPoint("geoPoint")
        return geoPoint?.let { Pair(it.latitude, it.longitude) }
    }

    // Fetch menu items and attach coordinates
    suspend fun getMenuForRestaurant(restaurantId: String): List<MenuItem> {
        val coords = getRestaurantCoordinates(restaurantId) ?: Pair(0.0, 0.0)
        val (lat, lng) = coords

        return listOf(
            MenuItem(
                firestoreId = "yum1",
                code = "YUM1",
                name = "Yumburger",
                description = "Classic Jollibee burger",
                price = 50.0,
                restaurantName = "Jollibee",
                imageUrl = "https://example.com/yumburger.png",
                quantity = 0,
                restaurantLat = lat,
                restaurantLng = lng
            )
        )
    }
}