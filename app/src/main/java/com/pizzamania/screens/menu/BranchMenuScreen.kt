@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.pizzamania.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pizzamania.data.model.MenuItem
import com.pizzamania.data.repo.MenuRepository
import com.pizzamania.data.repo.CartRepository
import com.google.firebase.auth.FirebaseAuth
import com.pizzamania.navigation.Routes

data class BranchMenuUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<MenuItem> = emptyList()
)

@HiltViewModel
class BranchMenuViewModel @Inject constructor(
    private val repo: MenuRepository,
    private val cart: CartRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BranchMenuUiState())
    val state = _state.asStateFlow()

    fun start(branchId: String) {
        if (!_state.value.loading) return
        viewModelScope.launch {
            try {
                repo.listenMenu(branchId).collect { list ->
                    _state.value = BranchMenuUiState(loading = false, items = list)
                }
            } catch (e: Exception) {
                _state.value = BranchMenuUiState(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun addToCart(
        branchId: String,
        item: MenuItem,
        size: String,
        extras: List<String>,
        qty: Int
    ) = viewModelScope.launch {
        val base = item.price
        val sizeMultiplier = when (size) { "S" -> 1.0; "M" -> 1.3; "L" -> 1.6; else -> 1.3 }
        val extrasCost = 80.0 * extras.size
        val unitPrice = (base * sizeMultiplier) + extrasCost

        val name = buildString {
            append(item.title ?: "Untitled")
            append(" (").append(size).append(")")
            if (extras.isNotEmpty()) append(" + ").append(extras.joinToString(", "))
        }
        val extrasCsv = extras.joinToString(", ")

        cart.addOrIncrement(
            branchId = branchId,
            itemId = item.id,
            name = name,
            computedUnitPrice = unitPrice,
            imageUrl = item.imageUrl,
            qty = qty,
            size = size,
            extrasCsv = extrasCsv
        )
    }
}

@Composable
fun BranchMenuScreen(
    navController: NavController,
    branchId: String,
    vm: BranchMenuViewModel = hiltViewModel()
) {
    LaunchedEffect(branchId) { vm.start(branchId) }
    val state by vm.state.collectAsState()
    val auth = remember { FirebaseAuth.getInstance() }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Menu • $branchId") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                onClick = {
                                    menuOpen = false
                                    if (auth.currentUser == null) {
                                        navController.navigate(Routes.Auth)
                                    } else {
                                        navController.navigate(Routes.Profile)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("My orders") },
                                onClick = {
                                    menuOpen = false
                                    if (auth.currentUser == null) {
                                        navController.navigate(Routes.Auth)
                                    } else {
                                        navController.navigate(Routes.MyOrders)
                                    }
                                }
                            )
                            if (auth.currentUser != null) {
                                DropdownMenuItem(
                                    text = { Text("Sign out") },
                                    onClick = {
                                        menuOpen = false
                                        auth.signOut()
                                        navController.navigate(Routes.Auth) {
                                            popUpTo(Routes.Splash) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("cart/$branchId") }) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart")
            }
        }
    ) { inner ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Text(
                "Error: ${state.error}",
                modifier = Modifier.padding(inner).padding(16.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items) { mi ->
                    MenuCard(
                        item = mi,
                        onAdd = { size, extras, qty -> vm.addToCart(branchId, mi, size, extras, qty) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    item: MenuItem,
    onAdd: (size: String, extras: List<String>, qty: Int) -> Unit
) {
    val title = item.title ?: "Untitled"
    val desc = item.description ?: ""
    val basePrice = item.price

    var size by remember { mutableStateOf("M") }
    val allExtras = listOf("Extra Cheese", "Olives", "Mushrooms", "Pepperoni")
    var extras by remember { mutableStateOf(setOf<String>()) }
    var qty by remember { mutableStateOf(1) }

    val sizeMultiplier = when (size) { "S" -> 1.0; "M" -> 1.3; "L" -> 1.6; else -> 1.3 }
    val unitPreview = (basePrice * sizeMultiplier) + (80.0 * extras.size)

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (desc.isNotBlank()) Text(desc)
            Text("Base: Rs. ${"%.2f".format(basePrice)}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("S","M","L").forEach { s ->
                    FilterChip(selected = size == s, onClick = { size = s }, label = { Text("Size $s") })
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allExtras.forEach { e ->
                    FilterChip(
                        selected = extras.contains(e),
                        onClick = { extras = if (extras.contains(e)) extras - e else extras + e },
                        label = { Text(e) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { if (qty > 1) qty-- }) { Text("-") }
                Text("$qty")
                OutlinedButton(onClick = { qty++ }) { Text("+") }
                Spacer(Modifier.weight(1f))
                Text("Unit: Rs. ${"%.2f".format(unitPreview)}")
            }

            Button(
                onClick = { onAdd(size, extras.toList(), qty) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to cart (Rs. ${"%.2f".format(unitPreview * qty)})")
            }
        }
    }
}
