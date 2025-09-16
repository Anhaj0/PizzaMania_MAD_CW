package com.pizzamania.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pizzamania.data.model.Order
import com.pizzamania.data.model.OrderStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminOrdersScreen(vm: AdminOrdersViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val all = remember { mutableStateListOf<Order>() }
    var selected by remember { mutableStateOf(FilterTab.All) }

    LaunchedEffect(Unit) {
        vm.allOrders.collectLatest { list ->
            all.clear(); all.addAll(list)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { inner ->
        Column(Modifier.padding(inner)) {
            FilterTabs(selected = selected, onSelected = { selected = it })
            val filtered = selected.apply(all)
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders in this view")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { order ->
                        AdminOrderCard(
                            order = order,
                            onAdvance = {
                                scope.launch {
                                    vm.advance(order)
                                    snackbar.showSnackbar("Advanced #${shortId(order.id)}")
                                }
                            },
                            onCancel = {
                                scope.launch {
                                    vm.cancel(order)
                                    snackbar.showSnackbar("Cancelled #${shortId(order.id)}")
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private enum class FilterTab(val label: String) {
    All("All"),
    Placed("Placed"),
    Preparing("Preparing"),
    Out("Out for delivery"),
    Delivered("Delivered"),
    Cancelled("Cancelled");

    fun apply(list: List<Order>): List<Order> = when (this) {
        All -> list
        Placed -> list.filter { it.status == OrderStatus.PLACED }
        Preparing -> list.filter { it.status == OrderStatus.PREPARING }
        Out -> list.filter { it.status == OrderStatus.OUT_FOR_DELIVERY }
        Delivered -> list.filter { it.status == OrderStatus.DELIVERED }
        Cancelled -> list.filter { it.status == OrderStatus.CANCELLED }
    }
}

@Composable
private fun FilterTabs(selected: FilterTab, onSelected: (FilterTab) -> Unit) {
    TabRow(selectedTabIndex = selected.ordinal) {
        FilterTab.values().forEachIndexed { idx, tab ->
            Tab(
                selected = idx == selected.ordinal,
                onClick = { onSelected(tab) },
                text = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: Order,
    onAdvance: () -> Unit,
    onCancel: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Order #${shortId(order.id)}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                StatusChip(order.status)
            }
            Text(
                "${formatDate(order.placedAt)} • Rs. ${"%,.0f".format(order.total)} • Branch ${order.branchId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val names = order.items.joinToString(", ") { it.title }
            Text(names, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Divider(Modifier.padding(vertical = 8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val canAdvance = order.status !in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
                Button(onClick = onAdvance, enabled = canAdvance, modifier = Modifier.weight(1f)) {
                    Text("Advance status")
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel order") }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val label = when (status) {
        OrderStatus.PLACED -> "Placed"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.OUT_FOR_DELIVERY -> "Out for delivery"
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.CANCELLED -> "Cancelled"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

private fun formatDate(ms: Long): String =
    try { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(ms)) } catch (_: Throwable) { "" }

private fun shortId(id: String?): String = id?.takeLast(6) ?: "—"
