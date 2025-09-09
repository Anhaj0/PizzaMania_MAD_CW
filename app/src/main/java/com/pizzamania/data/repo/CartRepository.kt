package com.pizzamania.data.repo

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val dao: CartDao
) {
    fun observeCart(branchId: String): Flow<List<CartItem>> = dao.observeCart(branchId)

    suspend fun addOrIncrement(branchId: String, itemId: String, name: String, price: Long, imageUrl: String?) {
        val existing = dao.getItem(branchId, itemId)
        if (existing == null) {
            dao.insert(CartItem(branchId = branchId, itemId = itemId, name = name, price = price, qty = 1, imageUrl = imageUrl))
        } else {
            dao.update(existing.copy(qty = existing.qty + 1))
        }
    }

    suspend fun changeQty(item: CartItem, newQty: Int) {
        if (newQty <= 0) dao.delete(item) else dao.update(item.copy(qty = newQty))
    }

    suspend fun clearBranch(branchId: String) = dao.clearBranch(branchId)
}
