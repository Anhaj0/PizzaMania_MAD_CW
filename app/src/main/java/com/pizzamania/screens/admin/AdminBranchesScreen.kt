package com.pizzamania.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.pizzamania.data.model.Branch
import com.pizzamania.data.repo.BranchRepository
import com.pizzamania.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminBranchesViewModel @Inject constructor(
    private val repo: BranchRepository
) : ViewModel() {
    var items by mutableStateOf<List<Branch>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun load() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                items = repo.fetchBranches()
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBranchesScreen(
    navController: NavController,
    vm: AdminBranchesViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin • Branches") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.AdminOrders) }) {
                        Icon(Icons.Filled.List, contentDescription = "Orders")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate(Routes.AdminBranchNew) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) { Text("New branch") }
        }
    ) { inner ->
        when {
            vm.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
            ) { CircularProgressIndicator(Modifier.padding(24.dp)) }

            vm.error != null -> Text(
                "Error: ${vm.error}",
                modifier = Modifier
                    .padding(inner)
                    .padding(16.dp)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                items(vm.items) { b ->
                    ListItem(
                        headlineContent = { Text(b.name) },
                        supportingContent = { Text(b.address) },
                        trailingContent = {
                            TextButton(
                                onClick = { navController.navigate("admin/branch/${b.id}") }
                            ) { Text("Edit") }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("admin/branch/${b.id}/menus")
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
