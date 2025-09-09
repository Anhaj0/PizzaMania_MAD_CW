package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : MenuRepository {

    private fun col(branchId: String) = db.collection("branches")
        .document(branchId)
        .collection("menuItems")          // <- use menuItems (matches your Firestore)

    override fun listenMenu(branchId: String): Flow<List<MenuItem>> = callbackFlow {
        val reg = col(branchId).addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull {
                it.toObject(MenuItem::class.java)?.copy(id = it.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun getMenuItemOnce(branchId: String, itemId: String): MenuItem? =
        col(branchId).document(itemId).get().await()
            .toObject(MenuItem::class.java)?.copy(id = itemId)

    override suspend fun addMenuItem(branchId: String, item: MenuItem) {
        val ref = if (item.id.isBlank()) col(branchId).document() else col(branchId).document(item.id)
        ref.set(item.copy(id = ref.id)).await()
    }

    override suspend fun updateMenuItem(branchId: String, item: MenuItem) {
        require(item.id.isNotBlank()) { "item.id missing" }
        col(branchId).document(item.id).set(item).await()
    }

    override suspend fun deleteMenuItem(branchId: String, itemId: String) {
        col(branchId).document(itemId).delete().await()
    }
}
