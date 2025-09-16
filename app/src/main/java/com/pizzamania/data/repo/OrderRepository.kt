package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzamania.data.model.Order
import com.pizzamania.data.model.OrderStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val col get() = db.collection("orders")

    /** Create a new order, returning the Firestore document id. */
    suspend fun create(order: Order): String {
        val docRef = col.add(order).await()
        return docRef.id
    }

    /** Observe all orders (admin or user-wide list) sorted by placedAt desc. */
    fun observeAllOrders(): Flow<List<Order>> = callbackFlow {
        val reg = col.orderBy("placedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    d.toObject(Order::class.java)?.copy(id = d.id)
                }?.filterNotNull().orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Observe orders for a specific user (My Orders). */
    fun observeOrdersForUser(userId: String): Flow<List<Order>> = callbackFlow {
        val reg = col.whereEqualTo("userId", userId)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    d.toObject(Order::class.java)?.copy(id = d.id)
                }?.filterNotNull().orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Update order status. */
    suspend fun updateStatus(orderId: String, status: OrderStatus) {
        col.document(orderId).update(
            mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }
}
