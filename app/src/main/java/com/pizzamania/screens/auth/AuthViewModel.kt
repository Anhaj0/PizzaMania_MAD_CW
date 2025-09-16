package com.pizzamania.screens.auth

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.pizzamania.data.model.User
import com.pizzamania.data.repo.BranchRepository
import com.pizzamania.util.distanceKm
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _state.value = AuthUiState(error = e.message ?: "Sign-in failed")
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
        }.onFailure { e ->
            if (e is CancellationException) throw e
            _state.value = AuthUiState(error = e.message ?: "Sign-up failed")
            onOk(null)
        }
    }

    /**
     * Returns the current user's role:
     *  - "admin" if users/{uid}.role == "admin"
     *  - "user" when present but not admin
     *  - "guest" when no auth user
     * Never throws.
     */
    suspend fun currentRole(): String = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext "guest"
        return@withContext try {
            val snap = FirebaseFirestore.getInstance()
                .collection("users").document(uid).get().await()
            (snap.getString("role") ?: "user").lowercase()
        } catch (_: Exception) {
            "user"
        }
    }

    /** Returns nearest branchId or null. Safe against missing permissions/GMS/locations. */
    suspend fun nearestBranchId(): String? = withContext(Dispatchers.IO) {
        val list = try { branches.fetchBranches() } catch (_: Exception) { emptyList() }
        if (list.isEmpty()) return@withContext null

        // use first branch with location if we can't get a user location
        val branchesWithLoc = list.filter { it.location != null }
        if (branchesWithLoc.isEmpty()) return@withContext list.firstOrNull()?.id

        val here: GeoPoint? = try {
            val fine = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!(fine || coarse)) null
            else {
                runCatching { fused.lastLocation.await() }.getOrNull()?.let {
                    GeoPoint(it.latitude, it.longitude)
                }
            }
        } catch (_: Exception) { null }

        return@withContext if (here != null) {
            branchesWithLoc.minByOrNull { distanceKm(here, it.location!!) }?.id
        } else {
            branchesWithLoc.first().id
        }
    }

    /** Best-effort reverse geocode last location to an address; never throws. */
    suspend fun prefillAddressFromLocation(): String? = withContext(Dispatchers.IO) {
        try {
            val fine = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) return@withContext null

            val loc = runCatching { fused.lastLocation.await() }.getOrNull() ?: return@withContext null

            if (!Geocoder.isPresent()) return@withContext null

            val geocoder = Geocoder(context, Locale.getDefault())
            val results = runCatching {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            }.getOrNull()

            val a = results?.firstOrNull() ?: return@withContext null
            listOfNotNull(
                a.featureName,
                a.subLocality,
                a.locality,
                a.adminArea,
                a.postalCode
            ).distinct().joinToString(", ").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
