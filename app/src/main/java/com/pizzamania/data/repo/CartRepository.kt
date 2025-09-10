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

    suspend fun addOrIncrement(
        branchId: String,
        itemId: String,
        name: String,
        price: Double,
        imageUrl: String?,
        qty: Int
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getItem(branchId, itemId)
        if (existing == null) {
            dao.insert(CartItem(itemId, branchId, name, price, qty, imageUrl))
        } else {
            existing.qty = existing.qty + qty
            existing.price = price
            existing.name = name
            existing.imageUrl = imageUrl
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
