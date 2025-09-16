package com.pizzamania.data.model

data class Topping(
    val id: String,
    val name: String,
    val price: Double,
    val emoji: String,          // fallback visual if you don’t add images
    val pieces: Int = 10        // how many fall when added
)
