package com.example.mealrushapplication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.GeoPoint
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPage(
    cart: MutableList<MenuItem>,
    navController: NavController,
    subtotal: Double,
    restaurantId: String,
    onBack: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var defaultFirstName by remember { mutableStateOf("") }
    var defaultSurname by remember { mutableStateOf("") }
    var defaultPhone by remember { mutableStateOf("") }
    var defaultStreet by remember { mutableStateOf("") }
    var defaultCity by remember { mutableStateOf("") }

    var selectedMethod by remember { mutableStateOf("Cash On Delivery") }
    var showMessage by remember { mutableStateOf(false) }
    var addNewRecipient by remember { mutableStateOf(false) }

    val savedRecipients = remember { mutableStateListOf<Map<String, String>>() }
    var recipientExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var deliveryFee by remember { mutableStateOf(0.0) }
    var totalPrice by remember { mutableStateOf(subtotal) }

    // Calculate delivery fee automatically when address/cart changes
    LaunchedEffect(street, city, cart, restaurantId) {
        val orsApiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImNjOTc4YmU0MjgxNDQ3N2ViMmMwMjI4ZmU1NjY2YmYyIiwiaCI6Im11cm11cjY0In0="

        if (street.isBlank() || city.isBlank()) {
            deliveryFee = 0.0
            totalPrice = subtotal
            return@LaunchedEffect
        }

        // Use hardcoded barangay coordinates instead of geocodeAddress
        val customerLatLng = GeocodeUtils.getBarangayCoords(street)
        if (customerLatLng == null) {
            deliveryFee = 0.0
            totalPrice = subtotal
            return@LaunchedEffect
        }

        val customerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirestoreUtils.saveCustomerLocation(customerId, customerLatLng)

        val restaurantDoc = db.collection("restaurant").document(restaurantId).get().await()
        val geoPoint = restaurantDoc.getGeoPoint("geoPoint") ?: return@LaunchedEffect

        val restaurantLatLng = LatLng(geoPoint.latitude, geoPoint.longitude)

        val distanceMeters = DistanceUtils.getRouteDistanceMeters(customerLatLng, listOf(restaurantLatLng), orsApiKey)
        if (distanceMeters <= 0) {
            deliveryFee = 0.0
            totalPrice = subtotal
            return@LaunchedEffect
        }

        deliveryFee = DistanceUtils.calculateDeliveryFee(distanceMeters)
        totalPrice = subtotal + deliveryFee
    }

    // Load saved profile data
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("user").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        defaultFirstName = doc.getString("firstName") ?: ""
                        defaultSurname = doc.getString("surname") ?: ""
                        defaultPhone = doc.getString("phone") ?: ""
                        defaultStreet = doc.getString("street") ?: ""
                        defaultCity = doc.getString("city") ?: ""

                        if (defaultFirstName.isNotBlank() || defaultSurname.isNotBlank() || defaultPhone.isNotBlank()) {
                            firstName = defaultFirstName
                            surname = defaultSurname
                            recipientPhone = defaultPhone
                            street = defaultStreet
                            city = defaultCity
                            addNewRecipient = false
                        } else {
                            addNewRecipient = true
                        }

                        val recs = doc.get("recipients") as? List<*>
                        recs?.mapNotNull { recMap -> recMap as? Map<String, Any> }?.let { saved ->
                            savedRecipients.clear()
                            savedRecipients.addAll(saved.map { r ->
                                mapOf(
                                    "firstName" to (r["firstName"]?.toString() ?: ""),
                                    "surname" to (r["surname"]?.toString() ?: ""),
                                    "phone" to (r["phone"]?.toString() ?: ""),
                                    "street" to (r["street"]?.toString() ?: ""),
                                    "city" to (r["city"]?.toString() ?: "")
                                )
                            })
                        }
                    } else {
                        addNewRecipient = true
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF9800),
                                    Color(0xFFFFB74D)
                                )
                            )
                        )
                ) {
                    // Row with back button + text side by side at bottom-left
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checkout",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text("Subtotal: ₱$subtotal", style = MaterialTheme.typography.titleMedium)
                Text("Delivery Fee: ₱$deliveryFee", style = MaterialTheme.typography.titleMedium)
                Text("Total: ₱$totalPrice", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            Log.e("PaymentPage", "No user logged in!")
                            return@Button
                        }
                        scope.launch {
                            try {
                                // Save order
                                val orderData = mapOf(
                                    "items" to cart.map { item ->
                                        mapOf(
                                            "name" to item.name,
                                            "price" to item.price,
                                            "quantity" to item.quantity,
                                            "subtotal" to item.price * item.quantity,
                                            "imageUrl" to item.imageUrl
                                        )
                                    },
                                    "subtotal" to subtotal,
                                    "deliveryFee" to deliveryFee,
                                    "totalPrice" to totalPrice,
                                    "paymentMethod" to selectedMethod,
                                    "recipient" to mapOf(
                                        "firstName" to firstName,
                                        "surname" to surname,
                                        "phone" to recipientPhone,
                                        "street" to street,
                                        "city" to city
                                    ),
                                    "status" to "pending",
                                    "timestamp" to FieldValue.serverTimestamp(),
                                    "userId" to user.uid
                                )
                                db.collection("orders").add(orderData)
                                    .addOnSuccessListener { docRef ->
                                        Log.d("PaymentPage", "Order saved successfully!")
                                        showMessage = true
                                        cart.clear()

                                        if (selectedMethod == "GCash") {
                                            // Navigate to GCash payment screen with orderId + amount
                                            navController.navigate("gcash_payment/${docRef.id}/$totalPrice")
                                        } else {
                                            onConfirmPayment()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("PaymentPage", "Error saving order", e)
                                    }

                                // Save recipient + address to user profile
                                val recipientData = mapOf(
                                    "firstName" to firstName,
                                    "surname" to surname,
                                    "phone" to recipientPhone,
                                    "street" to street,
                                    "city" to city
                                )

                                val userDocRef = db.collection("user").document(user.uid)
                                userDocRef.get().addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        val hasDefault =
                                            doc.getString("firstName")?.isNotBlank() == true ||
                                                    doc.getString("surname")?.isNotBlank() == true ||
                                                    doc.getString("phone")?.isNotBlank() == true

                                        if (!hasDefault) {
                                            userDocRef.set(recipientData, SetOptions.merge())
                                        }
                                    } else {
                                        userDocRef.set(recipientData, SetOptions.merge())
                                    }
                                    userDocRef.update("recipients", FieldValue.arrayUnion(recipientData))
                                        .addOnSuccessListener {
                                            Log.d("PaymentPage", "Recipient added to profile successfully!")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("PaymentPage", "Error saving recipient to profile", e)
                                        }
                                }
                            } catch (e: Exception) {
                                Log.e("PaymentPage", "Error placing order", e)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Place Order", style = MaterialTheme.typography.titleMedium)
                }

            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // Recipient dropdown + fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recipient Details", style = MaterialTheme.typography.titleMedium)
                Row {
                    Box {
                        IconButton(onClick = { recipientExpanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Recipient")
                        }
                        DropdownMenu(
                            expanded = recipientExpanded,
                            onDismissRequest = { recipientExpanded = false }
                        ) {
                            if (defaultFirstName.isNotBlank() || defaultStreet.isNotBlank() || defaultCity.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("$defaultFirstName $defaultSurname - $defaultStreet, $defaultCity") },
                                    onClick = {
                                        firstName = defaultFirstName
                                        surname = defaultSurname
                                        recipientPhone = defaultPhone
                                        street = defaultStreet
                                        city = defaultCity
                                        recipientExpanded = false
                                        addNewRecipient = false
                                    }
                                )
                            }
                            savedRecipients.forEach { rec ->
                                DropdownMenuItem(
                                    text = { Text("${rec["firstName"]} ${rec["surname"]} - ${rec["street"]}, ${rec["city"]}") },
                                    onClick = {
                                        firstName = rec["firstName"] ?: ""
                                        surname = rec["surname"] ?: ""
                                        recipientPhone = rec["phone"] ?: ""
                                        street = rec["street"] ?: ""
                                        city = rec["city"] ?: ""
                                        recipientExpanded = false
                                        addNewRecipient = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        firstName = ""
                        surname = ""
                        recipientPhone = ""
                        street = ""
                        city = ""
                        addNewRecipient = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Recipient")
                    }
                }
            }

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                readOnly = !addNewRecipient,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
            )

            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Surname") },
                readOnly = !addNewRecipient,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
            )

            OutlinedTextField(
                value = recipientPhone,
                onValueChange = { recipientPhone = it },
                label = { Text("Phone") },
                readOnly = !addNewRecipient,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Delivery Address", style = MaterialTheme.typography.titleMedium)

            BarangayDropdown(
                barangays = listOf(
                    "Alang-alang",
                    "Amantacop",
                    "Balacdas",
                    "Balud 1",
                    "Balud 2",
                    "Bato",
                    "Bayobay",
                    "Bugas",
                    "Cabong",
                    "Cagbonga",
                    "Calico-an",
                    "Calingatngan",
                    "Camada",
                    "Campesao",
                    "Can-abong",
                    "Canjaway",
                    "Canlaray",
                    "Hindang",
                    "Lalawigan",
                    "Libuton",
                    "Locso-on",
                    "Maybacong",
                    "Maypangdan",
                    "Pepelitan",
                    "Punta Maria",
                    "Purok A",
                    "Purok B",
                    "Purok C",
                    "Purok D1",
                    "Purok D2",
                    "Purok E",
                    "Purok F",
                    "Purok G",
                    "Purok H",
                    "Sabang North",
                    "Sabang South",
                    "San Gregorio",
                    "San Saturnino",
                    "Santa Fe",
                    "Sohutan",
                    "Songco",
                    "Suribao",
                    "Surok",
                    "Taboc",
                    "Tabunan",
                    "Tamoso"
                ),
                selectedBarangay = street,
                onBarangaySelected = { selected ->
                    street = selected
                    scope.launch {
                        val orsApiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImNjOTc4YmU0MjgxNDQ3N2ViMmMwMjI4ZmU1NjY2YmYyIiwiaCI6Im11cm11cjY0In0="

                        // ✅ Use hardcoded barangay coordinates
                        val destCoords = GeocodeUtils.getBarangayCoords(selected)
                        if (destCoords == null) {
                            deliveryFee = 0.0
                            totalPrice = subtotal
                            return@launch
                        }

                        val customerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        FirestoreUtils.saveCustomerLocation(customerId, destCoords)

                        val restaurantDoc = db.collection("restaurant").document(restaurantId).get().await()
                        val restaurantGeo = restaurantDoc.getGeoPoint("geoPoint") ?: return@launch
                        val restaurantLatLng = LatLng(restaurantGeo.latitude, restaurantGeo.longitude)

                        val distanceMeters = DistanceUtils.getRouteDistanceMeters(destCoords, listOf(restaurantLatLng), orsApiKey)
                        if (distanceMeters <= 0) {
                            deliveryFee = 0.0
                            totalPrice = subtotal
                            return@launch
                        }

                        deliveryFee = DistanceUtils.calculateDeliveryFee(distanceMeters)
                        totalPrice = subtotal + deliveryFee
                    }
                }
            )

            // Fix City / Province to Borongan City
            CityDropdown(
                cities = listOf("Borongan City"),
                selectedCity = city,
                onCitySelected = { selected ->
                    city = selected
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Choose Payment Method", style = MaterialTheme.typography.titleMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,   // ✅ align to left
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMethod == "GCash",
                        onClick = { selectedMethod = "GCash" }
                    )
                    Text("GCash", fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Spacer(modifier = Modifier.width(24.dp)) // ✅ spacing between options

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMethod == "Cash On Delivery",
                        onClick = { selectedMethod = "Cash On Delivery" }
                    )
                    Text("Cash On Delivery", fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }

    if (showMessage) {
        LaunchedEffect(Unit) {
            delay(2000)
            showMessage = false

            val user = FirebaseAuth.getInstance().currentUser
            val db = FirebaseFirestore.getInstance()

            user?.let {
                val orderData = mapOf(
                    "items" to cart.map { item: MenuItem ->
                        mapOf(
                            "name" to item.name,
                            "price" to item.price,
                            "quantity" to item.quantity,
                            "imageUrl" to item.imageUrl
                        )
                    },
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(it.uid)
                    .collection("orders")
                    .add(orderData)
            }

            if (cart is MutableList<MenuItem>) {
                cart.clear()
            }

            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false } // keep start destination
                launchSingleTop = true
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.padding(32.dp).size(width = 260.dp, height = 200.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Order Placed",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
