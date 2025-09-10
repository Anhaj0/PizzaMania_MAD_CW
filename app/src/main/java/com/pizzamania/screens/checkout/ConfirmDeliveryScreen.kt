package com.pizzamania.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.data.local.CartItem
import com.pizzamania.data.repo.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val cart: CartRepository
) : androidx.lifecycle.ViewModel() {

    suspend fun submit(branchId: String, name: String, address: String, phone: String) {
        val items: List<CartItem> = cart.observeCart(branchId).first()
        val subtotal = items.sumOf { it.price * it.qty }
        val now = System.currentTimeMillis()

        val order = hashMapOf(
            "branchId" to branchId,
            "items" to items.map {
                mapOf(
                    "itemId" to it.itemId,
                    "title"  to it.name,
                    "price"  to it.price,
                    "qty"    to it.qty
                )
            },
            "subtotal"    to subtotal,
            "deliveryFee" to 0.0,
            "total"       to subtotal,
            "delivery"    to mapOf("name" to name, "address" to address, "phone" to phone),
            "status"      to "PLACED",
            "placedAt"    to now,
            "updatedAt"   to now
        )

        db.collection("orders").add(order)
        cart.clearBranch(branchId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeliveryScreen(
    navController: NavController,
    branchId: String,
    vm: ConfirmViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Delivery details") }) },
        bottomBar = {
            Button(
                onClick = {
                    submitting = true
                    scope.launch {
                        try {
                            vm.submit(branchId, name, address, phone)
                            navController.popBackStack(route = "home", inclusive = false)
                        } finally { submitting = false }
                    }
                },
                enabled = name.isNotBlank() && address.isNotBlank() && phone.isNotBlank() && !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) { Text(if (submitting) "Submitting..." else "Place order") }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(scroll)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}
