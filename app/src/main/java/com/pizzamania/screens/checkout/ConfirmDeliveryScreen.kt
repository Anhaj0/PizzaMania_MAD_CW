package com.pizzamania.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeliveryScreen(
    navController: NavController,
    branchId: String,
    vm: ConfirmDeliveryViewModel = hiltViewModel()
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var subtotal by remember { mutableStateOf(0.0) }
    var delivery by remember { mutableStateOf(0.0) }
    var total by remember { mutableStateOf(0.0) }

    var placing by remember { mutableStateOf(false) }
    var showPlaced by remember { mutableStateOf(false) }
    var lastOrderId by remember { mutableStateOf<String?>(null) }

    // Prefill profile + totals
    LaunchedEffect(branchId) {
        vm.loadProfile()?.let { p ->
            if (p.name.isNotBlank()) name = p.name
            if (p.phone.isNotBlank()) phone = p.phone
            if (p.address.isNotBlank()) address = p.address
        }
        val items = vm.loadCartOnce(branchId)
        subtotal = items.sumOf { it.price * it.qty }
        delivery = if (items.isEmpty() || subtotal >= 3000.0) 0.0 else 250.0
        total = subtotal + delivery
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Confirm delivery") },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delivery details", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Delivery instructions (optional)") })
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order summary", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth()) {
                        Text("Subtotal"); Spacer(Modifier.weight(1f)); Text("Rs. ${"%,.0f".format(subtotal)}")
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Delivery"); Spacer(Modifier.weight(1f)); Text(if (delivery == 0.0) "FREE" else "Rs. ${"%,.0f".format(delivery)}")
                    }
                    Divider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("Rs. ${"%,.0f".format(total)}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        if (placing) return@launch
                        if (name.isBlank() || phone.isBlank() || address.isBlank()) {
                            snackbar.showSnackbar("Please fill name, phone and address")
                            return@launch
                        }
                        placing = true
                        try {
                            vm.saveProfileIfChanged(name, phone, address)
                            val items = vm.loadCartOnce(branchId)
                            if (items.isEmpty()) {
                                snackbar.showSnackbar("Your cart is empty")
                                placing = false
                                return@launch
                            }
                            val orderId = vm.placeOrder(branchId, name, phone, address, notes, items)
                            vm.clearCart(branchId)

                            lastOrderId = orderId
                            showPlaced = true
                            snackbar.showSnackbar("Order placed • #$orderId")
                        } catch (t: Throwable) {
                            snackbar.showSnackbar(t.message ?: "Failed to place order")
                        } finally {
                            placing = false
                        }
                    }
                },
                enabled = !placing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (placing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (placing) "Placing…" else "Place order")
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // Success dialog
    if (showPlaced) {
        AlertDialog(
            onDismissRequest = { /* Block outside dismiss to make it explicit */ },
            title = { Text("Order placed") },
            text = {
                Text(
                    buildString {
                        append("Your order has been placed successfully")
                        lastOrderId?.let { append(" (#").append(it).append(")") }
                        append(".")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPlaced = false
                        // Go back to Cart, then back to Menu/Builder
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                ) { Text("OK") }
            }
        )
    }
}
