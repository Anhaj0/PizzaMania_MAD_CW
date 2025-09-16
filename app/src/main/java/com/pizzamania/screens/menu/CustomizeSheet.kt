package com.pizzamania.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pizzamania.data.repo.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Lightweight UI model your Menu screen can pass to the sheet.
 * If your real menu model differs, just map fields into this on click.
 */
data class MenuItemUi(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val basePrice: Double,
    val branchId: String,
    val defaultSize: String = "M",
    /** Typical multipliers; override per-item if needed. */
    val sizeMultipliers: Map<String, Double> = linkedMapOf("S" to 0.90, "M" to 1.00, "L" to 1.20),
    val extras: List<Extra> = emptyList()
)

data class Extra(val name: String, val price: Double)

@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val cartRepo: CartRepository
) : ViewModel() {

    suspend fun addToCart(
        item: MenuItemUi,
        size: String,
        qty: Int,
        selectedExtras: List<Extra>
    ) {
        val multiplier = item.sizeMultipliers[size] ?: 1.0
        val extrasSum = selectedExtras.sumOf { it.price }
        val unitPrice = ((item.basePrice * multiplier) + extrasSum).round2()
        val extrasCsv = selectedExtras.joinToString(",") { it.name }.ifBlank { null }

        cartRepo.addOrIncrement(
            branchId = item.branchId,
            itemId = item.id,
            name = item.title,              // Note: Orders screen reconstructs size/extras from title+extrasCsv
            computedUnitPrice = unitPrice,
            imageUrl = item.imageUrl,
            qty = qty,
            size = size,
            extrasCsv = extrasCsv
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBottomSheet(
    item: MenuItemUi,
    onDismiss: () -> Unit,
    vm: CustomizeViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var size by remember(item) { mutableStateOf(item.defaultSize.takeIf { it in item.sizeMultipliers.keys } ?: item.sizeMultipliers.keys.first()) }
    var qty by remember { mutableStateOf(1) }
    var selected by remember(item) { mutableStateOf(setOf<String>()) }

    val selectedExtras = remember(selected, item) {
        item.extras.filter { it.name in selected }
    }

    val liveUnit = remember(item, size, selectedExtras) {
        val multiplier = item.sizeMultipliers[size] ?: 1.0
        val extrasSum = selectedExtras.sumOf { it.price }
        ((item.basePrice * multiplier) + extrasSum).round2()
    }
    val liveTotal = remember(liveUnit, qty) { (liveUnit * qty).round2() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // Top block: image + title + base
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!item.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.description!!, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text("From Rs. ${item.basePrice.format0()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Divider()

        // Size selector
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Size", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(item.sizeMultipliers.entries.toList()) { (label, mult) ->
                    FilterChip(
                        selected = size == label,
                        onClick = { size = label },
                        label = {
                            val price = (item.basePrice * mult).round2()
                            Text("$label  •  Rs. ${price.format0()}")
                        }
                    )
                }
            }
        }

        // Extras selector (optional)
        if (item.extras.isNotEmpty()) {
            Divider()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Extras", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.extras) { ex ->
                        FilterChip(
                            selected = ex.name in selected,
                            onClick = {
                                selected = if (ex.name in selected) selected - ex.name else selected + ex.name
                            },
                            label = { Text("${ex.name}  •  +Rs. ${ex.price.format0()}") }
                        )
                    }
                }
            }
        }

        // Quantity + running totals
        Divider()
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Quantity", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(onClick = { if (qty > 1) qty-- }) { Text("−") }
                    Spacer(Modifier.width(12.dp))
                    Text("$qty", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(12.dp))
                    OutlinedIconButton(onClick = { qty++ }) { Text("+") }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Unit: Rs. ${liveUnit.format0()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Total: Rs. ${liveTotal.format0()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Sticky CTA inside sheet
        Surface(tonalElevation = 3.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            vm.addToCart(item, size, qty, selectedExtras)
                            // We can't show a snackbar here that persists after dismiss
                            // Best to show it on the menu screen after the sheet closes
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to cart — Rs. ${liveTotal.format0()}")
                }
            }
        }

        // Local snackbar host for feedback inside sheet
        SnackbarHost(hostState = snackbar, modifier = Modifier.padding(bottom = 8.dp))
    }
}

// --- helpers ---
private fun Double.round2(): Double = round(this * 100.0) / 100.0
private fun Double.format0(): String = String.format("%.2f", this)