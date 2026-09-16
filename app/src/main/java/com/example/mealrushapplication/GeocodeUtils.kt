package com.example.mealrushapplication

import android.util.Log
import com.google.android.gms.maps.model.LatLng

object GeocodeUtils {
    // Hardcoded barangay coordinates (approximate)
    private val barangayCoords = mapOf(
        "alang-alang" to LatLng(11.601126, 125.435665),
        "amantacop" to LatLng(11.695271, 125.393218),
        "balacdas" to LatLng(11.690595, 125.433515),
        "balud 1" to LatLng(11.6085, 125.4320),
        "balud 2" to LatLng(11.6100, 125.4330),
        "bato" to LatLng(11.5991, 125.4398),
        "bayobay" to LatLng(11.646484, 125.437454),
        "bugas" to LatLng(11.702741, 125.467797),
        "cabong" to LatLng(11.586966, 125.452003),
        "cagbonga" to LatLng(11.652076, 125.423058),
        "calico-an" to LatLng(11.595727, 125.404378),
        "calingatngan" to LatLng(11.623226, 125.416816),
        "camada" to LatLng(11.541795, 125.457929),
        "campesao" to LatLng(11.611962, 125.418959),
        "can-abong" to LatLng(11.587271, 125.440425),
        "canjaway" to LatLng(11.693817, 125.475742),
        "canlaray" to LatLng(11.631795, 125.450943),
        "hindang" to LatLng(11.637885, 125.442991),
        "lalawigan" to LatLng(11.584663, 125.469415),
        "libuton" to LatLng(11.647743, 125.450776),
        "locso-on" to LatLng(11.568142, 125.460417),
        "maybacong" to LatLng(11.639125, 125.452837),
        "maypangdan" to LatLng(11.657246, 125.446223),
        "pepelitan" to LatLng(11.629622, 125.446533),
        "punta maria" to LatLng(11.673449, 125.480943),
        "purok a" to LatLng(11.611212, 125.436756),
        "purok b" to LatLng(11.608503, 125.437801),
        "purok c" to LatLng(11.609700, 125.433919),
        "purok d1" to LatLng(11.607743, 125.434908),
        "purok d2" to LatLng(11.607182, 125.433529),
        "purok e" to LatLng(11.608237, 125.432086),
        "purok f" to LatLng(11.606765, 125.432314),
        "purok g" to LatLng(11.607762, 125.430631),
        "purok h" to LatLng(11.606342, 125.430241),
        "sabang north" to LatLng(11.629410, 125.439637),
        "sabang south" to LatLng(11.622125, 125.440301),
        "san gregorio" to LatLng(11.533728, 125.451524),
        "san saturnino" to LatLng(11.673886, 125.461368),
        "santa fe" to LatLng(11.694258, 125.464156),
        "sohutan" to LatLng(11.605722, 125.407079),
        "songco" to LatLng(11.6163, 125.4357),
        "suribao" to LatLng(11.550962, 125.469227),
        "surok" to LatLng(11.627532, 125.420697),
        "taboc" to LatLng(11.603629, 125.431420),
        "tabunan" to LatLng(11.6645, 125.4509),
        "tamoso" to LatLng(11.684282, 125.464648),
    )

    fun getBarangayCoords(barangay: String): LatLng? {
        val normalized = barangay.trim().lowercase()
        val coords = barangayCoords[normalized]
        if (coords == null) {
            Log.w("GeocodeUtils", "No coordinates found for barangay: $barangay")
        } else {
            Log.d("GeocodeUtils", "Barangay $barangay → $coords")
        }
        return coords
    }
}
