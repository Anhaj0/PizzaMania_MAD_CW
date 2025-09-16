package com.pizzamania.data.local;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items")
public class CartItem {

    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String itemId;
    public String branchId;

    // Display
    public String name;
    /** unit price after size/extras have been applied */
    public double price;
    public int qty;
    @Nullable public String imageUrl;

    // Options
    /** "S","M","L" — never null (default "M") */
    public String size;
    /** comma-separated list: "Extra Cheese,Olives" (nullable = no extras) */
    @Nullable public String extrasCsv;

    /** REQUIRED by Room — keep fields public so Room can set them directly */
    public CartItem() {
        this.size = "M";
    }

    /** Convenience constructor for creating copies or new items */
    @Ignore
    public CartItem(String itemId, String branchId, String name, double price, int qty, @Nullable String imageUrl, String size, @Nullable String extrasCsv) {
        this.itemId = itemId;
        this.branchId = branchId;
        this.name = name;
        this.price = price;
        this.qty = qty;
        this.imageUrl = imageUrl;
        this.size = (size == null || size.isEmpty()) ? "M" : size;
        this.extrasCsv = (extrasCsv != null && extrasCsv.trim().isEmpty()) ? null : extrasCsv;
    }
}