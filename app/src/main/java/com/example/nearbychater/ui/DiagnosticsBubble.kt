package com.example.nearbychater.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nearbychater.ui.state.DiagnosticsBubbleState

@Composable
internal fun DiagnosticsBubble(
        state: DiagnosticsBubbleState,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
) {
    AnimatedVisibility(
            visible = state.isVisible && state.latestEvent != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier
    ) {
        Surface(
                tonalElevation = 8.dp, // 增加高度以获得更好的阴影效果
                shadowElevation = 8.dp, // 添加阴影使其更加突出
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f) // 使用错误容器颜色并增加透明度
        ) {
            Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 添加错误图标以更好地指示这是错误/诊断消息
                Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                            text = state.latestEvent?.code ?: "", 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                            text = state.latestEvent?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss diagnostics",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
