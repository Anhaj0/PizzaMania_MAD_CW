package com.pizzamania.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import androidx.annotation.Nullable;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface CartDao {

    @Query("SELECT * FROM cart_items WHERE branchId = :branchId ORDER BY localId DESC")
    Flow<List<CartItem>> observeCart(String branchId);

    // Same item + same options → merge qty
    @Query("SELECT * FROM cart_items " +
            "WHERE branchId = :branchId " +
            "AND itemId = :itemId " +
            "AND size = :size " +
            "AND COALESCE(extrasCsv,'') = COALESCE(:extrasCsv,'') " +
            "LIMIT 1")
    CartItem getItemWithOptions(String branchId, String itemId, String size, @Nullable String extrasCsv);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CartItem item);

    @Update
    void update(CartItem item);

    @Delete
    void delete(CartItem item);

    @Query("DELETE FROM cart_items WHERE branchId = :branchId")
    void clearBranch(String branchId);
}
