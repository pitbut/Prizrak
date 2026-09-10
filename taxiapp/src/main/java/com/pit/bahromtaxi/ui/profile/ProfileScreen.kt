package com.pit.bahromtaxi.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onAccountDeleted: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Кабинет") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.loading) {
                CircularProgressIndicator()
                return@Column
            }

            viewModel.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (viewModel.saved) {
                Text(
                    "Сохранено",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            viewModel.email?.let {
                Text("Email: $it", style = MaterialTheme.typography.bodySmall)
            }
            viewModel.role?.let {
                Text(
                    "Роль: ${
                        when (it) {
                            "driver" -> "Водитель"
                            "carrier" -> "Перевозчик"
                            else -> "Пассажир"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = viewModel.nameInput,
                onValueChange = { viewModel.nameInput = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = viewModel.phoneInput,
                onValueChange = { viewModel.phoneInput = it },
                label = { Text("Номер телефона") },
                placeholder = { Text("+998901234567") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (viewModel.phoneInput.isBlank()) {
                Text(
                    "Укажите номер — по нему с вами свяжется водитель или пассажир",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { viewModel.save() },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (viewModel.saving) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Сохранить")
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !viewModel.deleting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (viewModel.deleting) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Удалить аккаунт")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить аккаунт?") },
            text = {
                Text(
                    "Аккаунт будет удалён безвозвратно, вход по этому номеру/почте станет " +
                        "снова доступен для новой регистрации. История поездок сохранится."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount(onAccountDeleted)
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
