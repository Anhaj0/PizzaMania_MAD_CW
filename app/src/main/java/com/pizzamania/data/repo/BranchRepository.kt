package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BranchRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun fetchBranches(): List<Branch> =
        db.collection("branches").get().await().toObjects(Branch::class.java)

    suspend fun getBranch(id: String): Branch? =
        db.collection("branches").document(id).get().await().toObject(Branch::class.java)

    suspend fun createBranch(id: String, b: Branch) {
        db.collection("branches").document(id).set(b).await()
    }

    suspend fun updateBranch(id: String, b: Branch) {
        db.collection("branches").document(id).set(b).await()
    }

    suspend fun deleteBranch(id: String) {
        db.collection("branches").document(id).delete().await()
    }
}
