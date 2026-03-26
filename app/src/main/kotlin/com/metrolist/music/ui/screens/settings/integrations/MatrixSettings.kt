/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.EnableMatrixRPCKey
import com.metrolist.music.constants.MatrixAccountsKey
import com.metrolist.music.constants.MatrixStatusFormatKey
import com.metrolist.music.models.MatrixAccount
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.InfoLabel
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val matrixJsonSerializer = Json { ignoreUnknownKeys = true }

@Composable
fun MatrixAccountDialog(
    initialAccount: MatrixAccount?,
    onDismiss: () -> Unit,
    onSave: (MatrixAccount) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var homeserver by rememberSaveable { mutableStateOf(initialAccount?.homeserver ?: "") }
    var userId by rememberSaveable { mutableStateOf(initialAccount?.userId ?: "") }
    var accessToken by rememberSaveable { mutableStateOf(initialAccount?.accessToken ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAccount == null) "Add Account" else "Edit Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = homeserver,
                    onValueChange = { homeserver = it },
                    label = { Text(stringResource(R.string.matrix_homeserver)) },
                    placeholder = { Text(stringResource(R.string.matrix_homeserver_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text(stringResource(R.string.matrix_user_id)) },
                    placeholder = { Text(stringResource(R.string.matrix_user_id_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    label = { Text(stringResource(R.string.matrix_access_token)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedServer = homeserver.trim()
                    val normalizedHomeserver = if (trimmedServer.isNotBlank() && !trimmedServer.startsWith("http://", ignoreCase = true) && !trimmedServer.startsWith("https://", ignoreCase = true)) {
                        "https://$trimmedServer"
                    } else trimmedServer

                    onSave(MatrixAccount(normalizedHomeserver, userId.trim(), accessToken.trim()))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixSettings(
    navController: NavController,
) {
    val (matrixRPC, onMatrixRPCChange) = rememberPreference(EnableMatrixRPCKey, false)
    var accountsJson by rememberPreference(MatrixAccountsKey, "[]")
    var statusFormat by rememberPreference(MatrixStatusFormatKey, "")

    val accounts = remember(accountsJson) {
        try {
            matrixJsonSerializer.decodeFromString<List<MatrixAccount>>(accountsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    var showStatusFormatDialog by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAdding by rememberSaveable { mutableStateOf(false) }

    if (isAdding || editingIndex != null) {
        MatrixAccountDialog(
            initialAccount = editingIndex?.let { accounts.getOrNull(it) },
            onDismiss = {
                isAdding = false
                editingIndex = null
            },
            onSave = { account ->
                val newAccounts = accounts.toMutableList()
                if (isAdding) {
                    newAccounts.add(account)
                } else {
                    editingIndex?.let { newAccounts[it] = account }
                }
                accountsJson = matrixJsonSerializer.encodeToString(newAccounts)
                isAdding = false
                editingIndex = null
            },
            onDelete = if (editingIndex != null) {
                {
                    val newAccounts = accounts.toMutableList()
                    editingIndex?.let { newAccounts.removeAt(it) }
                    accountsJson = matrixJsonSerializer.encodeToString(newAccounts)
                    editingIndex = null
                }
            } else null
        )
    }

    if (showStatusFormatDialog) {
        val defaultFormat = stringResource(R.string.matrix_status_format_default)
        TextFieldDialog(
            onDismiss = { showStatusFormatDialog = false },
            onDone = {
                statusFormat = it
                showStatusFormatDialog = false
            },
            singleLine = true,
            initialTextFieldValue = TextFieldValue(statusFormat.ifEmpty { defaultFormat }),
            extraContent = {
                InfoLabel(text = stringResource(R.string.matrix_status_format_description))
            },
        )
    }

    Column(
        modifier = Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.matrix_information_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        val accountItems = accounts.mapIndexed { index, account ->
            Material3SettingsItem(
                title = { Text(account.homeserver.ifEmpty { "New Matrix Server" }) },
                description = { Text(account.userId) },
                onClick = { editingIndex = index }
            )
        }.toMutableList()

        if (accounts.size < 3) {
            accountItems.add(
                Material3SettingsItem(
                    title = { Text("Add Account") },
                    onClick = { isAdding = true }
                )
            )
        }

        Material3SettingsGroup(
            title = stringResource(R.string.matrix_integration),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.enable_matrix_rpc)) },
                    trailingContent = {
                        Switch(
                            checked = matrixRPC,
                            onCheckedChange = onMatrixRPCChange,
                        )
                    },
                    onClick = {
                        onMatrixRPCChange(!matrixRPC)
                    },
                )
            ) + accountItems
        )

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = matrixRPC) {
            Column(modifier = Modifier.animateContentSize()) {
                Material3SettingsGroup(
                    title = stringResource(R.string.options),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.matrix_status_format)) },
                            description = {
                                Text(statusFormat.ifEmpty { stringResource(R.string.matrix_status_format_default) })
                            },
                            onClick = { showStatusFormatDialog = true },
                        )
                    )
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.matrix_status_format_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.matrix_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
