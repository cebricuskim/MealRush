package com.example.mealrushapplication

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

data class LatLng(val lat: Double, val lng: Double)

// Fetch store coordinates from Firestore
suspend fun getStores(): List<LatLng> {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("stores")
            .get()
            .await()

        snapshot.documents.mapNotNull {
            val lat = it.getDouble("lat")
            val lng = it.getDouble("lng")
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

suspend fun findNearestStore(stores: List<LatLng>, dest: LatLng): LatLng {
    var nearest = stores.firstOrNull() ?: LatLng(0.0, 0.0)
    var minDistance = Double.MAX_VALUE
    for (store in stores) {
        try {
            val km = getDistanceKm(store.lat, store.lng, dest.lat, dest.lng)
            if (km < minDistance) {
                minDistance = km
                nearest = store
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return nearest
}

// Use ORS Directions API to compute driving distance
suspend fun getDistanceKm(originLat: Double, originLng: Double, destLat: Double, destLng: Double): Double {
    return try {
        val url = "https://api.openrouteservice.org/v2/directions/driving-car"
        val body = """
            {
              "coordinates":[[$originLng,$originLat],[$destLng,$destLat]]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "YOUR_ORS_API_KEY")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "")
        val distanceMeters = json.getJSONArray("routes")
            .getJSONObject(0)
            .getJSONObject("summary")
            .getDouble("distance")

        distanceMeters / 1000.0 // convert to km
    } catch (e: Exception) {
        e.printStackTrace()
        0.0
    }
}

// Use ORS Geocoding API to resolve barangay name to coordinates
suspend fun geocodeBarangay(barangay: String): LatLng {
    return try {
        val url = "https://api.openrouteservice.org/geocode/search?api_key=YOUR_ORS_API_KEY&text=$barangay Borongan City"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "")
        val features = json.optJSONArray("features")
        if (features == null || features.length() == 0) {
            LatLng(0.0, 0.0)
        } else {
            val coords = features.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")
            LatLng(coords.getDouble(1), coords.getDouble(0))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        LatLng(0.0, 0.0)
    }
}