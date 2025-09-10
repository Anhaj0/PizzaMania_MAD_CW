package com.pizzamania.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBranchEditScreen(
    navController: NavController,
    branchId: String?,
    vm: AdminBranchEditViewModel = hiltViewModel()
) {
    // Load or start new once per branchId
    LaunchedEffect(branchId) {
        if (branchId.isNullOrBlank()) vm.startNew() else vm.load(branchId)
    }

    // Navigate back once saved
    LaunchedEffect(vm.saved) {
        if (vm.saved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (vm.isEdit) "Edit Branch" else "New Branch") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = vm.id,
                onValueChange = { vm.id = it },
                label = { Text("ID") },
                singleLine = true,
                enabled = !vm.isEdit,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.address,
                onValueChange = { vm.address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.phone,
                onValueChange = { vm.phone = it },
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = vm.lat,
                    onValueChange = { vm.lat = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vm.lon,
                    onValueChange = { vm.lon = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Active")
                Switch(checked = vm.active, onCheckedChange = { vm.active = it })
            }

            if (vm.error != null) {
                Text(
                    text = vm.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.save() },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (vm.isEdit) "Save Changes" else "Create Branch") }

            if (vm.isEdit) {
                OutlinedButton(
                    onClick = { vm.delete() },
                    enabled = !vm.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete Branch") }
            }
        }
    }
}
