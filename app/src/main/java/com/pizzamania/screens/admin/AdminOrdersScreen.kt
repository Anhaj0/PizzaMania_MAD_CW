package com.pizzamania.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzamania.data.model.Order
import com.pizzamania.data.model.OrderStatus
import com.pizzamania.data.repo.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class AdminOrdersViewModel @Inject constructor(
    private val repo: OrderRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders = _orders.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAllOrders().collect { _orders.value = it }
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) = viewModelScope.launch {
        repo.updateStatus(orderId, status)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(vm: AdminOrdersViewModel = hiltViewModel()) {
    val orders by vm.orders.collectAsState()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Admin • Orders") }) }
    ) { inner ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
            ) {
                Text(
                    "No orders yet",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    OrderCard(order = order, onSet = { status ->
                        vm.updateStatus(order.id, status)
                    })
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onSet: (OrderStatus) -> Unit) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val placed = fmt.format(Date(order.placedAt))

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Order: ${order.id.take(8)}…", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Placed: $placed")
            Text("Customer: ${order.delivery.name}")
            Text("Address: ${order.delivery.address}")
            Spacer(Modifier.height(6.dp))
            Text("Items:")
            order.items.forEach {
                Text("• ${it.title}  x${it.qty} — Rs. ${it.price}")
            }
            Spacer(Modifier.height(6.dp))
            Text("Total: Rs. ${order.total}")

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onSet(OrderStatus.PREPARING) },
                    label = { Text("Preparing") },
                    enabled = order.status != OrderStatus.PREPARING
                )
                AssistChip(
                    onClick = { onSet(OrderStatus.OUT_FOR_DELIVERY) },
                    label = { Text("Out for delivery") },
                    enabled = order.status != OrderStatus.OUT_FOR_DELIVERY
                )
                AssistChip(
                    onClick = { onSet(OrderStatus.DELIVERED) },
                    label = { Text("Delivered") },
                    enabled = order.status != OrderStatus.DELIVERED
                )
            }

            Spacer(Modifier.height(6.dp))
            Text("Status: ${order.status}")
        }
    }
}
