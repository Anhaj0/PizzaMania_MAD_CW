package com.pizzamania.ui.components

import com.pizzamania.R

fun defaultPromos(): List<Promo> = listOf(
    Promo(
        id = "free_delivery",
        title = "Free Delivery Weekend",
        subtitle = "Orders above Rs. 2,000",
        imageRes = R.drawable.promo_free_delivery
    ),
    Promo(
        id = "two_for_one",
        title = "2 for 1 Classic Pizzas",
        subtitle = "Every Tuesday 5–9 PM",
        imageRes = R.drawable.promo_combo_2for1
    ),
    Promo(
        id = "new_branch",
        title = "Now delivering from Kotte",
        subtitle = "Faster ETAs near you",
        imageRes = R.drawable.promo_new_branch
    )
)
