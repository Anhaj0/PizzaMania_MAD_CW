package com.pizzamania.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Branch(
    @DocumentId val id: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val active: Boolean = true,
    // Firestore field: "location" (GeoPoint)
    val location: GeoPoint? = null
)
