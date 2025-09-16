package com.pizzamania.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.pizzamania.data.repo.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MenuCartBarViewModel @Inject constructor(
    private val cartRepo: CartRepository
) : ViewModel() {

    /** Observe total count + subtotal for a branch. */
    fun state(branchId: String) = cartRepo.observeCart(branchId)
        .map { items ->
            val count = items.sumOf { it.qty }
            val subtotal = items.sumOf { it.price * it.qty }
            MiniCartState(count = count, subtotal = subtotal)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiniCartState())

    data class MiniCartState(val count: Int = 0, val subtotal: Double = 0.0)
}

/** Bottom bar that appears only when there are items in the cart.
 *  Adds a tiny "hold" after count drops to 0 to prevent flicker. */
@Composable
fun MiniCartBar(
    navController: NavHostController,
    branchId: String,
    vm: MenuCartBarViewModel = hiltViewModel()
) {
    val raw by vm.state(branchId).collectAsState()

    // Hold visibility for a short time after going to zero (prevents blink during updates)
    var holdUntil by remember { mutableStateOf(0L) }
    val now = System.currentTimeMillis()
    val visible = if (raw.count > 0) {
        holdUntil = now + 1200L // 1.2s hold
        true
    } else {
        now < holdUntil
    }

    if (!visible) return

    Surface(tonalElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${raw.count} item${if (raw.count == 1) "" else "s"} in cart",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Rs. ${raw.subtotal.format0()}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(onClick = { navController.navigate(com.pizzamania.navigation.Routes.Cart(branchId)) }) {
                Text("View cart")
            }
        }
    }
}

private fun Double.format0(): String = String.format("%.2f", this)
