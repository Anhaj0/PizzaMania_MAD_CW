package com.pizzamania.screens.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pizzamania.data.local.CartItem
import com.pizzamania.data.repo.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(private val repo: CartRepository)
    : androidx.lifecycle.ViewModel() {

    fun cart(branchId: String) = repo.observeCart(branchId)
    suspend fun change(item: CartItem, qty: Int) = repo.changeQty(item, qty)
    suspend fun clear(branchId: String) = repo.clearBranch(branchId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController, branchId: String, vm: CartViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val items by vm.cart(branchId).collectAsState(initial = emptyList())
    val total by vm.cart(branchId).map { list -> list.sumOf { it.price * it.qty } }.collectAsState(initial = 0.0)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cart") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { scope.launch { vm.clear(branchId) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear cart")
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(12.dp)) {
            if (items.isEmpty()) {
                Text("Your cart is empty.")
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { it ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(it.name, style = MaterialTheme.typography.titleMedium)
                                if (it.extrasCsv != null) {
                                    Text("Extras: ${it.extrasCsv}")
                                }
                                Text("Size: ${it.size}")
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Rs. ${"%.2f".format(it.price)} x ${it.qty}")
                                    Row {
                                        TextButton(onClick = { scope.launch { vm.change(it, it.qty - 1) } }) { Text("-") }
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(onClick = { scope.launch { vm.change(it, it.qty + 1) } }) { Text("+") }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Total: Rs. ${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("confirm/$branchId") },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm delivery") }
            }
        }
    }
}
