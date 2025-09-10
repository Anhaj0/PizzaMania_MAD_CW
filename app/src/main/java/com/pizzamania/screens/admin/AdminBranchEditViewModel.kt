package com.pizzamania.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.pizzamania.data.model.Branch
import com.pizzamania.data.repo.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminBranchEditViewModel @Inject constructor(
    private val repo: BranchRepository
) : ViewModel() {

    var isEdit by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var saved by mutableStateOf(false); private set

    var id by mutableStateOf("")
    var name by mutableStateOf("")
    var address by mutableStateOf("")
    var phone by mutableStateOf("")
    var active by mutableStateOf(true)
    var lat by mutableStateOf("")
    var lon by mutableStateOf("")

    fun startNew() {
        isEdit = false
        id = ""
        name = ""
        address = ""
        phone = ""
        active = true
        lat = ""
        lon = ""
        error = null
        saved = false
    }

    fun load(branchId: String) {
        if (loading) return
        isEdit = true
        id = branchId
        loading = true
        viewModelScope.launch {
            try {
                val b = repo.getBranch(branchId)
                if (b != null) {
                    // Safely map values from Firestore model
                    name = b.name ?: ""
                    address = b.address ?: ""
                    phone = b.phone ?: ""
                    active = b.isActive
                    lat = b.location?.latitude?.toString() ?: ""
                    lon = b.location?.longitude?.toString() ?: ""
                    error = null
                } else {
                    error = "Branch not found"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun save() {
        if (id.isBlank() || name.isBlank()) {
            error = "ID and Name are required"
            return
        }
        loading = true
        viewModelScope.launch {
            try {
                val latD = lat.toDoubleOrNull() ?: 0.0
                val lonD = lon.toDoubleOrNull() ?: 0.0
                val geo = GeoPoint(latD, lonD) // Branch requires GeoPoint

                // Your Branch model signature is:
                // Branch(id: String, name: String, address: String, phone: String, active: Boolean, location: GeoPoint)
                val branch = Branch(
                    id.trim(),
                    name.trim(),
                    address.trim(),
                    phone.trim(),
                    active,
                    geo
                )

                if (isEdit) repo.updateBranch(id, branch) else repo.createBranch(id, branch)
                saved = true
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun delete() {
        if (!isEdit) return
        loading = true
        viewModelScope.launch {
            try {
                repo.deleteBranch(id)
                saved = true
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
}
