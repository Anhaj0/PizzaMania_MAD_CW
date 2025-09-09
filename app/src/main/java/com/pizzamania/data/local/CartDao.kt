package com.pizzamania.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE branchId = :branchId ORDER BY localId DESC")
    fun observeCart(branchId: String): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE branchId = :branchId AND itemId = :itemId LIMIT 1")
    suspend fun getItem(branchId: String, itemId: String): CartItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: CartItem): Long

    @Update
    suspend fun update(item: CartItem)

    @Delete
    suspend fun delete(item: CartItem)

    @Query("DELETE FROM cart_items WHERE branchId = :branchId")
    suspend fun clearBranch(branchId: String)
}
