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

    /** Create a clean MenuItem without mutating Firestore objects. */
    private fun patched(doc: DocumentSnapshot): MenuItem {
        val raw = doc.toObject(MenuItem::class.java)

        // Pull fields with fallbacks (support legacy "name" → title)
        val id = (raw?.id?.trim().takeUnless { it.isNullOrEmpty() } ?: doc.id)
        val title = (raw?.title?.trim()
            ?: doc.getString("title")?.trim()
            ?: doc.getString("name")?.trim()
            ?: "Untitled")
        val description = raw?.description
        val price = raw?.price?.takeUnless { it.isNaN() } ?: 0.0
        val available = raw?.available ?: false // default false if missing
        val imageUrl = raw?.imageUrl
        val category = (raw?.category ?: doc.getString("category") ?: "pizza")

        return MenuItem(
            id = id,
            title = title,
            description = description,
            price = price,
            available = available,
            imageUrl = imageUrl,
            category = category
        )
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
        val toSave = MenuItem(
            id = newId,
            title = (item.title ?: "").ifBlank { "Untitled" },
            description = item.description,
            price = item.price.takeUnless { it.isNaN() } ?: 0.0,
            available = item.available,
            imageUrl = item.imageUrl,
            category = item.category ?: "pizza"
        )
        col(branchId).document(newId).set(toSave).await()
    }

    override suspend fun updateMenuItem(branchId: String, item: MenuItem) {
        val id = item.id?.trim().orEmpty()
        require(id.isNotBlank()) { "item.id missing" }
        val toSave = MenuItem(
            id = id,
            title = (item.title ?: "").ifBlank { "Untitled" },
            description = item.description,
            price = item.price.takeUnless { it.isNaN() } ?: 0.0,
            available = item.available,
            imageUrl = item.imageUrl,
            category = item.category ?: "pizza"
        )
        col(branchId).document(id).set(toSave).await()
    }

    override suspend fun deleteMenuItem(branchId: String, itemId: String) {
        col(branchId).document(itemId).delete().await()
    }
}
