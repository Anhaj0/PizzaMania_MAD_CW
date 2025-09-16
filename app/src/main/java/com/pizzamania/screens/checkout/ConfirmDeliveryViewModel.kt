package com.pizzamania.screens.checkout

import androidx.lifecycle.ViewModel
import com.pizzamania.data.local.CartItem
import com.pizzamania.data.model.DeliveryDetails
import com.pizzamania.data.model.Order
import com.pizzamania.data.model.OrderItem
import com.pizzamania.data.model.OrderStatus
import com.pizzamania.data.model.UserProfile
import com.pizzamania.data.repo.CartRepository
import com.pizzamania.data.repo.OrderRepository
import com.pizzamania.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
class ConfirmDeliveryViewModel @Inject constructor(
    private val cartRepo: CartRepository,
    private val orderRepo: OrderRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    suspend fun loadCartOnce(branchId: String): List<CartItem> =
        cartRepo.observeCart(branchId).first()

    suspend fun clearCart(branchId: String) = cartRepo.clearBranch(branchId)

    suspend fun loadProfile(): UserProfile? {
        val uid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Throwable) { null }
        return profileRepo.get(uid)
    }

    suspend fun saveProfileIfChanged(name: String, phone: String, address: String) {
        val uid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Throwable) { null }
        if (uid.isNullOrBlank()) return
        val cur = profileRepo.get(uid)
        if (cur?.name == name && cur.phone == phone && cur.address == address) return
        profileRepo.save(UserProfile(uid = uid, name = name, phone = phone, address = address))
    }

    suspend fun placeOrder(
        branchId: String,
        name: String,
        phone: String,
        address: String,
        notes: String?,
        items: List<CartItem>
    ): String {
        val now = System.currentTimeMillis()
        val subtotal = items.sumOf { it.price * it.qty }
        val deliveryFee = if (items.isEmpty() || subtotal >= 3000.0) 0.0 else 250.0

        val mapped = items.map { line ->
            OrderItem(
                itemId = line.itemId,
                title = buildString {
                    append(line.name)
                    append(" (").append(line.size).append(")")
                    if (!line.extrasCsv.isNullOrBlank()) append(" + ").append(line.extrasCsv)
                },
                price = line.price,
                qty = line.qty
            )
        }

        val uid = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" } catch (_: Throwable) { "" }

        val order = Order(
            id = "",
            userId = uid,
            branchId = branchId,
            items = mapped,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            total = subtotal + deliveryFee,
            delivery = DeliveryDetails(name = name, address = address, phone = phone),
            status = OrderStatus.PLACED,
            placedAt = now,
            updatedAt = now
        )

        val orderId = orderRepo.create(order)
        if (!notes.isNullOrBlank()) {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("orders").document(orderId)
                    .update(mapOf("notes" to notes))
            } catch (_: Throwable) { /* non-fatal */ }
        }
        return orderId
    }
}
