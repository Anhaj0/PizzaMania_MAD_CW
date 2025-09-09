package com.pizzamania.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pizzamania.data.model.MenuItem
import com.pizzamania.data.repo.MenuRepository
import com.pizzamania.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminMenusViewModel @Inject constructor(
    private val repo: MenuRepository
) : ViewModel() {
    var items by mutableStateOf<List<MenuItem>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun listen(branchId: String) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                repo.listenMenu(branchId).collectLatest {
                    items = it
                    error = null
                }
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenusScreen(
    nav: NavController,
    branchId: String,
    vm: AdminMenusViewModel = hiltViewModel()
) {
    LaunchedEffect(branchId) { vm.listen(branchId) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Admin • Menus") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("admin/branch/$branchId/menu/new") }) {
                Text("+")
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            if (vm.error != null) {
                Text("Error: ${vm.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.items) { m ->
                    Card(onClick = {
                        nav.navigate("admin/branch/$branchId/menu/${m.id}")
                    }) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AsyncImage(
                                model = m.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.titleMedium)
                                Text("Rs. ${m.price}", style = MaterialTheme.typography.bodyMedium)
                                if (!m.available) Text("Unavailable", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
