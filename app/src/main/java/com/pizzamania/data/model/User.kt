package com.pizzamania.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "USER" // or "ADMIN"
)
