package com.pizzamania.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzamania.data.local.CartItem
import com.pizzamania.data.repo.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repo: CartRepository
) : ViewModel() {

    /** Stream the actual cart for this branch (fixes flicker/empty cart). */
    fun cart(branchId: String): Flow<List<CartItem>> = repo.observeCart(branchId)

    /**
     * Make + / − / ✕ work.
     *
     * Implementation uses addOrIncrement with a delta (can be negative).
     * If your CartRepository forbids negative deltas, swap to setQuantity/remove
     * (see commented lines).
     */
    fun change(item: CartItem, newQty: Int) {
        if (newQty == item.qty) return

        // We assume CartItem contains the needed fields (branchId, itemId, etc.)
        val delta = newQty - item.qty

        viewModelScope.launch {
            when {
                newQty <= 0 -> {
                    // Option A: set qty to zero via negative delta:
                    repo.addOrIncrement(
                        branchId = item.branchId,
                        itemId = item.itemId,
                        name = item.name,
                        computedUnitPrice = item.price,
                        imageUrl = item.imageUrl,
                        qty = -item.qty, // remove all
                        size = item.size,
                        extrasCsv = item.extrasCsv
                    )
                    // Option B (if your repo has explicit remove):
                    // repo.removeLine(item.branchId, item.localId)
                }

                delta != 0 -> {
                    // Option A: apply +1 / -1 (or any delta)
                    repo.addOrIncrement(
                        branchId = item.branchId,
                        itemId = item.itemId,
                        name = item.name,
                        computedUnitPrice = item.price,
                        imageUrl = item.imageUrl,
                        qty = delta, // can be negative to decrement
                        size = item.size,
                        extrasCsv = item.extrasCsv
                    )
                    // Option B (if you expose setQuantity):
                    // repo.setQuantity(item.branchId, item.localId, newQty)
                }
            }
        }
    }

    /** Used by the UNDO snackbar. Returning the same object is fine for now. */
    fun createUndoable(line: CartItem): CartItem = line
}
