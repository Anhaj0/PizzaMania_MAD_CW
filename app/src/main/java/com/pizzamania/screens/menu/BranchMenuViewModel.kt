package com.pizzamania.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzamania.data.repo.CartRepository
import com.pizzamania.data.repo.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class BranchMenuViewModel @Inject constructor(
    private val menuRepo: MenuRepository,
    private val cartRepo: CartRepository
) : ViewModel() {

    private val branch = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(MenuCategory.Pizza)

    fun setBranch(id: String) { branch.value = id }
    fun setQuery(q: String) { query.value = q }
    fun setCategory(c: MenuCategory) { category.value = c }

    /** Expose filtered menu items for UI */
    val uiItems: StateFlow<List<BranchMenuItem>> = combine(
        branch.filterNotNull().flatMapLatest { b -> menuRepo.listenMenu(b) },
        query.debounce(200),
        category
    ) { items, q, cat ->
        val ql = q.trim().lowercase()
        items
            .map { it.toUi() }
            .filter { it.category == cat }
            .filter {
                if (ql.isBlank()) true
                else it.name.lowercase().contains(ql) || (it.description?.lowercase()?.contains(ql) == true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Quick add for non-customizable items (sides/drinks). */
    fun quickAdd(branchId: String, item: BranchMenuItem) {
        viewModelScope.launch {
            cartRepo.addOrIncrement(
                branchId = branchId,
                itemId = item.id,
                name = item.name,
                computedUnitPrice = item.basePrice,
                imageUrl = item.imageUrl,
                qty = 1,
                size = "M",
                extrasCsv = null
            )
        }
    }
}

/** UI-friendly menu item shape for this screen */
data class BranchMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val basePrice: Double,
    val category: MenuCategory,
    val imageUrl: String? = null
)

/** Map repo model → UI model (supports both title/name + category string). */
private fun com.pizzamania.data.model.MenuItem.toUi(): BranchMenuItem =
    BranchMenuItem(
        id = id ?: "",
        name = (title ?: /* legacy */ name ?: "Untitled"),
        description = description,
        basePrice = price,
        category = when ((category ?: "pizza").lowercase()) {
            "pizza" -> MenuCategory.Pizza
            "sides" -> MenuCategory.Sides
            "drinks" -> MenuCategory.Drinks
            else -> MenuCategory.Pizza
        },
        imageUrl = imageUrl
    )
