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
        auth.createUserWithEmailAndPassword(e, password).await()
        val uid = auth.currentUser!!.uid
        val user = User(uid, e, name.trim(), "USER")
        // Save profile fields too
        db.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "email" to e,
                "name" to name.trim(),
                "role" to "USER",
                "phone" to phone.trim(),
                "address" to address.trim()
            ),
            SetOptions.merge()
        ).await()
        return user
    }

    suspend fun signIn(email: String, password: String): User? {
        val e = email.trim().lowercase()
        auth.signInWithEmailAndPassword(e, password).await()
        val uid = auth.currentUser?.uid ?: return null

        val docRef = db.collection("users").document(uid)
        val doc = docRef.get().await()
        val existing = doc.toObject(User::class.java)
        if (existing != null) return existing

        // If user doc missing (old accounts), create a minimal one.
        val fallback = User(uid, e, auth.currentUser?.displayName ?: e.substringBefore('@'), "USER")
        docRef.set(fallback, SetOptions.merge()).await()
        return fallback
    }

    fun signOut() = auth.signOut()
}
