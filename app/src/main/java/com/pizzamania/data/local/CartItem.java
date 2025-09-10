package com.pizzamania.data.local;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items")
public class CartItem {

    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String itemId;
    public String branchId;

    // Keep names aligned with Kotlin usage
    public String name;
    public double price;
    public int qty;

    @Nullable
    public String imageUrl;

    public CartItem(String itemId, String branchId, String name,
                    double price, int qty, @Nullable String imageUrl) {
        this.itemId = itemId;
        this.branchId = branchId;
        this.name = name;
        this.price = price;
        this.qty = qty;
        this.imageUrl = imageUrl;
    }
}
