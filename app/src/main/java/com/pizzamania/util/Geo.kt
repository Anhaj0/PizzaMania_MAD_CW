package com.pizzamania.util

import com.google.firebase.firestore.GeoPoint
import kotlin.math.*

fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
    val R = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val aTerm = sin(dLat/2).pow(2.0) + sin(dLon/2).pow(2.0) * cos(lat1) * cos(lat2)
    val c = 2 * asin(min(1.0, sqrt(aTerm)))
    return R * c
}
