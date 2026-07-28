# Архитектура

## Общая схема

Приложение состоит из одного главного экрана (`BarcodeScannerScreen`), вспомогательных экранов и компонентов камеры.

```
MainActivity
└── BarcodeScannerTheme
    └── BarcodeScannerScreen
        ├── TopAppBar (TriStateCheckbox, +, share, copy, delete, ⋮)
        ├── LazyColumn (список штрихкодов)
        │   └── Card (чекбокс, номер, код)
        └── ManualCameraScanView (камера + ML Kit)
            ├── PreviewView
            ├── ImageAnalysis
            ├── ImageCapture
            └── FullScreenImagePreview (предпросмотр фото)
                └── SettingsScreen (отдельный экран настроек)
```

## Модель данных

### BarcodeItem

```kotlin
data class BarcodeItem(
    val id: String,
    val code: String,
    val imagePath: String?,
    var isSelected: Boolean = true
)
```

- `id` — уникальный идентификатор
- `code` — отсканированный штрихкод
- `imagePath` — путь к сохраненному фото
- `isSelected` — признак выбора в списке (по умолчанию `true`)

### ThemeMode

```kotlin
enum class ThemeMode { LIGHT, DARK, AUTO }
```

- `LIGHT` — светлая тема
- `DARK` — темная тема
- `AUTO` — следование системной теме (по умолчанию)

## Основные экраны

### BarcodeScannerScreen

Главный экран списка. Отвечает за:
- отображение списка `BarcodeItem`;
- выбор всех/снятие выбора через `TriStateCheckbox`;
- удаление выбранных элементов и фото;
- открытие камеры;
- массовые действия: копирование, шеринг текста/фото, удаление;
- полноэкранный просмотр фото через `FullScreenImagePreview`;
- открытие экрана настроек `SettingsScreen`.

### SettingsScreen

Отдельный экран настроек. Отвечает за:
- выбор темы приложения (светлая/темная/авто);
- возврат на главный экран по кнопке «Назад».

### ManualCameraScanView

Экран камеры. Отвечает за:
- привязку CameraX к жизненному циклу;
- анализ кадров через `ImageAnalysis`;
- распознавание штрихкодов через ML Kit;
- сохранение фото через `ImageCapture` с обрезкой по `boundingBox`;
- воспроизведение звука затвора;
- управление вспышкой, зумом, экспозицией;
- отображение рамки вокруг найденного штрихкода;
- предпросмотр с кнопками «Сохранить»/«Отклонить»;
- возврат на камеру после закрытия превью.

### FullScreenImagePreview

Полноэкранный просмотр изображения. Отвечает за:
- загрузку и отображение изображения;
- зум pinch-жестами;
- показ номера элемента в списке (режим списка);
- переход между изображениями по свайпу влево/вправо (только при открытии из списка).

## Состояния

- `barcodeList` — `mutableStateListOf<BarcodeItem>`
- `hasSelection` — derived state для тулбара
- `selectAllState` — derived state для `TriStateCheckbox`
- `currentImagePath` — путь к фото для просмотра
- `viewerBarcode` — код штрихкода для превью
- `showSettings` — флаг открытия экрана настроек
- `currentThemeMode` — текущий режим темы
- `isCameraOpen` — флаг открытия камеры

## Хранение

- Штрихкоды и фото: `SharedPreferences` + файлы в `context.filesDir/barcode_images`
- Тема: `SharedPreferences` (`barcode_prefs`)

## Тесты

- Юнит-тесты: `app/src/test/.../BarcodeScannerLogicTest.kt`
- Android-тесты: `app/src/androidTest/.../BarcodeScannerScreenTest.kt`
