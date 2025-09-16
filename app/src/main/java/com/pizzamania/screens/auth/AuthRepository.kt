package com.pizzamania.screens.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pizzamania.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        address: String
    ): User {
        val e = email.trim().lowercase()

        // Create account
        val result = auth.createUserWithEmailAndPassword(e, password).await()
        val uid = result.user?.uid ?: auth.currentUser?.uid
        ?: throw IllegalStateException("Sign up succeeded but no user session was returned")

        // Write profile (merge)
        val profile = mapOf(
            "uid" to uid,
            "email" to e,
            "name" to name.trim().ifBlank { e.substringBefore('@') },
            "role" to "USER",
            "phone" to phone.trim(),
            "address" to address.trim()
        )
        val docRef = db.collection("users").document(uid)
        docRef.set(profile, SetOptions.merge()).await()

        // Read back as model (avoids constructor mismatch)
        return docRef.get().await().toObject(User::class.java)
            ?: throw IllegalStateException("Failed to parse user profile")
    }

    suspend fun signIn(email: String, password: String): User {
        val e = email.trim().lowercase()
        val result = auth.signInWithEmailAndPassword(e, password).await()
        val uid = result.user?.uid ?: auth.currentUser?.uid
        ?: throw IllegalStateException("Sign in succeeded but no user session was returned")

        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        val existing = snap.toObject(User::class.java)
        if (existing != null) return existing

        // Create minimal doc if missing, then read back
        val minimal = mapOf(
            "uid" to uid,
            "email" to e,
            "name" to (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: e.substringBefore('@')),
            "role" to "USER"
        )
        docRef.set(minimal, SetOptions.merge()).await()
        return docRef.get().await().toObject(User::class.java)
            ?: throw IllegalStateException("Failed to create user profile")
    }

    fun signOut() = auth.signOut()
}
