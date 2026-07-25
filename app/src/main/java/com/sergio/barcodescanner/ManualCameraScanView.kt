package com.sergio.barcodescanner

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun ManualCameraScanView(
    scannedCount: Int,
    onBarcodeFound: (String, String?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var shouldScanNextFrame by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    var pendingBarcode by remember { mutableStateOf<String?>(null) }
    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var previewScale by remember { mutableFloatStateOf(1f) }
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var detectedRect by remember { mutableStateOf<Rect?>(null) }
    var imageSize by remember { mutableStateOf<android.util.Size?>(null) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        imageSize = android.util.Size(imageProxy.width, imageProxy.height)
                        rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        if (shouldScanNextFrame && !isProcessing) {
                            isProcessing = true
                            val mediaImage = imageProxy.image

                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val firstBarcode = barcodes.firstOrNull()
                                        if (firstBarcode != null) {
                                            detectedBarcode = firstBarcode.rawValue
                                            detectedRect = firstBarcode.boundingBox
                                            val photoFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "${System.currentTimeMillis()}.jpg")
                                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                            imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                    val imagePath = output.savedUri?.toString() ?: photoFile.absolutePath
                                                    val bitmap = BitmapFactory.decodeFile(imagePath)
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        pendingBarcode = firstBarcode.rawValue
                                                        pendingImagePath = imagePath
                                                        previewBitmap = bitmap
                                                    }
                                                }
                                                override fun onError(exception: ImageCaptureException) {
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        pendingBarcode = firstBarcode.rawValue
                                                        pendingImagePath = null
                                                        previewBitmap = null
                                                    }
                                                }
                                            })
                                        } else {
                                            detectedBarcode = null
                                            detectedRect = null
                                            ContextCompat.getMainExecutor(context).execute {
                                                Toast.makeText(
                                                    context,
                                                    "Штрихкод не найден в кадре",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        shouldScanNextFrame = false
                                        isProcessing = false
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                                shouldScanNextFrame = false
                                isProcessing = false
                            }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val boundCamera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis,
                            imageCapture
                        )
                        camera = boundCamera
                        boundCamera.cameraControl.enableTorch(torchEnabled)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        detectedRect?.let { rect ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imageW = imageSize?.width?.toFloat() ?: 1f
                val imageH = imageSize?.height?.toFloat() ?: 1f
                val viewW = size.width
                val viewH = size.height

                val rotation = rotationDegrees
                val (srcW, srcH) = if (rotation == 90 || rotation == 270) {
                    imageH to imageW
                } else {
                    imageW to imageH
                }

                val scale = kotlin.math.max(viewW / srcW, viewH / srcH)
                val offsetX = (viewW - srcW * scale) / 2f
                val offsetY = (viewH - srcH * scale) / 2f

                val normalizedLeft = minOf(rect.left, rect.right)
                val normalizedTop = minOf(rect.top, rect.bottom)
                val normalizedRight = maxOf(rect.left, rect.right)
                val normalizedBottom = maxOf(rect.top, rect.bottom)

                val rLeft: Float
                val rTop: Float
                val rRight: Float
                val rBottom: Float
                when (rotation) {
                    90 -> {
                        rLeft = imageH - normalizedBottom
                        rTop = normalizedLeft.toFloat()
                        rRight = imageH - normalizedTop
                        rBottom = normalizedRight.toFloat()
                    }
                    180 -> {
                        rLeft = imageW - normalizedRight
                        rTop = imageH - normalizedBottom
                        rRight = imageW - normalizedLeft
                        rBottom = imageH - normalizedTop
                    }
                    270 -> {
                        rLeft = normalizedTop.toFloat()
                        rTop = imageW - normalizedRight
                        rRight = normalizedBottom.toFloat()
                        rBottom = imageW - normalizedLeft
                    }
                    else -> {
                        rLeft = normalizedLeft.toFloat()
                        rTop = normalizedTop.toFloat()
                        rRight = normalizedRight.toFloat()
                        rBottom = normalizedBottom.toFloat()
                    }
                }

                val left = rLeft * scale + offsetX
                val top = rTop * scale + offsetY
                val right = rRight * scale + offsetX
                val bottom = rBottom * scale + offsetY

                drawRect(
                    color = Color.Green,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(kotlin.math.max(0f, right - left), kotlin.math.max(0f, bottom - top)),
                    style = Stroke(width = 8f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "В списке: $scannedCount",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Готово")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(onClick = {
                    torchEnabled = !torchEnabled
                    camera?.cameraControl?.enableTorch(torchEnabled)
                }) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torchEnabled) "Выключить фонарик" else "Включить фонарик",
                        tint = if (torchEnabled) Color.Yellow else Color.White
                    )
                }
                IconButton(onClick = {
                    zoomRatio = (zoomRatio - 0.5f).coerceAtLeast(1f)
                    camera?.cameraControl?.setZoomRatio(zoomRatio)
                }) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Уменьшить зум",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    val maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 5f
                    zoomRatio = (zoomRatio + 0.5f).coerceAtMost(maxZoom)
                    camera?.cameraControl?.setZoomRatio(zoomRatio)
                }) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Увеличить зум",
                        tint = Color.White
                    )
                }
            }

            Slider(
                value = exposureIndex.toFloat(),
                onValueChange = { exposureIndex = it.toInt() },
                valueRange = -10f..10f,
                steps = 20,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            LaunchedEffect(exposureIndex) {
                camera?.cameraControl?.setExposureCompensationIndex(exposureIndex)
            }

            Button(
                onClick = {
                    if (!isProcessing) {
                        shouldScanNextFrame = true
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(if (isProcessing) "Сканирование..." else "Считать штрихкод")
            }
        }

        if (pendingBarcode != null) {
            LaunchedEffect(pendingBarcode) {
                previewScale = 1f
            }
            AlertDialog(
                onDismissRequest = {
                    pendingImagePath?.let { File(it).delete() }
                    pendingBarcode = null
                    pendingImagePath = null
                    previewBitmap = null
                    previewScale = 1f
                },
                confirmButton = {
                    TextButton(onClick = {
                        val code = pendingBarcode
                        val path = pendingImagePath
                        pendingBarcode = null
                        pendingImagePath = null
                        previewBitmap = null
                        previewScale = 1f
                        if (code != null) {
                            onBarcodeFound(code, path)
                        }
                    }) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingImagePath?.let { File(it).delete() }
                        pendingBarcode = null
                        pendingImagePath = null
                        previewBitmap = null
                        previewScale = 1f
                    }) {
                        Text("Отклонить")
                    }
                },
                title = { Text("Предпросмотр") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        previewBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Предпросмотр штрихкода",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .graphicsLayer {
                                        scaleX = previewScale
                                        scaleY = previewScale
                                    }
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            previewScale = (previewScale * zoom).coerceIn(1f, 5f)
                                        }
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = pendingBarcode ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
