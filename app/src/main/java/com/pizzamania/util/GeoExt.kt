package com.pizzamania.util

import com.google.firebase.firestore.GeoPoint

fun distanceKm(a: GeoPoint, b: GeoPoint): Double =
    GeoUtils.haversineKm(a.latitude, a.longitude, b.latitude, b.longitude)
