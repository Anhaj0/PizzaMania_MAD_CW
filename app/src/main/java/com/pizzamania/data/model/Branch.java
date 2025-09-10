package com.pizzamania.data.model;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.GeoPoint;

@Keep
public class Branch {
    private String id;
    private String name;
    private String address;
    private String phone;
    private boolean active;
    @Nullable private GeoPoint location; // lat/lng for nearest-branch logic

    public Branch() {}

    public Branch(String id, String name, String address, String phone,
                  boolean active, @Nullable GeoPoint location) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
        this.location = location;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Nullable public GeoPoint getLocation() { return location; }
    public void setLocation(@Nullable GeoPoint location) { this.location = location; }
}
