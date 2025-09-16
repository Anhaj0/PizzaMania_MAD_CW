package com.pizzamania.screens.splash

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.pizzamania.R
import com.pizzamania.navigation.Routes
import com.pizzamania.screens.auth.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavHostController,
    vm: AuthViewModel = hiltViewModel()
) {
    var play by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (play) 1f else 0.8f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splash-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (play) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splash-alpha"
    )

    val scope = rememberCoroutineScope()
    var requested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch { continueToApp(navController, vm) }
    }

    LaunchedEffect(Unit) {
        play = true
        delay(600)
        if (!requested) {
            requested = true
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= 33) { /* ignore */ }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.logo_pizzamania),
            contentDescription = "PizzaMania",
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .alpha(alpha)
        )
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(20.dp)
        )
    }
}

private suspend fun continueToApp(
    navController: NavHostController,
    vm: AuthViewModel
) {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    if (!isLoggedIn) {
        navController.popBackStack(Routes.Splash, inclusive = true)
        navController.navigate(Routes.Auth) { launchSingleTop = true }
        return
    }

    // Route by role first
    val role = vm.currentRole()
    if (role == "admin") {
        navController.popBackStack(0, inclusive = true)
        navController.navigate(Routes.AdminBranches) { launchSingleTop = true }
        return
    }

    // Otherwise go to nearest branch / home
    val branchId = vm.nearestBranchId()
    val target = branchId?.let { Routes.BranchMenu(it) } ?: Routes.Home

    navController.popBackStack(0, inclusive = true)
    navController.navigate(target) { launchSingleTop = true }
}
