package com.pizzamania.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CartBadgeViewModel @Inject constructor(
    private val cartRepository: com.pizzamania.data.repo.CartRepository
) : ViewModel() {

    private val branchId = MutableStateFlow<String?>(null)

    // Sum quantities across lines so the badge reflects total items
    val cartCount: StateFlow<Int> = branchId
        .flatMapLatest { b ->
            if (b.isNullOrBlank()) flowOf(0)
            else cartRepository.observeCart(b).map { items ->
                items.sumOf { it.qty }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setBranch(id: String?) {
        branchId.value = id
    }
}
