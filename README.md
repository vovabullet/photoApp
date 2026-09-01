# PhotoApp (Camera Coursework)

Android-приложение для съёмки фото и видео на базе CameraX с встроенной галереей.

## Возможности

- Съёмка фото (JPEG)
- Запись видео (MP4)
- Переключение фронтальной/основной камеры
- Встроенная галерея медиафайлов (фото и видео)
- Просмотр и удаление медиафайлов из галереи

## Стек

- Java
- Android SDK (minSdk 21, targetSdk 34)
- CameraX
- AndroidX Navigation
- ViewBinding
- Glide

## Требования

- Android Studio (рекомендуется последняя стабильная версия)
- JDK 8+
- Android SDK 34

## Запуск проекта

1. Откройте проект в Android Studio:
   - `/home/runner/work/photoApp/photoApp`
2. Дождитесь синхронизации Gradle.
3. Запустите приложение на устройстве или эмуляторе.
4. При первом запуске выдайте разрешения на камеру и микрофон.

## Сборка из консоли

```bash
./gradlew assembleDebug
```

## Основные экраны

- `PhotoFragment` — фото-съёмка
- `VideoFragment` — видео-съёмка
- `GalleryFragment` — просмотр и удаление медиа

## Разрешения

Приложение использует:

- `android.permission.CAMERA`
- `android.permission.RECORD_AUDIO`
- `android.permission.WRITE_EXTERNAL_STORAGE` (только для старых версий Android, maxSdkVersion=28)
