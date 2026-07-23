package com.sergio.barcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class BarcodeItem(
    val id: String = System.currentTimeMillis().toString(),
    val code: String,
    val imagePath: String? = null,
    var isSelected: Boolean = false
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen() {
    val context = LocalContext.current

    val barcodeList = remember { mutableStateListOf<BarcodeItem>() }
    val isAllSelected = remember { derivedStateOf { barcodeList.isNotEmpty() && barcodeList.all { it.isSelected } } }
    var currentImagePath by remember { mutableStateOf<String?>(null) }

    var isCameraOpen by remember { mutableStateOf(false) }

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

    val onAddClick = {
        if (hasCameraPermission) {
            isCameraOpen = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onSelectAllClick = {
        val newValue = !isAllSelected.value
        barcodeList.forEach { it.isSelected = newValue }
    }

    Scaffold(
        topBar = {
            if (!isCameraOpen) {
                TopAppBar(
                    title = { Text("Список штрихкодов") },
                    actions = {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAllSelected.value,
                                onCheckedChange = { onSelectAllClick() },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Выбрать все")
                            IconButton(onClick = onAddClick) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить штрихкод"
                                )
                            }
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
                ManualCameraScanView(
                    scannedCount = barcodeList.size,
                    onBarcodeFound = { barcodeValue ->
                        if (barcodeList.any { it.code == barcodeValue }) {
                            Toast.makeText(
                                context,
                                "Этот штрихкод уже есть в списке",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            barcodeList.add(0, BarcodeItem(code = barcodeValue))
                        }
                    },
                    onClose = { isCameraOpen = false }
                )
            } else {
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
                        items(barcodeList) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { checked ->
                                            item.isSelected = checked
                                        }
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = item.code,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (!item.imagePath.isNullOrBlank()) {
                                        IconButton(onClick = { currentImagePath = item.imagePath }) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Просмотреть изображение"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    currentImagePath?.let { path ->
        AlertDialog(
            onDismissRequest = { currentImagePath = null },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { currentImagePath = null }) {
                    Text("Закрыть")
                }
            },
            title = null,
            text = {
                var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(path) {
                    bitmap = BitmapFactory.decodeFile(path)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Изображение штрихкода",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
