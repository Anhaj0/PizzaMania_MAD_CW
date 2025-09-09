package com.pizzamania.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface CartDao {

    @Query("SELECT * FROM cart_items WHERE branchId = :branchId")
    Flow<List<CartItem>> watchCart(String branchId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CartItem item);

    @Update
    void update(CartItem item);

    @Query("DELETE FROM cart_items WHERE localId = :localId")
    void deleteById(int localId);

    @Query("DELETE FROM cart_items")
    void clear();
}
