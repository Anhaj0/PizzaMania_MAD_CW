package com.pizzamania.data.repo

import com.pizzamania.data.model.MenuItem
import kotlinx.coroutines.flow.Flow

/** Contract for per-branch menu CRUD + realtime listening. */
interface MenuRepository {
    fun listenMenu(branchId: String): Flow<List<MenuItem>>
    suspend fun getMenuItemOnce(branchId: String, itemId: String): MenuItem?
    suspend fun addMenuItem(branchId: String, item: MenuItem)
    suspend fun updateMenuItem(branchId: String, item: MenuItem)
    suspend fun deleteMenuItem(branchId: String, itemId: String)
}
