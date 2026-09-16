package com.example.mealrushapplication

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DistanceUtils {
    private val client = OkHttpClient()

    suspend fun getRouteDistanceMeters(
        customerLatLng: LatLng,
        restaurantLatLngs: List<LatLng>,
        apiKey: String
    ): Int = withContext(Dispatchers.IO) {
        val restaurant = restaurantLatLngs.first()

        val url = "https://api.openrouteservice.org/v2/directions/driving-car" +
                "?api_key=$apiKey&start=${restaurant.longitude},${restaurant.latitude}" +
                "&end=${customerLatLng.longitude},${customerLatLng.latitude}"

        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                Log.d("DistanceUtils", "Raw directions response: $body")

                if (!response.isSuccessful) {
                    Log.e("DistanceUtils", "ORS request failed")
                    return@withContext haversineDistanceMeters(
                        restaurant.latitude, restaurant.longitude,
                        customerLatLng.latitude, customerLatLng.longitude
                    )
                }

                val json = JSONObject(body)
                val routes = json.optJSONArray("features") ?: return@withContext haversineDistanceMeters(
                    restaurant.latitude, restaurant.longitude,
                    customerLatLng.latitude, customerLatLng.longitude
                )

                if (routes.length() == 0) {
                    Log.w("DistanceUtils", "No routes found, using haversine fallback")
                    return@withContext haversineDistanceMeters(
                        restaurant.latitude, restaurant.longitude,
                        customerLatLng.latitude, customerLatLng.longitude
                    )
                }

                val properties = routes.getJSONObject(0).getJSONObject("properties")
                val segments = properties.getJSONArray("segments")
                val distanceValue = segments.getJSONObject(0).getDouble("distance") // meters

                Log.d("DistanceUtils", "Parsed distance: $distanceValue meters")

                // sanity check: if ORS returns something absurd, fallback
                if (distanceValue > 100_000) { // >100 km
                    Log.w("DistanceUtils", "Unrealistic distance, using haversine fallback")
                    return@withContext haversineDistanceMeters(
                        restaurant.latitude, restaurant.longitude,
                        customerLatLng.latitude, customerLatLng.longitude
                    )
                }

                distanceValue.toInt()
            }
        } catch (e: Exception) {
            Log.e("DistanceUtils", "Error fetching route distance", e)
            // fallback to straight-line distance
            haversineDistanceMeters(
                restaurant.latitude, restaurant.longitude,
                customerLatLng.latitude, customerLatLng.longitude
            )
        }
    }

    // Straight-line fallback (Haversine formula)
    private fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = R * c
        Log.d("DistanceUtils", "Haversine fallback distance: $distance meters")
        return distance.toInt()
    }

    fun calculateDeliveryFee(distanceMeters: Int): Double {
        if (distanceMeters <= 0) {
            Log.w("DistanceUtils", "Invalid address, skipping fee calculation")
            return 0.0
        }

        // sanity check: if distance is absurd, use safe default
        if (distanceMeters > 50_000) { // >50 km
            Log.w("DistanceUtils", "Unrealistic distance: $distanceMeters meters")
            return 200.0 // safe fallback fee
        }

        // Linear fee: 500m = ₱50 → ₱0.025 per meter
        val pesosPerMeter = 0.025
        val fee = distanceMeters * pesosPerMeter

        // Ensure minimum ₱50 for any valid trip
        return fee.coerceAtLeast(50.0).roundToInt().toDouble()
    }
}
