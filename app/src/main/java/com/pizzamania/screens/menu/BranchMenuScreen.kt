package com.pizzamania.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pizzamania.data.model.MenuItem
import com.pizzamania.data.repo.CartRepository
import com.pizzamania.data.repo.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class BranchMenuViewModel @Inject constructor(
    private val repo: MenuRepository,
    private val cart: CartRepository
) : ViewModel() {
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var items by mutableStateOf<List<MenuItem>>(emptyList()); private set
    val qty = mutableStateMapOf<String, Int>()

    fun start(branchId: String) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                repo.listenMenu(branchId).collectLatest { list ->
                    items = list.filter { it.isAvailable } // Java boolean getter -> Kotlin property
                    items.forEach { if (qty[it.id] == null) qty[it.id] = 0 }
                    loading = false
                }
            } catch (e: Exception) {
                error = e.message
                loading = false
            }
        }
    }

    fun inc(id: String) { qty[id] = (qty[id] ?: 0) + 1 }
    fun dec(id: String) { qty[id] = max(0, (qty[id] ?: 0) - 1) }

    fun addToCart(branchId: String, item: MenuItem, count: Int) = viewModelScope.launch {
        if (count <= 0) return@launch
        cart.addOrIncrement(
            branchId = branchId,
            itemId = item.id,
            name = item.title,
            price = item.price,
            imageUrl = item.imageUrl,
            qty = count
        )
        qty[item.id] = 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchMenuScreen(
    navController: NavController,
    branchId: String,
    vm: BranchMenuViewModel = hiltViewModel()
) {
    LaunchedEffect(branchId) { vm.start(branchId) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Menu • $branchId") }) },
        bottomBar = {
            Button(
                onClick = { navController.navigate("cart/$branchId") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text("Go to cart") }
        }
    ) { inner ->
        when {
            vm.loading -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            vm.error != null -> Text("Error: ${vm.error}", modifier = Modifier.padding(inner).padding(16.dp))
            else -> LazyColumn(Modifier.fillMaxSize().padding(inner)) {
                items(vm.items) { mi ->
                    MenuRow(
                        item = mi,
                        qty = vm.qty[mi.id] ?: 0,
                        onMinus = { vm.dec(mi.id) },
                        onPlus = { vm.inc(mi.id) },
                        onAdd = { vm.addToCart(branchId, mi, vm.qty[mi.id] ?: 0) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    item: MenuItem,
    qty: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), tonalElevation = 2.dp) {
        Column(Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text("Rs. ${"%.2f".format(item.price)}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onMinus, enabled = qty > 0) { Text("-") }
                Text("$qty", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onPlus) { Text("+") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onAdd, enabled = qty > 0) { Text("Add") }
            }
        }
    }
}
