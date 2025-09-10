package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.data.model.Branch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BranchRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun fetchBranches(): List<Branch> =
        db.collection("branches").get().await().map { doc ->
            // Fix: Manually map the document ID to the 'id' field of the Branch object.
            doc.toObject(Branch::class.java).apply { id = doc.id }
        }

    suspend fun getBranch(id: String): Branch? =
        db.collection("branches").document(id).get().await().let { doc ->
            // Fix: Check if the document exists and map the ID before returning.
            if (doc.exists()) {
                doc.toObject(Branch::class.java)?.apply { this.id = doc.id }
            } else {
                null
            }
        }

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