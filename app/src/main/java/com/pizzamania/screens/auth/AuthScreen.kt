package com.pizzamania.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.pizzamania.R
import com.pizzamania.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavHostController,
    authVm: AuthViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun clearError() { if (error != null) error = null }

    // Decide where to go next based on role
    suspend fun goNext() {
        val role = authVm.currentRole()
        if (role == "admin") {
            navController.popBackStack(0, inclusive = true)
            navController.navigate(Routes.AdminBranches) { launchSingleTop = true }
        } else {
            val branchId = authVm.nearestBranchId()
            val dest = branchId?.let { Routes.BranchMenu(it) } ?: Routes.Home
            navController.popBackStack(0, inclusive = true)
            navController.navigate(dest) { launchSingleTop = true }
        }
    }

    fun submit() {
        clearError()
        if (!email.isValidEmail()) { error = "Enter a valid email"; return }
        if (password.length < 6) { error = "Password must be at least 6 characters"; return }
        if (!isLogin && name.isBlank()) { error = "Enter your name"; return }

        loading = true
        scope.launch {
            try {
                val e = email.trim().lowercase()
                if (isLogin) {
                    auth.signInWithEmailAndPassword(e, password).await()
                } else {
                    auth.createUserWithEmailAndPassword(e, password).await()
                    auth.currentUser?.updateProfile(
                        userProfileChangeRequest { displayName = name.trim() }
                    )?.await()
                }
                // route by role
                goNext()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                error = t.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(if (isLogin) "Log in" else "Create account") })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_pizzamania),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            AuthModeTabs(
                isLogin = isLogin,
                onModeChange = { isLogin = it; clearError() }
            )

            Spacer(Modifier.height(20.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (!isLogin) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; clearError() },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; clearError() },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; clearError() },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val txt = if (showPassword) "Hide" else "Show"
                            TextButton(onClick = { showPassword = !showPassword }) { Text(txt) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (!loading) submit() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { submit() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isLogin) "Log in" else "Create account")
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            val e = email.trim().lowercase()
                            scope.launch {
                                loading = true
                                try {
                                    if (!e.isValidEmail()) throw IllegalArgumentException("Enter your email to reset password.")
                                    FirebaseAuth.getInstance().sendPasswordResetEmail(e).await()
                                } catch (t: Throwable) {
                                    error = t.toUserMessage()
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Forgot password?") }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Continue as guest → still role-aware (will go to Home/Menu)
            OutlinedButton(
                onClick = { scope.launch { goNext() } },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue as guest") }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/* ---------- UI bits ---------- */

@Composable
private fun AuthModeTabs(
    isLogin: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    val selectedIndex = if (isLogin) 0 else 1
    TabRow(selectedTabIndex = selectedIndex) {
        Tab(selected = selectedIndex == 0, onClick = { onModeChange(true) }, text = { Text("Log in") })
        Tab(selected = selectedIndex == 1, onClick = { onModeChange(false) }, text = { Text("Sign up") })
    }
}

/* ---------- Helpers ---------- */

private fun String.isValidEmail(): Boolean =
    isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

private fun Throwable.toUserMessage(): String {
    val e = unwrap()
    return when (e) {
        is FirebaseAuthInvalidUserException -> "No account found for that email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "That email is already in use."
        is FirebaseTooManyRequestsException -> "Too many attempts. Try again later."
        is FirebaseNetworkException -> "Can’t reach Firebase. Check your Internet."
        is FirebaseAuthException -> e.localizedMessage ?: "Authentication failed."
        is IllegalArgumentException -> e.message ?: "Invalid input."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }
}

private fun Throwable.unwrap(): Throwable {
    var cur: Throwable = this
    while (cur.cause != null && cur.cause !== cur) cur = cur.cause!!
    return cur
}
