package com.example.nearbychater.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nearbychater.core.model.Attachment
import com.example.nearbychater.core.model.AttachmentType

@Composable
internal fun PhotoAttachmentView(attachment: Attachment, onClick: () -> Unit) {
    // Box占位，避免图片加载时布局跳动
    // 使用Coil (AsyncImage) 异步加载图片，自动处理缓存与后台解码
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.3f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable(onClick = onClick)
    ) {
        AsyncImage(
                model =
                        ImageRequest.Builder(LocalContext.current)
                                .data(Base64.decode(attachment.dataBase64, Base64.DEFAULT))
                                .crossfade(true)
                                .build(),
                contentDescription = "图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
        )
    }
}


@Composable
internal fun PhotoPreviewDialog(attachment: Attachment, onDismiss: () -> Unit, onSave: () -> Unit) {
    // 图片预览弹窗
    // 使用Coil加载，避免手动Bitmap解码导致的OOM风险

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp.dp * 0.8f)
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 8.dp) {
            Column(
                    Modifier.padding(16.dp).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(min = 180.dp, max = maxHeight)
                                        .clip(MaterialTheme.shapes.medium)
                ) {
                    AsyncImage(
                            model =
                                    ImageRequest.Builder(LocalContext.current)
                                            .data(
                                                    Base64.decode(
                                                            attachment.dataBase64,
                                                            Base64.DEFAULT
                                                    )
                                            )
                                            .build(),
                            contentDescription = "图片预览",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                    )
                }
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onSave) { Text("保存到相册") }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

internal fun formatMessageTimestamp(time: Long): String {
    if (time == 0L) return ""
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(time)
}

internal fun saveAttachmentToGallery(context: Context, attachment: Attachment): Boolean {
    if (attachment.type != AttachmentType.PHOTO) return false
    val mime = attachment.mimeType.ifBlank { "image/jpeg" }
    val extension = if (mime.contains("png", ignoreCase = true)) "png" else "jpg"
    val name = "NearbyChater_${System.currentTimeMillis()}.$extension"
    val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val resolver = context.contentResolver
    val pendingValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    return runCatching {
        val uri = resolver.insert(imageCollection, pendingValues) ?: return@runCatching false
        resolver.openOutputStream(uri)?.use { stream ->
            val data = Base64.decode(attachment.dataBase64, Base64.DEFAULT)
            stream.write(data)
        } ?: return@runCatching false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val readyValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, readyValues, null, null)
        }
        true
    }.getOrElse { false }
}
