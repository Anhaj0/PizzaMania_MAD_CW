package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.data.model.MenuItem
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
        .collection("menuItems")

    override fun listenMenu(branchId: String): Flow<List<MenuItem>> = callbackFlow {
        val reg = col(branchId).addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { d ->
                val item = d.toObject(MenuItem::class.java)
                if (item != null) {
                    if (item.id.isBlank()) item.id = d.id  // Java getter/setter exposed as Kotlin property
                    item
                } else null
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun getMenuItemOnce(branchId: String, itemId: String): MenuItem? {
        val d = col(branchId).document(itemId).get().await()
        val item = d.toObject(MenuItem::class.java)
        if (item != null && item.id.isBlank()) item.id = d.id
        return item
    }

    override suspend fun addMenuItem(branchId: String, item: MenuItem) {
        val ref = if (item.id.isBlank()) col(branchId).document() else col(branchId).document(item.id)
        val toSave = MenuItem(
            ref.id,
            item.title,
            item.description,
            item.price,
            item.isAvailable,
            item.imageUrl
        )
        ref.set(toSave).await()
    }

    override suspend fun updateMenuItem(branchId: String, item: MenuItem) {
        require(item.id.isNotBlank()) { "item.id missing" }
        val ref = col(branchId).document(item.id)
        val toSave = MenuItem(
            item.id,
            item.title,
            item.description,
            item.price,
            item.isAvailable,
            item.imageUrl
        )
        ref.set(toSave).await()
    }

    override suspend fun deleteMenuItem(branchId: String, itemId: String) {
        col(branchId).document(itemId).delete().await()
    }
}
