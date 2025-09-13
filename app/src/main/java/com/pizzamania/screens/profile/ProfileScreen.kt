package com.pizzamania.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.navigation.NavController

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {
    suspend fun load(): Triple<String?, String?, String?>? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("users").document(uid).get().await()
        return Triple(doc.getString("name"), doc.getString("phone"), doc.getString("address"))
    }
    suspend fun save(name: String, phone: String, address: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf("name" to name, "phone" to phone, "address" to address), SetOptions.merge())
            .await()
    }
    suspend fun deleteAccount(): String? {
        val user = auth.currentUser ?: return "Not signed in"
        // Best-effort delete Firestore doc
        runCatching { db.collection("users").document(user.uid).delete().await() }
        return runCatching { user.delete().await(); null }
            .getOrElse { e -> e.message ?: "Delete failed. Re-login and try again." }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavController, vm: ProfileViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val data = vm.load()
        name = data?.first ?: ""
        phone = data?.second ?: ""
        address = data?.third ?: ""
        loading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your profile") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(address, { address = it }, label = { Text("Address") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    loading = true
                    scope.launch {
                        vm.save(name.trim(), phone.trim(), address.trim())
                        loading = false
                        nav.popBackStack()
                    }
                },
                enabled = !loading && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showConfirmDelete = true },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete account") }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete account?") },
            text = { Text("This will permanently remove your account.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDelete = false
                    scope.launch {
                        val err = vm.deleteAccount()
                        if (err == null) {
                            snack.showSnackbar("Account deleted")
                            nav.popBackStack() // back to previous (likely menu → will send to auth on actions)
                        } else {
                            snack.showSnackbar(err)
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
