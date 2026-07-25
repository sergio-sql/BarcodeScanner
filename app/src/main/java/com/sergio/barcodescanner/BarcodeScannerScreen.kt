package com.sergio.barcodescanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.ArrayList
import com.sergio.barcodescanner.ui.theme.ThemeMode
import com.sergio.barcodescanner.ui.theme.ThemePreference

private fun saveBarcodeList(context: Context, list: List<BarcodeItem>) {
    val json = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("code", item.code)
        obj.put("imagePath", item.imagePath ?: JSONObject.NULL)
        json.put(obj)
    }
    val prefs = context.getSharedPreferences("barcode_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("barcode_list", json.toString()).apply()
}

private fun loadBarcodeList(context: Context): List<BarcodeItem> {
    val prefs = context.getSharedPreferences("barcode_prefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("barcode_list", null)
    val result = mutableListOf<BarcodeItem>()

    jsonStr?.let {
        val json = JSONArray(it)
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val imagePath = if (obj.isNull("imagePath")) null else obj.getString("imagePath")
            result.add(
                BarcodeItem(
                    id = obj.getString("id"),
                    code = obj.getString("code"),
                    imagePath = imagePath,
                    isSelected = false
                )
            )
        }
    }
    return result
}

private fun copyBarcodeText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("barcode", text))
    Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
}

private fun copyBarcodeImage(context: Context, imagePath: String?) {
    if (imagePath == null) {
        Toast.makeText(context, "Нет изображения", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val file = if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
            File(context.cacheDir, "shared_image_${System.currentTimeMillis()}.jpg").also { shared ->
                context.contentResolver.openInputStream(Uri.parse(imagePath))?.use { input ->
                    shared.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } else {
            File(imagePath)
        }
        if (!file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "barcode_image", uri))
        Toast.makeText(context, "Изображение скопировано", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка копирования: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareBarcodeText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться штрихкодом"))
}

private fun shareBarcodeImage(context: Context, imagePath: String?) {
    if (imagePath == null) {
        Toast.makeText(context, "Нет изображения", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val file = if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
            File(context.cacheDir, "shared_image_${System.currentTimeMillis()}.jpg").also { shared ->
                context.contentResolver.openInputStream(Uri.parse(imagePath))?.use { input ->
                    shared.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } else {
            File(imagePath)
        }
        if (!file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться изображением"))
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareSelectedImages(context: Context, items: List<BarcodeItem>) {
    if (items.isEmpty()) return
    try {
        val shareDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
        val uris = ArrayList<Uri>()

        for ((index, item) in items.withIndex()) {
            val imagePath = item.imagePath ?: continue
            val file = if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                File(shareDir, "shared_${index}_${System.currentTimeMillis()}.jpg").also { shared ->
                    context.contentResolver.openInputStream(Uri.parse(imagePath))?.use { input ->
                        shared.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } else {
                File(imagePath)
            }
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                uris.add(uri)
            }
        }

        if (uris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться изображениями"))
        } else {
            Toast.makeText(context, "Нет доступных изображений", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

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
    var viewerBarcode by remember { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(ThemePreference.getThemeMode(context)) }

    var expandedActions by remember { mutableStateOf(false) }

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
            if (isGranted) {
                isCameraOpen = true
            }
        }
    )

    LaunchedEffect(Unit) {
        if (hasCameraPermission) {
            isCameraOpen = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
        saveBarcodeList(context, barcodeList)
    }

    val activity = context as ComponentActivity
    LaunchedEffect(isCameraOpen) {
        activity.requestedOrientation = if (isCameraOpen) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(Unit) {
        val saved = loadBarcodeList(context)
        barcodeList.clear()
        barcodeList.addAll(saved)
    }

    DisposableEffect(Unit) {
        onDispose {
            saveBarcodeList(context, barcodeList)
        }
    }

    currentImagePath?.let { path ->
        FullScreenImagePreview(
            imagePath = path,
            barcode = viewerBarcode,
            onSave = {  },
            onDismiss = {
                currentImagePath = null
                viewerBarcode = null
            },
            showActions = false
        )
    }

    if (showSettings) {
        SettingsScreen(
            currentThemeMode = currentThemeMode,
            onBack = {
                showSettings = false
                isCameraOpen = false
            }
        )
    } else {
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
                                    TriStateCheckbox(
                                        state = when (selectAllState.value) {
                                            SelectAllState.Checked -> ToggleableState.On
                                            SelectAllState.Indeterminate -> ToggleableState.Indeterminate
                                            else -> ToggleableState.Off
                                        },
                                        onClick = onSelectAllClick
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onAddClick) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить штрихкод"
                                )
                            }
                            Box {
                                IconButton(onClick = { expandedActions = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Меню действий"
                                    )
                                }
                                DropdownMenu(
                                    expanded = expandedActions,
                                    onDismissRequest = { expandedActions = false }
                                ) {
                                    if (hasSelection.value) {
                                        DropdownMenuItem(
                                            text = { Text("Копировать штрихкоды") },
                                            onClick = {
                                                val selectedCodes = barcodeList.filter { it.isSelected }.joinToString("\n") { it.code }
                                                copyBarcodeText(context, selectedCodes)
                                                expandedActions = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Поделиться штрихкодами") },
                                            onClick = {
                                                val selectedCodes = barcodeList.filter { it.isSelected }.joinToString("\n") { it.code }
                                                shareBarcodeText(context, selectedCodes)
                                                expandedActions = false
                                            }
                                        )
                                        val selectedWithImages = barcodeList.filter { it.isSelected && !it.imagePath.isNullOrBlank() }
                                        if (selectedWithImages.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Поделиться изображениями") },
                                                onClick = {
                                                    shareSelectedImages(context, selectedWithImages)
                                                    expandedActions = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Удалить") },
                                            onClick = {
                                                onDeleteClick()
                                                expandedActions = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Настройки") },
                                        onClick = {
                                            expandedActions = false
                                            showSettings = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Выход") },
                                        onClick = {
                                            expandedActions = false
                                            (context as ComponentActivity).finish()
                                        }
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
                        onBarcodeFound = { barcodeValue, imagePath ->
                            if (barcodeList.any { it.code == barcodeValue }) {
                                Toast.makeText(
                                    context,
                                    "Этот штрихкод уже есть в списке",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                barcodeList.add(0, BarcodeItem(code = barcodeValue, imagePath = imagePath))
                                saveBarcodeList(context, barcodeList)
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
                                            IconButton(onClick = {
                                                val absolutePath = item.imagePath?.let { path ->
                                                    when {
                                                        path.startsWith("content://") || path.startsWith("file://") -> path
                                                        else -> File(context.filesDir, path).absolutePath
                                                    }
                                                }
                                                currentImagePath = absolutePath
                                                viewerBarcode = item.code
                                            }) {
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
    }
}

