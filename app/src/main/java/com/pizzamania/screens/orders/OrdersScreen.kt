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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(vm: OrdersViewModel = hiltViewModel()) {
    val orders = remember { mutableStateListOf<Order>() }

    LaunchedEffect(Unit) {
        vm.myOrders.collectLatest { list ->
            orders.clear(); orders.addAll(list)
        }
    }

    Scaffold { inner ->
        if (orders.isEmpty()) {
            Box(
                Modifier.padding(inner).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("No orders yet") }
        } else {
            LazyColumn(
                modifier = Modifier.padding(inner).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderCard(order)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Order #${shortId(order.id)}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                StatusChip(order.status)
            }

            Text(
                "${formatDate(order.placedAt)} • Rs. ${"%,.0f".format(order.total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val names = order.items.joinToString(", ") { it.title }
            Text(names, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Divider(Modifier.padding(vertical = 8.dp))

            OrderTimeline(order.status)
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

@Composable
private fun OrderTimeline(status: OrderStatus) {
    val steps = listOf("Placed", "Preparing", "Out for delivery", "Delivered")
    val idx = when (status) {
        OrderStatus.PLACED -> 0
        OrderStatus.PREPARING -> 1
        OrderStatus.OUT_FOR_DELIVERY -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.CANCELLED -> -1
    }

    if (status == OrderStatus.CANCELLED) {
        Text("Order cancelled", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { i, title ->
            val done = i <= idx
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (done) "●" else "○")
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(ms: Long): String =
    try { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(ms)) } catch (_: Throwable) { "" }

private fun shortId(id: String?): String = id?.takeLast(6) ?: "—"
