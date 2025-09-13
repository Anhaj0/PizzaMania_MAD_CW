package com.pizzamania.screens.checkout

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.pizzamania.data.local.CartItem
import com.pizzamania.data.repo.CartRepository
import com.pizzamania.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ConfirmViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val cart: CartRepository,
    private val auth: FirebaseAuth
) : androidx.lifecycle.ViewModel() {

    suspend fun loadProfile(): Triple<String?, String?, String?>? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("users").document(uid).get().await()
        return Triple(doc.getString("name"), doc.getString("phone"), doc.getString("address"))
    }

    suspend fun submit(branchId: String, name: String, address: String, phone: String) {
        val items: List<CartItem> = cart.observeCart(branchId).first()
        val subtotal = items.sumOf { it.price * it.qty }
        val now = System.currentTimeMillis()
        val uid = auth.currentUser?.uid
        val token = try { FirebaseMessaging.getInstance().token.await() } catch (_: Exception) { null }

        val order = hashMapOf(
            "id" to "",
            "userId" to (uid ?: ""),
            "branchId" to branchId,
            "items" to items.map {
                mapOf("itemId" to it.itemId, "title" to it.name, "price" to it.price, "qty" to it.qty)
            },
            "subtotal" to subtotal,
            "deliveryFee" to 0.0,
            "total" to subtotal,
            "delivery" to mapOf("name" to name, "address" to address, "phone" to phone),
            "status" to "PLACED",
            "placedAt" to now,
            "updatedAt" to now,
            "fcmToken" to (token ?: "")
        )

        db.collection("orders").add(order).await()
        if (uid != null) {
            db.collection("users").document(uid).set(
                mapOf("name" to name, "address" to address, "phone" to phone),
                SetOptions.merge()
            ).await()
        }
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
    val ctx = LocalContext.current

    // Prefill from saved profile
    LaunchedEffect(Unit) {
        vm.loadProfile()?.let { (n, p, a) ->
            name = n ?: ""
            phone = p ?: ""
            address = a ?: ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Delivery details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    submitting = true
                    scope.launch {
                        try {
                            vm.submit(branchId, name, address, phone)
                            Toast.makeText(ctx, "Delivering soon!", Toast.LENGTH_SHORT).show()
                            // after placing order, go to Orders tracking screen
                            navController.navigate(Routes.MyOrders) {
                                popUpTo(Routes.Splash) { inclusive = false }
                                launchSingleTop = true
                            }
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
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Address") }, minLines = 3, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                label = { Text("Phone") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}
