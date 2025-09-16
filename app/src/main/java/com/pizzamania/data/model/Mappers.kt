package com.pizzamania.data.model

import com.pizzamania.screens.menu.BranchMenuItem
import com.pizzamania.screens.menu.MenuCategory

fun MenuItem.toBranchMenuItem(): BranchMenuItem {
    val cat = when (category.trim().lowercase()) {
        "sides", "side" -> MenuCategory.Sides
        "drinks", "drink", "beverage" -> MenuCategory.Drinks
        else -> MenuCategory.Pizza
    }
    return BranchMenuItem(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        basePrice = basePrice,
        category = cat
    )
}
