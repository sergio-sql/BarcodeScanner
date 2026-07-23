package com.sergio.barcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen() {
    val context = LocalContext.current

    // Список отсканированных штрихкодов
    val barcodeList = remember { mutableStateListOf<String>() }

    // Состояние: открыта ли камера в данный момент
    var isCameraOpen by remember { mutableStateOf(false) }

    // Проверка разрешения на камеру
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (isGranted) isCameraOpen = true
        }
    )

    // Функция обработки клика по кнопке "+"
    val onAddClick = {
        if (hasCameraPermission) {
            isCameraOpen = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            if (!isCameraOpen) {
                @kotlin.OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Список штрихкодов") },
                    actions = {
                        // Кнопка "+" в тулбаре
                        IconButton(onClick = onAddClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить штрихкод"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCameraOpen) PaddingValues(0.dp) else paddingValues)
        ) {
            if (isCameraOpen) {
                // Камера со счетчиком поверх превью
                ManualCameraScanView(
                    scannedCount = barcodeList.size,
                    onBarcodeFound = { barcodeValue ->
                        if (barcodeValue in barcodeList) {
                            Toast.makeText(
                                context,
                                "Этот штрихкод уже есть в списке",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            barcodeList.add(0, barcodeValue)
                        }
                    },
                    onClose = { isCameraOpen = false }
                )
            } else {
                // Главный экран: Список штрихкодов
                if (barcodeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нажмите +, чтобы отсканировать штрихкод",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(barcodeList) { code ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = code,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Превью камеры
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
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
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Верхняя панель превью
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

        // Нижняя панель сканирования
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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