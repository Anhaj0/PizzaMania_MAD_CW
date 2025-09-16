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
import com.pizzamania.util.toDirectImageUrl
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
    var priceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm.existing) {
        vm.existing?.let {
            title = it.title ?: ""
            desc = it.description ?: ""
            price = if (it.price == 0.0) "" else it.price.toString()
            available = it.available
            imageUrl = it.imageUrl ?: ""
        }
    }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun sanitizePrice(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        var dotSeen = false
        val sb = StringBuilder()
        for (ch in filtered) {
            if (ch == '.') {
                if (dotSeen) continue
                dotSeen = true
            }
            sb.append(ch)
        }
        val s = sb.toString()
        return if (s == ".") "0." else s
    }

    fun validatePrice(s: String): Double? {
        val d = s.toDoubleOrNull() ?: return null
        if (d < 0.0) return null
        return d
    }

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
                onValueChange = {
                    price = sanitizePrice(it)
                    priceError = null
                },
                label = { Text("Price") },
                isError = priceError != null,
                supportingText = { if (priceError != null) Text(priceError!!) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available"); Spacer(Modifier.width(8.dp))
                Switch(checked = available, onCheckedChange = { available = it })
            }
            OutlinedTextField(
                value = imageUrl, onValueChange = { imageUrl = it },
                label = { Text("Image URL (paste Google Drive link OK)") },
                placeholder = { Text("https://drive.google.com/file/d/…/view?usp=sharing") },
                modifier = Modifier.fillMaxWidth()
            )

            if (vm.error != null) Text("Error: ${vm.error}", color = MaterialTheme.colorScheme.error)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (title.isBlank()) { toast("Title is required"); return@Button }
                        val priceD = validatePrice(price) ?: run {
                            priceError = "Enter a valid non-negative number"
                            return@Button
                        }

                        // ✅ normalize Google Drive share links to direct image URL automatically
                        val normalizedUrl = toDirectImageUrl(imageUrl.trim())

                        val built = MenuItem(
                            id = itemId ?: "",
                            title = title.trim(),
                            description = desc.trim().ifBlank { null },
                            price = priceD,
                            available = available,
                            imageUrl = normalizedUrl?.ifBlank { null },
                            category = vm.existing?.category ?: "pizza"
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
