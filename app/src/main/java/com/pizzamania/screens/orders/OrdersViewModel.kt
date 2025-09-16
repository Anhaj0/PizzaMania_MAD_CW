package com.pizzamania.screens.orders

import androidx.lifecycle.ViewModel
import com.pizzamania.data.model.Order
import com.pizzamania.data.repo.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepo: OrderRepository
) : ViewModel() {

    private fun uidOrEmpty(): String =
        try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
        catch (_: Throwable) { "" }

    /** Live stream of current user's orders, newest first. */
    val myOrders: Flow<List<Order>> =
        orderRepo.observeAllOrders().map { all ->
            val u = uidOrEmpty()
            all.filter { it.userId == u }.sortedByDescending { it.placedAt }
        }
}
