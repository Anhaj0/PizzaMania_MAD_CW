package com.pizzamania.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val branchId: String,
    val itemId: String,
    val name: String,
    val price: Long,
    val qty: Int,
    val imageUrl: String?
)
