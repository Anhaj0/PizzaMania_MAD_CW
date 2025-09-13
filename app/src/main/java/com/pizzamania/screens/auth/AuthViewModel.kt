package com.pizzamania.screens.auth

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.GeoPoint
import com.pizzamania.data.model.User
import com.pizzamania.data.repo.BranchRepository
import com.pizzamania.util.distanceKm
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val branches: BranchRepository,
    private val fused: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun signIn(email: String, password: String, onOk: (User?) -> Unit) =
        viewModelScope.launch {
            runCatching {
                _state.value = AuthUiState(loading = true)
                repo.signIn(email, password)
            }.onSuccess {
                _state.value = AuthUiState()
                onOk(it)
            }.onFailure {
                _state.value = AuthUiState(error = it.message)
                onOk(null)
            }
        }

    fun signUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        onOk: (User?) -> Unit
    ) = viewModelScope.launch {
        runCatching {
            _state.value = AuthUiState(loading = true)
            repo.signUp(name, email, password, phone, address)
        }.onSuccess {
            _state.value = AuthUiState()
            onOk(it)
        }.onFailure {
            _state.value = AuthUiState(error = it.message)
            onOk(null)
        }
    }

    /** Returns nearest branchId or null (no branches / location). */
    suspend fun nearestBranchId(): String? {
        val list = try { branches.fetchBranches() } catch (_: Exception) { emptyList() }
        if (list.isEmpty()) return null

        val here: GeoPoint? = try {
            val fine = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fine || coarse) {
                val loc = fused.lastLocation.await()
                if (loc != null) GeoPoint(loc.latitude, loc.longitude) else null
            } else null
        } catch (_: Exception) { null }

        val withLoc = list.filter { it.location != null }
        if (withLoc.isEmpty()) return null

        return if (here != null) {
            withLoc.minByOrNull { distanceKm(here, it.location!!) }?.id
        } else {
            // Fallback: first active branch
            withLoc.first().id
        }
    }

    /** Best-effort reverse geocode last location to an address string. */
    suspend fun prefillAddressFromLocation(): String? = withContext(Dispatchers.IO) {
        try {
            val fine = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) return@withContext null
            val loc = fused.lastLocation.await() ?: return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            val a = results?.firstOrNull() ?: return@withContext null
            listOfNotNull(
                a.featureName,
                a.subLocality,
                a.locality,
                a.adminArea,
                a.postalCode
            ).distinct().joinToString(", ")
        } catch (_: Exception) {
            null
        }
    }
}
