package com.pizzamania.screens.cart

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.pizzamania.data.local.CartItem
import kotlinx.coroutines.launch

private const val DELIVERY_FEE_RS = 250.0
private const val FREE_DELIVERY_THRESHOLD_RS = 3000.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    branchId: String,
    vm: CartViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Raw items from repo
    val rawItems by vm.cart(branchId).collectAsState(initial = emptyList())
    // Hide any zero-qty rows so ✕ (which sets qty to 0) removes the line immediately
    val items = remember(rawItems) { rawItems.filter { it.qty > 0 } }

    // Derived totals
    val subtotal by remember(items) {
        derivedStateOf { items.sumOf { it.price * it.qty } }
    }
    val deliveryFee by remember(items, subtotal) {
        derivedStateOf {
            if (items.isEmpty()) 0.0
            else if (subtotal >= FREE_DELIVERY_THRESHOLD_RS) 0.0
            else DELIVERY_FEE_RS
        }
    }
    val total by remember(subtotal, deliveryFee) {
        derivedStateOf { subtotal + deliveryFee }
    }
    val remainingToFree by remember(subtotal) {
        derivedStateOf { (FREE_DELIVERY_THRESHOLD_RS - subtotal).coerceAtLeast(0.0) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cart") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        if (items.isEmpty()) {
            EmptyCartState(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                onGoToMenu = { navController.popBackStack() }
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.localId }) { line ->
                    CartLineCard(
                        line = line,
                        onIncrement = {
                            scope.launch { vm.change(line, line.qty + 1) }
                        },
                        onDecrement = {
                            scope.launch {
                                if (line.qty > 1) {
                                    vm.change(line, line.qty - 1)
                                } else {
                                    // going to zero -> remove with UNDO
                                    val toRemove = vm.createUndoable(line)
                                    vm.change(line, 0)
                                    val res = snackbar.showSnackbar(
                                        message = "Removed ${toRemove.name}",
                                        actionLabel = "UNDO",
                                        withDismissAction = true
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        vm.change(toRemove.apply { qty = 1 }, 1)
                                    }
                                }
                            }
                        },
                        onRemove = {
                            scope.launch {
                                val toRemove = vm.createUndoable(line)
                                vm.change(line, 0) // sets to zero; UI filters it out
                                val res = snackbar.showSnackbar(
                                    message = "Removed ${toRemove.name}",
                                    actionLabel = "UNDO",
                                    withDismissAction = true
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    vm.change(toRemove.apply { qty = 1 }, 1)
                                }
                            }
                        }
                    )
                }
            }

            // Totals + CTA (sticky)
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (remainingToFree > 0.0) {
                        AssistChip(onClick = {}, label = {
                            Text("Rs. ${remainingToFree.format0()} to free delivery")
                        })
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyLarge)
                        Text("Rs. ${subtotal.format0()}", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (deliveryFee == 0.0) "FREE"
                            else "Rs. ${deliveryFee.format0()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Divider(Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text("Rs. ${total.format0()}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { navController.navigate("confirm/$branchId") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm delivery")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumb
            if (!line.imageUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(line.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(end = 12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍕", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    line.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = buildString {
                    append("Size: ${line.size}")
                    if (!line.extrasCsv.isNullOrBlank()) {
                        append("  •  Extras: ${line.extrasCsv}")
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(sub, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rs. ${line.price.format0()} × ${line.qty}", style = MaterialTheme.typography.bodyLarge)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(onClick = onDecrement) { Text("−") }
                        Spacer(Modifier.width(8.dp))
                        Text("${line.qty}", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        OutlinedIconButton(onClick = onIncrement) { Text("+") }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCartState(modifier: Modifier = Modifier, onGoToMenu: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Your cart is empty.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGoToMenu) { Text("Browse menu") }
        }
    }
}

private fun Double.format0(): String = String.format("%.2f", this)
