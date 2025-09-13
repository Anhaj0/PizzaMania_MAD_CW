package com.pizzamania.data.repo

import com.google.firebase.firestore.DocumentSnapshot
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

    /** Patch legacy docs (fallback title from "name", ensure id). */
    private fun patched(doc: DocumentSnapshot): MenuItem {
        val item = doc.toObject(MenuItem::class.java) ?: MenuItem()

        // Ensure id
        val id = item.id?.trim().orEmpty()
        if (id.isEmpty()) item.id = doc.id

        // Ensure title (fallback from legacy "name")
        val title = item.title?.trim().orEmpty()
        if (title.isEmpty()) {
            val fromDoc = (doc.getString("title") ?: doc.getString("name"))?.trim()
            item.title = if (fromDoc.isNullOrBlank()) "Untitled" else fromDoc
        }

        // Ensure price is not NaN (rare malformed docs)
        if (java.lang.Double.isNaN(item.price)) item.price = 0.0

        return item
    }

    override fun listenMenu(branchId: String): Flow<List<MenuItem>> = callbackFlow {
        val reg = col(branchId).addSnapshotListener { snap, e ->
            if (e != null) { trySend(emptyList()); return@addSnapshotListener }
            trySend(snap?.documents?.map { patched(it) } ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    override suspend fun getMenuItemOnce(branchId: String, itemId: String): MenuItem? =
        col(branchId).document(itemId).get().await().let { if (it.exists()) patched(it) else null }

    override suspend fun addMenuItem(branchId: String, item: MenuItem) {
        val newId = item.id?.trim().orEmpty().ifBlank { col(branchId).document().id }
        val ref = col(branchId).document(newId)
        val toSave = MenuItem(
            newId,
            (item.title ?: "").ifBlank { "Untitled" },
            item.description,
            item.price,
            item.isAvailable,
            item.imageUrl
        )
        ref.set(toSave).await()
    }

    override suspend fun updateMenuItem(branchId: String, item: MenuItem) {
        val id = item.id?.trim().orEmpty()
        require(id.isNotBlank()) { "item.id missing" }
        val toSave = MenuItem(
            id,
            (item.title ?: "").ifBlank { "Untitled" },
            item.description,
            item.price,
            item.isAvailable,
            item.imageUrl
        )
        col(branchId).document(id).set(toSave).await()
    }

    override suspend fun deleteMenuItem(branchId: String, itemId: String) {
        col(branchId).document(itemId).delete().await()
    }
}
