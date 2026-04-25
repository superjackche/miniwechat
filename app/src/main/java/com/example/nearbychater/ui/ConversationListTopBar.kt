package com.example.nearbychater.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun TopBar(
        onAddMenuToggle: () -> Unit,
        onAddMenuDismiss: () -> Unit,
        onAddContact: () -> Unit,
        onCreateGroup: () -> Unit,
        onLogs: () -> Unit,
        onSettings: () -> Unit,
        isAddMenuExpanded: Boolean
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = "nearbyChater",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = onAddMenuToggle) {
                    Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加",
                            tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                        expanded = isAddMenuExpanded,
                        onDismissRequest = onAddMenuDismiss
                ) {
                    DropdownMenuItem(
                            text = { Text("添加联系人") },
                            onClick = {
                                onAddMenuDismiss()
                                onAddContact()
                            }
                    )
                    DropdownMenuItem(
                            text = { Text("创建群聊") },
                            onClick = {
                                onAddMenuDismiss()
                                onCreateGroup()
                            }
                    )
                }
            }
            IconButton(onClick = onLogs) {
                Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Logs",
                        tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFFF6F6F6)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = "搜索", color = Color(0xFF9B9B9B)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF9B9B9B)) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )
    }
}
