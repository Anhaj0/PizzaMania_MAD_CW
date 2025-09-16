package com.pizzamania.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose

@Singleton
class ProfileRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private fun doc(uid: String) = db.collection("users").document(uid)

    /** Live profile stream for a user; emits empty profile if not found. */
    fun observe(uid: String): Flow<UserProfile> = callbackFlow {
        val reg = doc(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(UserProfile(uid = uid))
                return@addSnapshotListener
            }
            val p = snap?.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
            trySend(p)
        }
        awaitClose { reg.remove() }
    }

    /** Read once (nullable if no uid). */
    suspend fun get(uid: String?): UserProfile? {
        if (uid.isNullOrBlank()) return null
        val s = doc(uid).get().await()
        return s.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
    }

    /** Upsert. */
    suspend fun save(profile: UserProfile) {
        require(profile.uid.isNotBlank())
        doc(profile.uid).set(profile).await()
    }
}
