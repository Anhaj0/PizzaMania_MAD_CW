package com.pizzamania.data.model

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val available: Boolean = true,
    val imageUrl: String? = null
)
