package com.pizzamania.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCustomizeTopBar(
    title: String,
    selectedTabIndex: Int,        // 0 = Menu, 1 = Customize
    onBack: () -> Unit,
    onSelectMenu: () -> Unit,
    onSelectCustomize: () -> Unit,
    actions: (@Composable () -> Unit)? = null
) {
    Column {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = { Text(title) },
            actions = { actions?.invoke() }
        )
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = onSelectMenu,
                text = { Text("Menu") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = onSelectCustomize,
                text = { Text("Customize") }
            )
        }
    }
}
