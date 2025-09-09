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
    public String title;
    public double price;
    public int qty;

    @Nullable
    public String photoUrl;

    public CartItem(String itemId, String branchId, String title,
                    double price, int qty, @Nullable String photoUrl) {
        this.itemId = itemId;
        this.branchId = branchId;
        this.title = title;
        this.price = price;
        this.qty = qty;
        this.photoUrl = photoUrl;
    }
}
