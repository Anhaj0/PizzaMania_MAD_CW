package com.pizzamania.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.firestore.GeoPoint
import com.pizzamania.data.model.Branch
import com.pizzamania.data.repo.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.Alignment


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
        id = ""; name = ""; address = ""; phone = ""; active = true; lat = ""; lon = ""
        error = null; saved = false
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
                    name = b.name
                    address = b.address
                    phone = b.phone
                    active = b.active
                    lat = b.location?.latitude?.toString() ?: ""
                    lon = b.location?.longitude?.toString() ?: ""
                } else {
                    error = "Branch not found"
                }
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false }
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
                val latD = lat.toDoubleOrNull()
                val lonD = lon.toDoubleOrNull()
                val loc: GeoPoint? = if (latD != null && lonD != null) GeoPoint(latD, lonD) else null

                val branch = Branch(
                    id = id.trim(),
                    name = name.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    active = active,
                    location = loc
                )
                if (isEdit) repo.updateBranch(id, branch) else repo.createBranch(id, branch)
                saved = true
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false }
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
            } finally { loading = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBranchEditScreen(
    navController: NavController,
    branchId: String?,
    vm: AdminBranchEditViewModel = hiltViewModel()
) {
    LaunchedEffect(branchId) {
        if (branchId == null) vm.startNew() else vm.load(branchId)
    }

    if (vm.saved) {
        // go back after save/delete
        LaunchedEffect(Unit) { navController.popBackStack() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(if (vm.isEdit) "Edit branch" else "New branch")
            })
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (vm.isEdit) {
                    OutlinedButton(
                        onClick = { vm.delete() },
                        enabled = !vm.loading,
                        modifier = Modifier.weight(1f)
                    ) { Text("Delete") }
                }
                Button(
                    onClick = { vm.save() },
                    enabled = !vm.loading,
                    modifier = Modifier.weight(1f)
                ) { Text(if (vm.loading) "Saving..." else "Save") }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = vm.id,
                onValueChange = { vm.id = it.lowercase().replace(" ", "-") },
                label = { Text("Branch ID (doc id)") },
                singleLine = true,
                enabled = !vm.isEdit,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = vm.name, onValueChange = { vm.name = it },
                label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = vm.address, onValueChange = { vm.address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = vm.phone, onValueChange = { vm.phone = it },
                label = { Text("Phone") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active")
                Spacer(Modifier.width(12.dp))
                Switch(checked = vm.active, onCheckedChange = { vm.active = it })
            }
            OutlinedTextField(
                value = vm.lat, onValueChange = { vm.lat = it },
                label = { Text("Latitude") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = vm.lon, onValueChange = { vm.lon = it },
                label = { Text("Longitude") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (vm.error != null) {
                Text(vm.error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
