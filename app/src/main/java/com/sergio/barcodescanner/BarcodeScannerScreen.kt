package com.sergio.barcodescanner

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen() {
    val context = LocalContext.current

    val barcodeList = remember { mutableStateListOf<BarcodeItem>() }
    val selectAllState = remember {
        derivedStateOf<SelectAllState> {
            when {
                barcodeList.isEmpty() -> SelectAllState.Unchecked
                barcodeList.all { it.isSelected } -> SelectAllState.Checked
                barcodeList.any { it.isSelected } -> SelectAllState.Indeterminate
                else -> SelectAllState.Unchecked
            }
        }
    }
    val hasSelection = remember { derivedStateOf<Boolean> { barcodeList.any { it.isSelected } } }
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
        val newState = selectAllState.value != SelectAllState.Checked
        barcodeList.forEachIndexed { i, item ->
            barcodeList[i] = item.copy(isSelected = newState)
        }
    }

    val onDeleteClick = {
        val toRemove = barcodeList.filter { it.isSelected }.toList()
        toRemove.forEach { item ->
            item.imagePath?.let { path ->
                context.filesDir.resolve(path).delete()
            }
        }
        barcodeList.removeAll(toRemove)
        barcodeList.forEachIndexed { i, item ->
            barcodeList[i] = item.copy(isSelected = false)
        }
    }

    val activity = context as ComponentActivity
    LaunchedEffect(isCameraOpen) {
        activity.requestedOrientation = if (isCameraOpen) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Scaffold(
        topBar = {
            if (!isCameraOpen) {
                TopAppBar(
                    title = {
                        if (barcodeList.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onSelectAllClick) {
                                    val icon = when (selectAllState.value) {
                                        SelectAllState.Checked -> Icons.Default.CheckBox
                                        SelectAllState.Indeterminate -> Icons.Default.IndeterminateCheckBox
                                        else -> Icons.Default.CheckBoxOutlineBlank
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Выбрать все"
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (hasSelection.value) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onDeleteClick) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить выбранные"
                                    )
                                }
                                IconButton(onClick = {
                                    barcodeList.forEachIndexed { i, item ->
                                        barcodeList[i] = item.copy(isSelected = false)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Отменить выбор"
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onAddClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить штрихкод"
                            )
                        }
                        IconButton(onClick = { activity.finish() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Выход"
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
                ManualCameraScanView(
                    scannedCount = barcodeList.size,
                    onBarcodeFound = { barcodeValue, imagePath ->
                        if (barcodeList.any { it.code == barcodeValue }) {
                            Toast.makeText(
                                context,
                                "Этот штрихкод уже есть в списке",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            barcodeList.add(0, BarcodeItem(code = barcodeValue, imagePath = imagePath))
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
                        itemsIndexed(barcodeList) { index, item ->
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
                                    val currentIndex = barcodeList.indexOf(item)
                                    if (currentIndex != -1) {
                                        barcodeList[currentIndex] = item.copy(isSelected = checked)
                                    }
                                }
                            )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodyLarge
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
        BarcodeImagePreviewDialog(
            imagePath = path,
            onDismiss = { currentImagePath = null }
        )
    }
}

