package com.example.mealrushapplication

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OrdersViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _orders = MutableStateFlow<List<Pair<String, Map<String, Any>>>>(emptyList())
    val orders: StateFlow<List<Pair<String, Map<String, Any>>>> = _orders

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var listener: ListenerRegistration? = null

    fun startListening(isOnline: Boolean) {
        listener?.remove()
        _orders.value = emptyList()
        _isLoading.value = true

        if (isOnline) {
            listener = firestore.collection("orders")
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        _orders.value = snapshot.documents.map { doc ->
                            doc.id to (doc.data ?: emptyMap())
                        }
                        _isLoading.value = false
                    }
                }
        } else {
            _isLoading.value = false
            _orders.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
