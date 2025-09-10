package com.pizzamania.data.model;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

@Keep
public class MenuItem {
    private String id;
    private String title;                   // <— use "title" (UI uses this)
    @Nullable private String description;
    private double price;
    private boolean available;
    @Nullable private String imageUrl;

    public MenuItem() {}

    public MenuItem(String id, String title, @Nullable String description,
                    double price, boolean available, @Nullable String imageUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.available = available;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Nullable public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Nullable public String getImageUrl() { return imageUrl; }
    public void setImageUrl(@Nullable String imageUrl) { this.imageUrl = imageUrl; }
}
