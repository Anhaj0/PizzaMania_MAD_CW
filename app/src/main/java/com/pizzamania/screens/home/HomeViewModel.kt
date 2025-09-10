package com.pizzamania.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.GeoPoint
import com.pizzamania.data.model.Branch
import com.pizzamania.data.repo.BranchRepository
import com.pizzamania.util.distanceKm
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = false,
    val branches: List<Branch> = emptyList(),
    val nearest: Branch? = null,
    val distanceKm: Double? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: BranchRepository,
    private val fused: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(loading = true))
    val state = _state.asStateFlow()

    init { loadBranches() }

    private fun loadBranches() {
        viewModelScope.launch {
            try {
                val list = repo.fetchBranches()
                _state.value = _state.value.copy(loading = false, branches = list, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load branches")
            }
        }
    }

    fun findNearest() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            _state.value = _state.value.copy(error = "Location permission not granted")
            return
        }

        _state.value = _state.value.copy(loading = true, error = null)

        val token = CancellationTokenSource()
        fused.getCurrentLocation(
            if (fine == PackageManager.PERMISSION_GRANTED) Priority.PRIORITY_HIGH_ACCURACY
            else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            token.token
        ).addOnSuccessListener { loc ->
            if (loc != null) handleLocation(loc.latitude, loc.longitude) else fallbackToLastLocation()
        }.addOnFailureListener {
            fallbackToLastLocation()
        }
    }

    private fun fallbackToLastLocation() {
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) handleLocation(loc.latitude, loc.longitude)
                else _state.value = _state.value.copy(
                    loading = false,
                    error = "Couldn't get location. Make sure location is ON and try again."
                )
            }
            .addOnFailureListener { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Location error")
            }
    }

    private fun handleLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val here = GeoPoint(lat, lon)
            val branches = _state.value.branches
            val nearest = branches.filter { it.location != null }
                .minByOrNull { distanceKm(here, it.location!!) }

            if (nearest != null) {
                _state.value = _state.value.copy(
                    loading = false,
                    nearest = nearest,
                    distanceKm = distanceKm(here, nearest.location!!),
                    error = null
                )
            } else {
                _state.value = _state.value.copy(loading = false, error = "No branches found.")
            }
        }
    }
}
