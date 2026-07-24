package com.sergio.barcodescanner

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun ManualCameraScanView(
    scannedCount: Int,
    onBarcodeFound: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    var shouldScanNextFrame by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }

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
                                        val firstBarcode = barcodes.firstOrNull()?.rawValue
                                        if (firstBarcode != null) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                onBarcodeFound(firstBarcode)
                                            }
                                        } else {
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
                            imageAnalysis
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
    }
}
