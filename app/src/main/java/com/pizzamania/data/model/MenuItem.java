package com.pizzamania.data.model;

import androidx.annotation.Keep;

@Keep
public class MenuItem {
    private String id;
    private String name;
    private String description;
    private double price;
    private boolean available;
    private String imageUrl;

    public MenuItem() {}

    public MenuItem(String id, String name, String description, double price,
                    boolean available, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
        this.imageUrl = imageUrl;
    }

    // ----- getters / setters -----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
