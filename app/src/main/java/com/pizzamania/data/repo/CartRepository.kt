package com.pizzamania.data.repo

import com.pizzamania.data.local.CartDao
import com.pizzamania.data.local.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val dao: CartDao
) {
    fun observeCart(branchId: String): Flow<List<CartItem>> = dao.observeCart(branchId)

    /**
     * Adds to cart. If the SAME item with SAME options already exists, increments its qty.
     */
    suspend fun addOrIncrement(
        branchId: String,
        itemId: String,
        name: String,
        computedUnitPrice: Double,
        imageUrl: String?,
        qty: Int,
        size: String,
        extrasCsv: String?
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getItemWithOptions(branchId, itemId, size, extrasCsv ?: "")
        if (existing == null) {
            dao.insert(CartItem(itemId, branchId, name, computedUnitPrice, qty, imageUrl, size, extrasCsv))
        } else {
            existing.qty = existing.qty + qty
            existing.price = computedUnitPrice // keep last computed unit price
            dao.update(existing)
        }
    }

    suspend fun changeQty(item: CartItem, newQty: Int) = withContext(Dispatchers.IO) {
        if (newQty <= 0) dao.delete(item) else {
            item.qty = newQty
            dao.update(item)
        }
    }

    suspend fun clearBranch(branchId: String) = withContext(Dispatchers.IO) {
        dao.clearBranch(branchId)
    }
}
