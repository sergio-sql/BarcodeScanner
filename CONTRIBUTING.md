# Contributing

## Как запустить проект

1. Откройте проект в Android Studio
2. Дождитесь синхронизации Gradle
3. Запустите на эмуляторе или устройстве:

```bash
./gradlew assembleDebug
```

## Как запустить тесты

```bash
# Юнит-тесты
./gradlew test

# Android-тесты
./gradlew connectedDebugAndroidTest
```

## Правила коммитов

Используйте понятные сообщения коммитов, например:
- `feat: add barcode selection`
- `fix: camera preview orientation`
- `refactor: split BarcodeScannerScreen`

## Структура кода

- Логика экрана → `BarcodeScannerScreen.kt`
- Камера → `ManualCameraScanView.kt`
- Модели → `BarcodeItem.kt`
- Диалоги → `BarcodeImagePreview.kt`
