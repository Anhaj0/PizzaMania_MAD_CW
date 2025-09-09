package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.data.model.MenuItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private fun col(branchId: String) =
        db.collection("branches").document(branchId).collection("menuItems")

    suspend fun getMenu(branchId: String): List<MenuItem> =
        col(branchId).get().await().documents.map { d ->
            d.toObject(MenuItem::class.java)?.copy(id = d.id) ?: MenuItem(id = d.id)
        }

    suspend fun getItem(branchId: String, itemId: String): MenuItem? =
        col(branchId).document(itemId).get().await()
            .toObject(MenuItem::class.java)?.copy(id = itemId)

    suspend fun create(branchId: String, item: MenuItem) {
        col(branchId).document(item.id).set(item).await()
    }

    suspend fun update(branchId: String, item: MenuItem) {
        col(branchId).document(item.id).set(item).await()
    }

    suspend fun delete(branchId: String, itemId: String) {
        col(branchId).document(itemId).delete().await()
    }
}
