package com.pizzamania.screens.splash

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.GeoPoint
import com.pizzamania.data.repo.BranchRepository
import com.pizzamania.util.distanceKm
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val branches: BranchRepository,
    private val fused: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Returns nearest branchId (or null) using last known location if available. */
    suspend fun nearestBranchId(): String? = withContext(Dispatchers.IO) {
        val all = try { branches.fetchBranches() } catch (_: Exception) { emptyList() }
        val withLoc = all.filter { it.location != null }
        if (withLoc.isEmpty()) return@withContext null

        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val here: GeoPoint? = try {
            if (fine || coarse) {
                val l = fused.lastLocation.await()
                if (l != null) GeoPoint(l.latitude, l.longitude) else null
            } else null
        } catch (_: Exception) { null }

        return@withContext if (here != null) {
            withLoc.minByOrNull { distanceKm(here, it.location!!) }?.id
        } else {
            withLoc.first().id
        }
    }
}
