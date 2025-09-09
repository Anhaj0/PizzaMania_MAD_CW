package com.pizzamania.data.repo

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Uploads an image to: menuImages/{branchId}/{itemId}
     * and returns the public download URL.
     */
    suspend fun uploadMenuImage(branchId: String, itemId: String, fileUri: Uri): String {
        val path = "menuImages/$branchId/$itemId"
        val ref = storage.reference.child(path)
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }
}
