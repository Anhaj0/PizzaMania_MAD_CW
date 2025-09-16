package com.pizzamania.screens.menu

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.pizzamania.navigation.Routes
import com.pizzamania.ui.components.PromoStrip
import com.pizzamania.ui.components.defaultPromos
import com.pizzamania.util.toDirectImageUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchMenuScreen(
    navController: NavHostController,
    branchId: String,
    vm: BranchMenuViewModel = hiltViewModel()
) {
    val snackbar = remember { SnackbarHostState() }
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity

    BackHandler { activity.finish() }

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MenuCategory.Pizza) }

    LaunchedEffect(branchId) { vm.setBranch(branchId) }
    LaunchedEffect(query) { vm.setQuery(query) }
    LaunchedEffect(selectedCategory) { vm.setCategory(selectedCategory) }

    val items by vm.uiItems.collectAsState()
    var sheetItem by remember { mutableStateOf<MenuItemUi?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Branch Menu") },
                actions = {
                    TextButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.popBackStack(0, inclusive = true)
                        navController.navigate(Routes.Auth) { launchSingleTop = true }
                    }) { Text("Sign out") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                MiniCartBar(navController = navController, branchId = branchId)
                BottomTabs(
                    navController = navController,
                    branchId = branchId,
                    selected = BottomTab.Menu
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item {
                PromoStrip(
                    promos = defaultPromos(),
                    onPromoClick = { promo ->
                        scope.launch {
                            when (promo.id) {
                                "free_delivery" -> snackbar.showSnackbar("Free delivery: Orders above Rs. 2,000")
                                "two_for_one"   -> snackbar.showSnackbar("2 for 1 on Tuesdays 5–9 PM")
                                "new_branch"    -> snackbar.showSnackbar("Now delivering from Kotte")
                            }
                        }
                    }
                )
            }

            item {
                SearchBar(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search pizzas, sides, drinks…",
                    onSearchAction = { focus.clearFocus() }
                )
            }

            item {
                CategoryChips(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            if (query.trim().isNotBlank()) {
                item {
                    Text(
                        "Results for “${query.trim()}”",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            items(items, key = { it.id }) { m ->
                MenuItemCard(
                    item = m,
                    onCustomize = {
                        sheetItem = MenuItemUi(
                            id = m.id,
                            title = m.name,
                            description = m.description,
                            imageUrl = m.imageUrl,
                            basePrice = m.basePrice,
                            branchId = branchId,
                            sizeMultipliers = linkedMapOf("S" to 0.9, "M" to 1.0, "L" to 1.2),
                            extras = listOf(
                                Extra("Extra Cheese", 200.0),
                                Extra("Olives", 150.0),
                                Extra("Mushrooms", 180.0)
                            )
                        )
                    },
                    onAdd = {
                        vm.quickAdd(branchId, m)
                        scope.launch { snackbar.showSnackbar("Added ${m.name} to cart") }
                    }
                )
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        text = "No items match your search.",
                        sub = "Try a different keyword or category."
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    sheetItem?.let { ui ->
        CustomizeBottomSheet(
            item = ui,
            onDismiss = { sheetItem = null }
        )
    }
}

/* ---------------- Bottom tabs ---------------- */

private enum class BottomTab { Menu, Build, Cart, Profile }

@Composable
private fun BottomTabs(
    navController: NavHostController,
    branchId: String,
    selected: BottomTab
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == BottomTab.Menu,
            onClick = { navController.navigate(Routes.BranchMenu(branchId)) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.LocalPizza, contentDescription = "Menu") },
            label = { Text("Menu") }
        )
        NavigationBarItem(
            selected = selected == BottomTab.Build,
            onClick = { navController.navigate(Routes.Builder(branchId)) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.Build, contentDescription = "Build") },
            label = { Text("Build") }
        )
        NavigationBarItem(
            selected = selected == BottomTab.Cart,
            onClick = { navController.navigate(Routes.Cart(branchId)) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart") },
            label = { Text("Cart") }
        )
        NavigationBarItem(
            selected = selected == BottomTab.Profile,
            onClick = { navController.navigate(Routes.Profile) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

/* ---------------- UI pieces ---------------- */

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearchAction: () -> Unit
) {
    val focus = LocalFocusManager.current
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.alpha(0.8f))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearchAction()
                        focus.clearFocus()
                    }
                ),
                decorationBox = { inner ->
                    if (value.isBlank()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(0.5f)
                        )
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun CategoryChips(
    selected: MenuCategory,
    onSelect: (MenuCategory) -> Unit
) {
    val categories = listOf(
        MenuCategory.Pizza to "Pizza",
        MenuCategory.Sides to "Sides",
        MenuCategory.Drinks to "Drinks"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (cat, label) ->
            val selectedState = cat == selected
            AssistChip(
                onClick = { onSelect(cat) },
                label = { Text(label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedState)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = if (selectedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun MenuItemCard(
    item: BranchMenuItem,
    onCustomize: () -> Unit,
    onAdd: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {

            val fixedUrl = toDirectImageUrl(item.imageUrl)
            if (!fixedUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fixedUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .alpha(0.95f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .alpha(0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalPizza, contentDescription = item.name, modifier = Modifier.size(48.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!item.description.isNullOrBlank()) {
                    Text(
                        item.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.8f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("Rs. ${item.basePrice.format0()}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.category == MenuCategory.Pizza) {
                        OutlinedButton(onClick = onCustomize) { Text("Customize") }
                    }
                    Button(onClick = onAdd) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String, sub: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (sub != null) {
            Spacer(Modifier.height(6.dp))
            Text(sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.alpha(0.7f))
        }
    }
}

private fun Double.format0(): String = String.format("%.2f", this)
