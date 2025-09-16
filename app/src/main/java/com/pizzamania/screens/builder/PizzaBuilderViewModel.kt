package com.pizzamania.screens.builder

import androidx.lifecycle.ViewModel
import com.pizzamania.data.model.Topping
import com.pizzamania.data.repo.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PizzaBuilderViewModel @Inject constructor(
    private val cartRepo: CartRepository
) : ViewModel() {

    suspend fun addCustomPizza(
        branchId: String,
        basePrice: Double,
        selections: List<Topping>,   // chosen toppings
        size: String = "M",
        qty: Int = 1
    ) {
        val extrasCsv = selections.joinToString(",") { it.name }.ifBlank { null }
        val unitPrice = basePrice + selections.sumOf { it.price }

        cartRepo.addOrIncrement(
            branchId = branchId,
            itemId = "custom-pizza",
            name = "Custom Pizza",
            computedUnitPrice = unitPrice,
            imageUrl = null,
            qty = qty,
            size = size,
            extrasCsv = extrasCsv
        )
    }
}
