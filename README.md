# Сканер штрихкодов

Android-приложение для сканирования штрихкодов с использованием камеры и ML Kit. Поддерживает сохранение результатов, просмотр фотографий, управление вспышкой и зумом.

## Возможности

- Сканирование штрихкодов через камеру
- Сохранение отсканированных кодов в список
- Просмотр и предпросмотр фотографий штрихкодов
- Чекбоксы для выбора и массового удаления (по умолчанию выбраны)
- Чекбокс "Выбрать все" с тремя состояниями
- Вспышка/фонарик
- Зум и экспозиция
- Автоматическое определение нескольких штрихкодов в кадре
- Горизонтальная и вертикальная ориентация экрана
- Полноэкранный предпросмотр изображений с переходом по свайпу и масштабированием жестами
- Отдельный экран настроек с выбором темы (светлая/темная/авто)
- Юнит-тесты и Android-тесты

## Технологии

- Kotlin + Jetpack Compose
- CameraX (preview, image analysis, image capture)
- ML Kit Barcode Scanning
- Material 3
- AndroidX Navigation / Lifecycle / Core KTX

## Сборка

```bash
./gradlew assembleDebug
```

## Тесты

```bash
# Юнит-тесты
./gradlew test

# Android-тесты
./gradlew connectedDebugAndroidTest
```

## Структура проекта

```
app/src/main/java/com/sergio/barcodescanner/
├── MainActivity.kt
├── BarcodeScannerScreen.kt
├── BarcodeItem.kt
├── SettingsScreen.kt
├── FullScreenImagePreview.kt
└── ManualCameraScanView.kt
```

## Требования

- Android SDK 27+
- Compile SDK 37
