package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzamania.data.model.Order
import com.pizzamania.data.model.OrderStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val col get() = db.collection("orders")

    fun observeAllOrders(): Flow<List<Order>> = callbackFlow {
        val reg = col.orderBy("placedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()) // keep UI alive
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    d.toObject(Order::class.java)?.copy(id = d.id)
                }?.filterNotNull().orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateStatus(orderId: String, status: OrderStatus) {
        col.document(orderId).update(
            mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }
}
