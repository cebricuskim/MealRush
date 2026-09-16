package com.example.mealrushapplication

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.android.gms.maps.model.LatLng

object FirestoreUtils {
    private val db = FirebaseFirestore.getInstance()

    fun saveCustomerLocation(customerId: String, latLng: LatLng) {
        val geoPoint = GeoPoint(latLng.latitude, latLng.longitude)
        db.collection("users").document(customerId).update("geoPoint", geoPoint)
    }
}
