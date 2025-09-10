package com.pizzamania.data.model;

import androidx.annotation.Keep;
import com.google.firebase.firestore.GeoPoint;

@Keep
public class Branch {
    private String id;
    private String name;
    private String address;
    private String phone;
    private boolean active = true;

    // New canonical field
    private GeoPoint location;

    // Legacy fields (older docs)
    private Double latitude;
    private Double longitude;

    public Branch() {}

    public Branch(String id, String name, String address, String phone, boolean active, GeoPoint location) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
        this.location = location;
    }

    // --- getters/setters ---
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

    /** Canonical accessor. If missing, synthesize from legacy lat/lng. */
    public GeoPoint getLocation() {
        if (location != null) return location;
        if (latitude != null && longitude != null) return new GeoPoint(latitude, longitude);
        return null;
    }
    public void setLocation(GeoPoint location) { this.location = location; }

    // Legacy accessors (kept so old docs/UIs still read/write if needed)
    public Double getLatitude() {
        if (latitude != null) return latitude;
        return location != null ? location.getLatitude() : null;
    }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() {
        if (longitude != null) return longitude;
        return location != null ? location.getLongitude() : null;
    }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
