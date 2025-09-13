package com.pizzamania.screens.auth

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pizzamania.navigation.Routes
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(nav: NavController, vm: AuthViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // LOGIN form
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val isEmailValid by derivedStateOf { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }

    // SIGNUP collapsed section
    var signUpOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Prefill signup address when section opens
    LaunchedEffect(signUpOpen) {
        if (signUpOpen && address.isBlank()) {
            vm.prefillAddressFromLocation()?.let { address = it }
        }
    }

    // Ask notifications on Android 13+
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun goToNearestMenu() = scope.launch {
        val id = vm.nearestBranchId()
        if (id != null) {
            nav.navigate("menu/$id") {
                popUpTo(Routes.Splash) { inclusive = true } // clear back stack
                launchSingleTop = true
            }
        } else {
            nav.navigate(Routes.Home) {
                popUpTo(Routes.Auth) { inclusive = true }
            }
            snack.showSnackbar("Could not find nearest branch.")
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar({ Text("Log in to PizzaMania") }) },
        snackbarHost = { SnackbarHost(snack) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LOGIN
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            if (s.error != null)
                Text("Error: ${s.error}", color = MaterialTheme.colorScheme.error)

            Button(
                onClick = {
                    vm.signIn(email.trim(), pass) { user ->
                        if (user == null) return@signIn
                        if (user.role == "ADMIN") {
                            nav.navigate(Routes.AdminBranches) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        } else {
                            scope.launch { snack.showSnackbar("Hello, ${user.name}") }
                            goToNearestMenu()
                        }
                    }
                },
                enabled = !s.loading && isEmailValid && pass.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (s.loading) "Signing in..." else "Sign In") }

            // SIGNUP COLLAPSIBLE
            TextButton(onClick = { signUpOpen = !signUpOpen }, modifier = Modifier.fillMaxWidth()) {
                Text(if (signUpOpen) "Have an account? Sign in" else "New user? Sign up here")
            }
            AnimatedVisibility(signUpOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Contact name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                        label = { Text("Phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        label = { Text("Address (prefilled from location)") },
                        minLines = 3, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pass, onValueChange = { pass = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            vm.signUp(name.trim(), email.trim(), pass, phone.trim(), address.trim()) { user ->
                                if (user != null) {
                                    scope.launch { snack.showSnackbar("Welcome, ${user.name}") }
                                    goToNearestMenu()
                                }
                            }
                        },
                        enabled = !s.loading && name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()
                                && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && pass.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Create account") }
                }
            }

            // GUEST
            OutlinedButton(
                onClick = { goToNearestMenu() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue as Guest") }
        }
    }
}
