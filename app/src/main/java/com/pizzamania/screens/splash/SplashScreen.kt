package com.pizzamania.screens.splash

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.pizzamania.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(nav: NavController, vm: SplashViewModel = hiltViewModel()) {
    val ctx = LocalContext.current

    var asked by remember { mutableStateOf(false) }
    var permHandled by remember { mutableStateOf(false) }
    var navigated by remember { mutableStateOf(false) }

    // Hard timeout so we never get stuck on splash
    LaunchedEffect(Unit) {
        delay(1500)
        if (!permHandled) permHandled = true
    }

    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permHandled = true
    }

    // Ask for location once, FIRST
    LaunchedEffect(Unit) {
        if (!asked) {
            asked = true
            val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
                locLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                permHandled = true
            }
        }
    }

    // After permission flow, if logged in go to menu, else go to login
    LaunchedEffect(permHandled) {
        if (permHandled && !navigated) {
            navigated = true
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                nav.navigate(Routes.Auth) { popUpTo(Routes.Splash) { inclusive = true } }
            } else {
                val id = runCatching { vm.nearestBranchId() }.getOrNull()
                if (id != null) {
                    nav.navigate("menu/$id") { popUpTo(Routes.Splash) { inclusive = true } }
                } else {
                    nav.navigate(Routes.Auth) { popUpTo(Routes.Splash) { inclusive = true } }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Getting your location…")
        }
    }
}
