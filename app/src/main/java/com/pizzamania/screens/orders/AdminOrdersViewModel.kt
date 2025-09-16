package com.pizzamania.screens.orders

import androidx.lifecycle.ViewModel
import com.pizzamania.data.model.Order
import com.pizzamania.data.model.OrderStatus
import com.pizzamania.data.repo.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class AdminOrdersViewModel @Inject constructor(
    private val orderRepo: OrderRepository
) : ViewModel() {

    val allOrders: Flow<List<Order>> =
        orderRepo.observeAllOrders().map { it.sortedByDescending { o -> o.placedAt } }

    suspend fun setStatus(orderId: String, status: OrderStatus) {
        orderRepo.updateStatus(orderId, status)
    }

    suspend fun advance(order: Order) {
        val next = when (order.status) {
            OrderStatus.PLACED -> OrderStatus.PREPARING
            OrderStatus.PREPARING -> OrderStatus.OUT_FOR_DELIVERY
            OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
            OrderStatus.DELIVERED -> OrderStatus.DELIVERED
            OrderStatus.CANCELLED -> OrderStatus.CANCELLED
        }
        if (next != order.status) setStatus(order.id, next)
    }

    suspend fun cancel(order: Order) = setStatus(order.id, OrderStatus.CANCELLED)
}
