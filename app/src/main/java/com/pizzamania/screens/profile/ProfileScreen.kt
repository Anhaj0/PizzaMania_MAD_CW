package com.pizzamania.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.pizzamania.data.model.UserProfile
import com.pizzamania.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository
) : ViewModel() {

    private val uid: String? = try {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Throwable) { null }

    val profile: StateFlow<UserProfile?> = flow {
        emit(repo.get(uid))
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    fun save(name: String, phone: String, address: String, onDone: (Boolean) -> Unit) {
        val u = uid
        if (u.isNullOrBlank()) { onDone(false); return }
        viewModelScope.launch {
            try {
                repo.save(UserProfile(uid = u, name = name.trim(), phone = phone.trim(), address = address.trim()))
                onDone(true)
            } catch (_: Throwable) {
                onDone(false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val p by vm.profile.collectAsState()

    var name by remember(p) { mutableStateOf(p?.name ?: "") }
    var phone by remember(p) { mutableStateOf(p?.phone ?: "") }
    var address by remember(p) { mutableStateOf(p?.address ?: "") }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                label = { Text("Phone") }, singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = address, onValueChange = { address = it },
                label = { Text("Address") }, minLines = 3
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || address.isBlank()) {
                        scope.launch { snackbar.showSnackbar("Please fill all fields.") }
                        return@Button
                    }
                    saving = true
                    vm.save(name, phone, address) { ok ->
                        saving = false
                        scope.launch {
                            snackbar.showSnackbar(if (ok) "Profile saved" else "Failed to save")
                            if (ok) navController.popBackStack()
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                }
                Text("Save")
            }
        }
    }
}