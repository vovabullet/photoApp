package com.example.camerapromax.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.camerapromax.R;
import com.example.camerapromax.databinding.FragmentPhotoBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Простой подкласс {@link Fragment}
 */
public class PhotoFragment extends Fragment {

    private static final String TAG = "PhotoFragment";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
    };

    private FragmentPhotoBinding binding;

    /** Объект CameraX. Он обрабатывает use case «сделать фото».
     * - запрашивает у CameraX поток кадров
     * - в момент takePicture():
     * -- берёт последний доступный кадр
     * -- обрабатывает его
     * -- сохраняет или отдаёт в callback
     * Снимок не делается напрямую, вместо этого говорится: «Когда будешь готов — сохрани кадр»
      */
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    // запускает системное действие и получает результат асинхронно и безопасно для lifecycle
    private final ActivityResultLauncher<String[]> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allPermissionsGranted = true;
                for (Boolean isGranted : result.values()) {
                    if (!isGranted) {
                        allPermissionsGranted = false;
                        break;
                    }
                }

                if (allPermissionsGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                    requireActivity().finish();
                }
            });

    /**
     * Создает макет для этого фрагмента.
     *
     * @param inflater           Объект LayoutInflater, который можно использовать для создания представлений во фрагменте. LayoutInflater — это объект, который превращает XML layout в реальные Java-объекты View.
     * @param container          Если не null, это родительское представление, к которому должен быть прикреплен интерфейс фрагмента.
     * @param savedInstanceState Если не null, этот фрагмент восстанавливается из предыдущего сохраненного состояния, как указано здесь.
     * @return Представление для интерфейса фрагмента или null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPhotoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Вызывается сразу после возврата из {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, но до восстановления сохраненного состояния в представление.
     * Этот метод инициализирует камеру, устанавливает слушатели для элементов интерфейса и запрашивает необходимые разрешения.
     *
     * @param view               Представление, возвращенное методом {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState Если не null, этот фрагмент восстанавливается из предыдущего сохраненного состояния, как указано здесь.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS);
        }

        NavController navController = Navigation.findNavController(view);

        binding.captureButton.setOnClickListener(v -> takePhoto());
        binding.switchCameraButton.setOnClickListener(v -> switchCamera());
        binding.toVideoButton.setOnClickListener(v -> navController.navigate(R.id.action_photoFragment_to_videoFragment));
        binding.toGalleryButton.setOnClickListener(v -> navController.navigate(R.id.action_photoFragment_to_galleryFragment));
    }

    /**
     * Проверяет, предоставлены ли все необходимые разрешения.
     *
     * @return {@code true}, если все разрешения предоставлены, {@code false} в противном случае.
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Инициализирует и запускает камеру с использованием CameraX.
     * Этот метод настраивает предварительный просмотр и сценарии захвата изображений и привязывает их к жизненному циклу фрагмента.
     */
    private void startCamera() {
        /* начинает асинхронную инициализацию CameraX, возвращает Future, который позже даст объект управления камерой
        Что происходит внутри CameraX:
            - связывается с CameraService (системный сервис Android)
            - проверяет доступность камеры
            - готовит pipeline
        */
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> { // "Когда камера будет готова — выполни этот код"
            try {
                // менеджер камеры
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                /* Что значит «подготовить камеру». Android НЕ просто создаёт объект. Он запускает целый технический процесс.
                Android имеет отдельный системный сервис: CameraService. Он:
                - отправляет запрос системе
                - проверяет доступность камеры
                - получает список камер устройства
                - Проверка разрешений
                - Получение характеристик камеры
                - Инициализация потоков
                - Подготовка pipeline:
                    - поток кадров
                    - обработку изображений
                    - буферы памяти
                    - синхронизацию preview/capture.

                 Всё это НЕ мгновенно, поэтому возвращается Future.
                 */

                // Preview — это use case, который подписывается на поток кадров камеры. Он получается кадры, отправялет их в Surface
                /* Use case в CameraX = конфигурация потока камеры. Например,
                    Preview → получать поток для отображения
                    ImageCapture → получать кадры для фото
                    VideoCapture → получать поток для записи
                    ImageAnalysis → получать кадры для анализа
                 */
                Preview preview = new Preview.Builder().build();
                // Camera НЕ рисует напрямую в View. Она рисует в Surface. Surface — низкоуровневый объект Android.
                // Camera -> Preview -> SurfaceProvider -> PreviewView(UI)
                // "Preview, когда тебе нужен Surface — бери его отсюда"
                // Surface = куда камера рисует пиксели.
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                // умеет делать JPEG снимок
                imageCapture = new ImageCapture.Builder().build();

                // уберать старые use cases
                cameraProvider.unbindAll();
                /* Пока Fragment жив:
                    - включи камеру
                    - запусти preview
                    - разреши делать фото

                    Fragment STARTED → камера работает
                    Fragment STOPPED → камера закрывается
                */
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
        /* Полная схема
        startCamera()

        1. cameraProviderFuture = "подготовь камеру"

        2. addListener(...)
           └ когда готово:

               cameraProvider = менеджер камеры

               preview = "хочу поток видео"
               imageCapture = "хочу делать фото"

               preview подключаем к PreviewView

               bindToLifecycle(...)
                    ↓
               камера реально стартует
         */
    }

    /**
     * Захватывает фотографию и сохраняет её в хранилище медиафайлов устройства.
     * Этот метод создает новый файл в хранилище медиафайлов, захватывает изображение и отображает всплывающее сообщение при успехе или неудаче.
     * Он также запускает анимацию вспышки.
     */
    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // КАК и КУДА сохранить фото
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(requireContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        /**
         * Камера постоянно снимает
         * Даже если пользователь ничего не делаешь:
         * - камера уже снимает
         * - кадры идут потоком
         * - Preview их отображает
         * ImageCapture тоже подписан на этот поток.
         * CameraX делает вот что:
         * 1. Ждёт подходящий кадр
         * 2. Делает:
         * - автофокус
         * - автоэкспозицию
         * - баланс белого
         * 3. Берёт самый качественный кадр
         * 4. Применяет:
         * - поворот
         * - зеркалирование (для фронталки)
         * 5. Кодирует в JPEG
         * 6. Сохраняет или отдаёт
         */
        imageCapture.takePicture(
                outputOptions,
                // Executor = объект, который решает, ГДЕ и КОГДА выполнить код - где (в каком потоке) и как (сразу / в очереди).
                // «Вызови onImageSaved() и onError() в UI-потоке»
                /*
                Context:
                - говорит, какой процесс
                - какой Looper
                - какой Main Thread
                Executor:
                - решает, ГДЕ выполнить callback
                 */
                ContextCompat.getMainExecutor(requireContext()), // callback вызовется в главном потоке (MainExecutor - это главный UI-поток). Context — это доступ к системе Android.
                new ImageCapture.OnImageSavedCallback() { // CameraX говорит: «Я не знаю, когда закончу. Вот два метода — я вызову ОДИН из них.»
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) { // результат операции сохранения
                        String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); // Toast — это маленькое всплывающее сообщение
                        Log.d(TAG, msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                }
        );
         // Анимация вспышки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.getRoot().postDelayed(() -> {
                binding.getRoot().setForeground(new android.graphics.drawable.ColorDrawable(0x00000000));
            }, 100);
            binding.getRoot().setForeground(new android.graphics.drawable.ColorDrawable(0xB0FFFFFF));
        }

    }

    /**
     * Переключает между передней и задней камерами.
     * После переключения селектора камеры перезапускает камеру для применения изменений.
     */
    private void switchCamera() {
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        }
        startCamera();
    }

    /**
     * Вызывается, когда представление, ранее созданное методом {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, было отсоединено от фрагмента.
     * При следующем отображении фрагмента будет создано новое представление.
     * Устанавливает привязку в null, чтобы избежать утечек памяти.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}