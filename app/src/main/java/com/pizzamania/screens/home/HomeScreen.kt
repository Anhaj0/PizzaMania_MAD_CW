package com.pizzamania.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pizzamania.navigation.Routes
import kotlinx.coroutines.tasks.await

@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var hello by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                doc.getString("name")?.let { if (it.isNotBlank()) hello = "Hello, $it" }
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            vm.findNearest()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) vm.findNearest()
    }

    val nearest = state.nearest

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        hello?.let {
            Text(it, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> CircularProgressIndicator()

            nearest != null -> {
                Text("Nearest branch:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(nearest.name)
                state.distanceKm?.let { Text(String.format("Distance: %.2f km", it)) }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.navigate(Routes.BranchMenu(nearest.id)) }) {
                    Text("View ${nearest.name} menu")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { navController.navigate("cart/${nearest.id}") }) {
                    Text("Open cart")
                }
            }

            else -> {
                Text(state.error ?: "Tap to find nearest branch")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) { Text("Use my location") }

                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.findNearest() }) { Text("Retry") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { navController.navigate(Routes.Auth) }) {
            Text("Admin login")
        }
    }
}
