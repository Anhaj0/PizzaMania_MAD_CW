package com.pizzamania.screens.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.pizzamania.data.model.MenuItem
import com.pizzamania.data.repo.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminMenuEditViewModel @Inject constructor(
    private val repo: MenuRepository
) : ViewModel() {
    var existing by mutableStateOf<MenuItem?>(null); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun load(branchId: String, itemId: String) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                existing = repo.getMenuItemOnce(branchId, itemId)
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false }
        }
    }

    suspend fun saveNew(branchId: String, item: MenuItem) = repo.addMenuItem(branchId, item)
    suspend fun saveEdit(branchId: String, item: MenuItem) = repo.updateMenuItem(branchId, item)
    suspend fun delete(branchId: String, itemId: String) = repo.deleteMenuItem(branchId, itemId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuEditScreen(
    nav: NavController,
    branchId: String,
    itemId: String? // null = create
) {
    val vm: AdminMenuEditViewModel = hiltViewModel()
    val ctx = LocalContext.current

    LaunchedEffect(itemId) { if (itemId != null) vm.load(branchId, itemId) }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    var imageUrl by remember { mutableStateOf("") }

    LaunchedEffect(vm.existing) {
        vm.existing?.let {
            title = it.title ?: ""
            desc = it.description ?: ""
            price = if (it.price == 0.0) "" else it.price.toString()
            available = it.isAvailable
            imageUrl = it.imageUrl ?: ""
        }
    }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (itemId == null) "New menu item" else "Edit menu item") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = desc, onValueChange = { desc = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Price") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available"); Spacer(Modifier.width(8.dp))
                Switch(checked = available, onCheckedChange = { available = it })
            }
            OutlinedTextField(
                value = imageUrl, onValueChange = { imageUrl = it.trim() },
                label = { Text("Image URL (paste)") },
                placeholder = { Text("https://.../photo.jpg") },
                modifier = Modifier.fillMaxWidth()
            )

            if (vm.error != null) Text("Error: ${vm.error}", color = MaterialTheme.colorScheme.error)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (title.isBlank() || price.isBlank()) {
                            toast("Title and price are required"); return@Button
                        }
                        val priceD = price.toDoubleOrNull() ?: run {
                            toast("Invalid price"); return@Button
                        }

                        // NOTE: Java constructor => positional args only (no named args), and
                        // description/imageUrl are @Nullable so we can pass null safely.
                        val built = MenuItem(
                            itemId ?: "",
                            title.trim(),
                            desc.trim().ifBlank { null },
                            priceD,
                            available,
                            imageUrl.ifBlank { null }
                        )

                        if (itemId == null) {
                            vm.viewModelScope.launch {
                                try {
                                    vm.saveNew(branchId, built); toast("Saved"); nav.popBackStack()
                                } catch (e: Exception) { toast(e.message ?: "Save failed") }
                            }
                        } else {
                            vm.viewModelScope.launch {
                                try {
                                    vm.saveEdit(branchId, built); toast("Updated"); nav.popBackStack()
                                } catch (e: Exception) { toast(e.message ?: "Update failed") }
                            }
                        }
                    },
                    enabled = !vm.loading
                ) { Text(if (itemId == null) "Save" else "Update") }

                if (itemId != null) {
                    OutlinedButton(
                        onClick = {
                            vm.viewModelScope.launch {
                                try { vm.delete(branchId, itemId); toast("Deleted"); nav.popBackStack() }
                                catch (e: Exception) { toast(e.message ?: "Delete failed") }
                            }
                        },
                        enabled = !vm.loading
                    ) { Text("Delete") }
                }
            }
        }
    }
}
