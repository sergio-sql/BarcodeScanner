# Архитектура

## Общая схема

Приложение состоит из одного экрана (`BarcodeScannerScreen`) и вспомогательных компонентов.

```
MainActivity
└── BarcodeScannerScreen
    ├── TopAppBar (выбор всех, удаление, добавление)
    ├── LazyColumn (список штрихкодов)
    │   └── Card (чекбокс, номер, код, иконка глаза)
    └── ManualCameraScanView (камера + ML Kit)
        ├── PreviewView
        ├── ImageAnalysis
        ├── ImageCapture
        └── AlertDialog (предпросмотр фото)
```

## Модель данных

### BarcodeItem

```kotlin
data class BarcodeItem(
    val id: String,
    val code: String,
    val imagePath: String?,
    var isSelected: Boolean
)
```

- `id` — уникальный идентификатор
- `code` — отсканированный штрихкод
- `imagePath` — путь к сохраненному фото
- `isSelected` — признак выбора в списке

## Основные экраны

### BarcodeScannerScreen

Главный экран списка. Отвечает за:
- отображение списка `BarcodeItem`;
- выбор всех/снятие выбора;
- удаление выбранных элементов и фото;
- открытие камеры;
- полноэкранный просмотр фото (`BarcodeImagePreviewDialog`).

### ManualCameraScanView

Экран камеры. Отвечает за:
- привязку CameraX к жизненному циклу;
- анализ кадров через `ImageAnalysis`;
- распознавание штрихкодов через ML Kit;
- сохранение фото через `ImageCapture`;
- управление вспышкой, зумом, экспозицией;
- отображение рамки вокруг найденного штрихкода;
- выбор конкретного штрихкода через `FilterChip` под кадром;
- предпросмотр с кнопками «Сохранить»/«Отклонить».

### BarcodeImagePreviewDialog

Диалог для просмотра сохраненного фото штрихкода.

## Состояния

- `barcodeList` — `mutableStateListOf<BarcodeItem>`
- `selectedItems` — derived state для тулбара
- `isAllSelected` — derived state для чекбокса "Выбрать все"
- `currentImagePath` — путь к фото для просмотра
- `pendingBarcodes` — список найденных в кадре штрихкодов
- `selectedDetectedIndex` — выбранный штрихкод в камере
- `previewBitmap` — bitmap для предпросмотра

## Тесты

- Юнит-тесты: `app/src/test/.../BarcodeScannerLogicTest.kt`
- Android-тесты: `app/src/androidTest/.../BarcodeScannerScreenTest.kt`
