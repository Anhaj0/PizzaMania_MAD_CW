package com.pizzamania.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class MenuItem(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val available: Boolean = true,
    val imageUrl: String? = null,
    val category: String = "Pizza"
) {
    // Bridges for existing UI code
    val name: String get() = title
    val basePrice: Double get() = price
}
