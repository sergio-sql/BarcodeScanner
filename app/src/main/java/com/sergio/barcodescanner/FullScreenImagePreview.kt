package com.sergio.barcodescanner

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun FullScreenImagePreview(
    imagePath: String?,
    barcode: String?,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    showActions: Boolean = true
) {
    val context = LocalContext.current
    val bitmapState = remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val scaleState = remember { mutableFloatStateOf(1f) }
    val maxScaleState = remember { mutableFloatStateOf(1f) }
    val viewportSizeState = remember { mutableStateOf<Size>(Size.Zero) }
    val loadError = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imagePath) {
                            loadError.value = null
        if (imagePath != null) {
            try {
                bitmapState.value = when {
                    imagePath.startsWith("content://") || imagePath.startsWith("file://") -> {
                        val uri = android.net.Uri.parse(imagePath)
                        val stream = context.contentResolver.openInputStream(uri)
                        stream?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }
                    else -> {
                        val file = File(imagePath)
                        if (!file.exists()) {
                            throw java.io.FileNotFoundException("Файл не найден: $imagePath")
                        }
                        BitmapFactory.decodeFile(imagePath)
                    }
                }

                if (bitmapState.value == null) {
                                    loadError.value = "Не удалось декодировать изображение"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadError.value = e.message ?: "Ошибка загрузки изображения"
                bitmapState.value = null
            }
        } else {
            bitmapState.value = null
            loadError.value = "Путь к изображению отсутствует"
        }
        scaleState.value = 1f
    }

    val bitmap = bitmapState.value
    val scale = scaleState.value
    val maxScale = maxScaleState.value
    val viewportSize = viewportSizeState.value

    LaunchedEffect(bitmap, viewportSize) {
        if (bitmap != null && viewportSize.width > 0 && viewportSize.height > 0) {
            val fitScale = minOf(viewportSize.width / bitmap.width, viewportSize.height / bitmap.height)
            maxScaleState.value = (1f / fitScale).coerceAtLeast(1f)
            scaleState.value = 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Предпросмотр штрихкода",
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        viewportSizeState.value = Size(size.width.toFloat(), size.height.toFloat())
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scaleState.value = (scaleState.value * zoom).coerceIn(1f, maxScale)
                        }
                    },
                contentScale = ContentScale.Fit
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (loadError.value != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Ошибка загрузки",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Text(
                            text = loadError.value ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        Button(onClick = {
        loadError.value = null
                            imagePath?.let { path ->
                                try {
                                    bitmapState.value = when {
                                        path.startsWith("content://") || path.startsWith("file://") -> {
                                            val uri = android.net.Uri.parse(path)
                                            val stream = context.contentResolver.openInputStream(uri)
                                            stream?.use { input ->
                                                BitmapFactory.decodeStream(input)
                                            }
                                        }
                                        else -> {
                                            BitmapFactory.decodeFile(path)
                                        }
                                    }
                                    if (bitmapState.value == null) {
                    loadError.value = "Не удалось декодировать изображение"
                                    }
                                } catch (e: Exception) {
                                    loadError.value = e.message ?: "Ошибка загрузки"
                                    bitmapState.value = null
                                }
                            }
                        }) {
                            Text("Повторить")
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = barcode ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (!showActions) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (showActions && bitmap != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Отклонить")
                }
                Button(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Сохранить")
                }
            }
        }
    }
}
