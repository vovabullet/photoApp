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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Простой подкласс {@link Fragment}
 */
public class PhotoFragment extends Fragment {

    private static final String TAG = "PhotoFragment";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private FragmentPhotoBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

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
     * @param inflater           Объект LayoutInflater, который можно использовать для создания представлений во фрагменте.
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

        cameraExecutor = Executors.newSingleThreadExecutor();
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
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
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

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(requireContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
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
     * Вызывается, когда фрагмент больше не используется.
     * Вызывается после {@link #onStop()} и до {@link #onDetach()}.
     * Завершает работу службы исполнителя камеры.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
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