package com.pizzamania.data.model

data class OrderItem(
    val itemId: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val qty: Int = 1
)

data class DeliveryDetails(
    val name: String = "",
    val address: String = "",
    val phone: String = ""
)

enum class OrderStatus { PLACED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }

data class Order(
    val id: String = "",
    val userId: String = "",
    val branchId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val delivery: DeliveryDetails = DeliveryDetails(),
    val status: OrderStatus = OrderStatus.PLACED,
    // store as epoch millis for simplicity
    val placedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
