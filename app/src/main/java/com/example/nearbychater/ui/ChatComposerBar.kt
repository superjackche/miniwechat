package com.example.nearbychater.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun MessageComposerBar(
        modifier: Modifier = Modifier,
        text: String,
        onTextChange: (String) -> Unit,
        onSend: () -> Unit,
        onPickPhoto: () -> Unit,
        isSending: Boolean,
        onInputFieldClick: () -> Unit = {}
) {
    // 优化：添加输入法内边距处理，确保输入框不被遮挡
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    Surface(
        color = MaterialTheme.colorScheme.surface, 
        tonalElevation = 2.dp,
        modifier = modifier // 由外层Box统一处理键盘抬升，避免重复抬升导致遮挡
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp)
                        .clickable { onInputFieldClick() },
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("发个消息…") },
                    maxLines = 4,
                    colors =
                            androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    focusedPlaceholderColor =
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedPlaceholderColor =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                            )
            )

            IconButton(onClick = onPickPhoto) {
                Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "发送图片",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSend, enabled = text.isNotBlank() && !isSending) {
                if (isSending) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
