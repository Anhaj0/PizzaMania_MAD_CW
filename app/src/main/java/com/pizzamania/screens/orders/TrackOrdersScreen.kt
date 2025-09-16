package com.pizzamania.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/* ---------- models ---------- */

data class UserOrder(
    val id: String,
    val status: String,
    val placedAt: Long,
    val total: Double,
    val items: List<Map<String, Any?>> = emptyList(),
    val branchId: String = ""
)

data class OrdersUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val orders: List<UserOrder> = emptyList()
)

/* ---------- ViewModel (no composite index required) ---------- */

@HiltViewModel
class TrackOrdersViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state = _state.asStateFlow()

    private var reg: ListenerRegistration? = null

    init {
        listen()
    }

    private fun listen() {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = OrdersUiState(loading = false, error = "Not signed in")
            return
        }

        // Dropped orderBy("placedAt") to avoid composite index requirement.
        val q = db.collection("orders").whereEqualTo("userId", uid)

        _state.update { it.copy(loading = true, error = null) }
        reg?.remove()
        reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                _state.value = OrdersUiState(loading = false, error = err.message)
                return@addSnapshotListener
            }

            val list = snap?.documents?.map { d ->
                UserOrder(
                    id = d.id,
                    status = d.getString("status") ?: "PLACED",
                    placedAt = d.getLong("placedAt") ?: 0L,
                    total = (d.getDouble("total") ?: 0.0),
                    items = (d.get("items") as? List<Map<String, Any?>>).orEmpty(),
                    branchId = d.getString("branchId") ?: ""
                )
            }.orEmpty()
                .sortedByDescending { it.placedAt } // client-side sort

            _state.value = OrdersUiState(loading = false, orders = list)
        }
    }

    override fun onCleared() {
        reg?.remove()
        reg = null
        super.onCleared()
    }
}

/* ---------- UI ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrdersScreen(
    navBack: () -> Unit,
    vm: TrackOrdersViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My orders") },
                navigationIcon = {
                    IconButton(onClick = navBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        when {
            s.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            s.error != null -> Text(
                "Error: ${s.error}",
                modifier = Modifier.padding(inner).padding(16.dp)
            )

            s.orders.isEmpty() -> Text(
                "No orders yet.",
                modifier = Modifier.padding(inner).padding(16.dp)
            )

            else -> {
                val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val ongoing = s.orders.filter { it.status != "DELIVERED" && it.status != "CANCELLED" }
                    val past = s.orders.filter { it.status == "DELIVERED" || it.status == "CANCELLED" }
                    items(ongoing + past, key = { it.id }) { o ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Order: ${o.id.take(8)}…",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("Placed: ${fmt.format(Date(o.placedAt))}")
                                Text("Status: ${o.status.replace('_', ' ')}")
                                Text("Total: Rs. ${"%.2f".format(o.total)}")
                                if (o.items.isNotEmpty()) {
                                    Text("Items:")
                                    o.items.forEach { itMap ->
                                        val t = itMap["title"] as? String ?: "Item"
                                        val q = (itMap["qty"] as? Number)?.toInt() ?: 1
                                        val p = (itMap["price"] as? Number)?.toDouble() ?: 0.0
                                        Text("• $t  x$q — Rs. $p")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
