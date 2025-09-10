package com.pizzamania.data.model;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

/** Firestore menu item model (Java so Kotlin sees platform types safely). */
@Keep
public class MenuItem {
    private String id = "";
    private String title = "";                 // canonical field
    @Nullable private String description;      // may be null
    private double price = 0.0;
    private boolean available = true;
    @Nullable private String imageUrl;         // may be null

    public MenuItem() {}

    public MenuItem(String id,
                    String title,
                    @Nullable String description,
                    double price,
                    boolean available,
                    @Nullable String imageUrl) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.description = description;
        this.price = price;
        this.available = available;
        this.imageUrl = imageUrl;
    }

    // ----- getters / setters -----
    public String getId() { return id; }
    public void setId(String id) { this.id = id == null ? "" : id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title; }

    @Nullable public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Nullable public String getImageUrl() { return imageUrl; }
    public void setImageUrl(@Nullable String imageUrl) { this.imageUrl = imageUrl; }

    // ---- legacy bridge (older docs used "name") ----
    public String getName() { return title; }
    public void setName(String name) { this.title = name; }
}
