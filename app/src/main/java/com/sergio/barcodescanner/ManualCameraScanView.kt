package com.sergio.barcodescanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.MediaActionSound
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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

private fun cropToBoundingBox(
    context: android.content.Context,
    originalPath: String,
    boundingBox: Rect,
    rotationDegrees: Int
): String? {
    val bitmap = BitmapFactory.decodeFile(originalPath) ?: return null
    
    val alignedBitmap = when (rotationDegrees) {
        90, 270 -> {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        180 -> {
            val matrix = Matrix().apply { postRotate(180f) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        else -> bitmap
    }
    
    val left = boundingBox.left.coerceIn(0, alignedBitmap.width)
    val top = boundingBox.top.coerceIn(0, alignedBitmap.height)
    val right = boundingBox.right.coerceIn(left, alignedBitmap.width)
    val bottom = boundingBox.bottom.coerceIn(top, alignedBitmap.height)
    
    if (right <= left || bottom <= top) return null
    
    val cropped = Bitmap.createBitmap(alignedBitmap, left, top, right - left, bottom - top)
    
    val picturesDir = File(context.filesDir, "barcode_images")
    if (!picturesDir.exists()) picturesDir.mkdirs()
    val croppedFile = File(picturesDir, "crop_${System.currentTimeMillis()}.jpg")
    
    FileOutputStream(croppedFile).use { out ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    
    if (alignedBitmap != bitmap) alignedBitmap.recycle()
    bitmap.recycle()
    File(originalPath).delete()
    
    return croppedFile.absolutePath
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun ManualCameraScanView(
    scannedCount: Int,
    initialZoomRatio: Float = 1f,
    initialExposureIndex: Int = 0,
    initialTorchEnabled: Boolean = false,
    onZoomChange: (Float) -> Unit = {},
    onExposureChange: (Int) -> Unit = {},
    onTorchChange: (Boolean) -> Unit = {},
    onBarcodeFound: (String, String?) -> Unit,
    onClose: () -> Unit,
    onAfterPhotoAction: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var isScanning by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(initialTorchEnabled) }
    var zoomRatio by remember { mutableFloatStateOf(initialZoomRatio) }
    var exposureIndex by remember { mutableIntStateOf(initialExposureIndex) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var detectedRect by remember { mutableStateOf<Rect?>(null) }
    var detectedImageSize by remember { mutableStateOf<android.util.Size?>(null) }
    var detectedRotation by remember { mutableIntStateOf(0) }
    var scanArea by remember { mutableStateOf<Rect?>(null) }
    var crosshairArea by remember { mutableStateOf<Rect?>(null) }
    var imageSize by remember { mutableStateOf<android.util.Size?>(null) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    LaunchedEffect(imageSize, rotationDegrees) {
        val w = imageSize?.width ?: return@LaunchedEffect
        val h = imageSize?.height ?: return@LaunchedEffect
        val (iw, ih) = if (rotationDegrees == 90 || rotationDegrees == 270) h to w else w to h
        val areaW = (iw * 0.5).toInt()
        val areaH = (ih * 0.4).toInt()
        val left = (iw - areaW) / 2
        val top = (ih - areaH) / 2
        scanArea = Rect(left, top, left + areaW, top + areaH)
        val crossW = (areaW * 0.3).toInt()
        val crossH = (areaH * 0.3).toInt()
        val crossLeft = left + (areaW - crossW) / 2
        val crossTop = top + (areaH - crossH) / 2
        crosshairArea = Rect(crossLeft, crossTop, crossLeft + crossW, crossTop + crossH)
    }

    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var capturedBarcode by remember { mutableStateOf<String?>(null) }
    var isPreviewOpen by remember { mutableStateOf(false) }

    fun captureBarcode() {
        if (detectedBarcode != null && !isCapturing && !isPreviewOpen) {
            isCapturing = true
            try {
                val picturesDir = File(context.filesDir, "barcode_images")
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }
                val photoFile = File(picturesDir, "${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val imagePath = output.savedUri?.toString() ?: photoFile.absolutePath
                        ContextCompat.getMainExecutor(context).execute {
                            MediaActionSound().play(MediaActionSound.SHUTTER_CLICK)
                            val code = detectedBarcode
                            val rect = detectedRect
                            val rotation = detectedRotation
                            detectedBarcode = null
                            detectedRect = null
                            detectedImageSize = null
                            detectedRotation = 0
                            if (code != null) {
                                val finalPath = if (rect != null) {
                                    cropToBoundingBox(context, imagePath, rect, rotation) ?: imagePath
                                } else {
                                    imagePath
                                }
                                capturedBarcode = code
                                capturedImagePath = finalPath
                                isPreviewOpen = true
                            }
                            isCapturing = false
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        ContextCompat.getMainExecutor(context).execute {
                            Toast.makeText(context, "Ошибка сохранения: ${exception.message}", Toast.LENGTH_SHORT).show()
                            detectedBarcode = null
                            detectedRect = null
                            detectedImageSize = null
                            detectedRotation = 0
                            isCapturing = false
                        }
                    }
                })
            } catch (e: Exception) {
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    detectedBarcode = null
                    detectedRect = null
                    isCapturing = false
                }
            }
        }
    }

    LaunchedEffect(detectedBarcode) {
        if (detectedBarcode != null) {
            captureBarcode()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.addListener({
                try {
                    cameraProviderFuture.get().unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isPreviewOpen) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 5f
                            zoomRatio = (zoomRatio * zoom).coerceIn(1f, maxZoom)
                            onZoomChange(zoomRatio)
                            camera?.cameraControl?.setZoomRatio(zoomRatio)
                        }
                    },
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            imageSize = android.util.Size(imageProxy.width, imageProxy.height)
                            rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            if (!isScanning && !isPreviewOpen) {
                                isScanning = true
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
                                                val code = firstBarcode.rawValue ?: return@addOnSuccessListener
                                                val box = firstBarcode.boundingBox
                                                val area = crosshairArea
                                                if (box != null && area != null) {
                                                    if (!Rect.intersects(box, area)) {
                                                        return@addOnSuccessListener
                                                    }
                                                }
                                                ContextCompat.getMainExecutor(context).execute {
                                                    detectedBarcode = code
                                                    detectedRect = box
                                                    detectedImageSize = imageSize
                                                    detectedRotation = rotationDegrees
                                                }
                                            } else {
                                                ContextCompat.getMainExecutor(context).execute {
                                                    detectedBarcode = null
                                                    detectedRect = null
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isScanning = false
                                            }
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                    ContextCompat.getMainExecutor(context).execute {
                                        isScanning = false
                                    }
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
                            boundCamera.cameraControl.setZoomRatio(zoomRatio)
                            boundCamera.cameraControl.setExposureCompensationIndex(exposureIndex)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))

                    previewView
                },
            )

            crosshairArea?.let { area ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val imageW = imageSize?.width?.toFloat() ?: 1f
                    val imageH = imageSize?.height?.toFloat() ?: 1f
                    val viewW = size.width
                    val viewH = size.height

                    val rotation = rotationDegrees
                    val srcW: Float
                    val srcH: Float
                    if (rotation == 90 || rotation == 270) {
                        srcW = imageH
                        srcH = imageW
                    } else {
                        srcW = imageW
                        srcH = imageH
                    }

                    val scale = kotlin.math.max(viewW / srcW, viewH / srcH)
                    val offsetX = (viewW - srcW * scale) / 2f
                    val offsetY = (viewH - srcH * scale) / 2f

                    val normalizedLeft = minOf(area.left, area.right)
                    val normalizedTop = minOf(area.top, area.bottom)
                    val normalizedRight = maxOf(area.left, area.right)
                    val normalizedBottom = maxOf(area.top, area.bottom)

                    val left = normalizedLeft * scale + offsetX
                    val top = normalizedTop * scale + offsetY
                    val right = normalizedRight * scale + offsetX
                    val bottom = normalizedBottom * scale + offsetY

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(kotlin.math.max(0f, right - left), kotlin.math.max(0f, bottom - top)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                        style = Stroke(width = 4f)
                    )

                    val centerX = (left + right) / 2f
                    val centerY = (top + bottom) / 2f
                    val lineLength = 40f

                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - lineLength, centerY),
                        end = Offset(centerX + lineLength, centerY),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX, centerY - lineLength),
                        end = Offset(centerX, centerY + lineLength),
                        strokeWidth = 4f
                    )
                }
            }

            detectedRect?.let { rect ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val imageW = detectedImageSize?.width?.toFloat() ?: 1f
                    val imageH = detectedImageSize?.height?.toFloat() ?: 1f
                    val viewW = size.width
                    val viewH = size.height

                    val rotation = detectedRotation
                    val srcW: Float
                    val srcH: Float
                    if (rotation == 90 || rotation == 270) {
                        srcW = imageH
                        srcH = imageW
                    } else {
                        srcW = imageW
                        srcH = imageH
                    }

                    val scale = kotlin.math.max(viewW / srcW, viewH / srcH)
                    val offsetX = (viewW - srcW * scale) / 2f
                    val offsetY = (viewH - srcH * scale) / 2f

                    val normalizedLeft = minOf(rect.left, rect.right)
                    val normalizedTop = minOf(rect.top, rect.bottom)
                    val normalizedRight = maxOf(rect.left, rect.right)
                    val normalizedBottom = maxOf(rect.top, rect.bottom)

                    val left = normalizedLeft * scale + offsetX
                    val top = normalizedTop * scale + offsetY
                    val right = normalizedRight * scale + offsetX
                    val bottom = normalizedBottom * scale + offsetY

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
                        onTorchChange(torchEnabled)
                        camera?.cameraControl?.enableTorch(torchEnabled)
                    }) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                            contentDescription = if (torchEnabled) "Выключить фонарик" else "Включить фонарик",
                            tint = if (torchEnabled) Color.Yellow else Color.White
                        )
                    }
                }

                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = {
                        exposureIndex = it.toInt()
                        onExposureChange(exposureIndex)
                    },
                    valueRange = -10f..10f,
                    steps = 20,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                LaunchedEffect(exposureIndex) {
                    camera?.cameraControl?.setExposureCompensationIndex(exposureIndex)
                }
            }
        }

        if (isPreviewOpen) {
            val currentBarcode = capturedBarcode
            val currentPath = capturedImagePath
            FullScreenImagePreview(
                imagePath = currentPath,
                barcode = currentBarcode,
                onSave = {
                    if (currentBarcode != null && currentPath != null) {
                        onBarcodeFound(currentBarcode, currentPath)
                        Toast.makeText(context, "Сохранено: $currentBarcode", Toast.LENGTH_SHORT).show()
                    }
                    capturedBarcode = null
                    capturedImagePath = null
                    isPreviewOpen = false
                    onAfterPhotoAction?.invoke(true)
                },
                onDismiss = {
                    currentPath?.let { File(it).delete() }
                    capturedBarcode = null
                    capturedImagePath = null
                    isPreviewOpen = false
                    onAfterPhotoAction?.invoke(false)
                }
            )
        }
    }
}
