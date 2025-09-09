package com.pizzamania.data.model;

import androidx.annotation.Keep;

@Keep // helps Firestore/obfuscation
public class Branch {
    private String id;
    private String name;
    private String address;
    private String phone;
    private boolean active;
    private double latitude;
    private double longitude;

    // Firestore needs a no-arg constructor
    public Branch() {}

    public Branch(String id, String name, String address, String phone,
                  boolean active, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ----- getters / setters -----
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

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
