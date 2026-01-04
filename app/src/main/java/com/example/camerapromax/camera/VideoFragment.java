package com.example.camerapromax.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.camerapromax.R;
import com.example.camerapromax.databinding.FragmentVideoBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Простой подкласс {@link Fragment}.
 */
public class VideoFragment extends Fragment {

    private static final String TAG = "VideoFragment";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private FragmentVideoBinding binding;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ExecutorService cameraExecutor;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private boolean isRecording = false;

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
        binding = FragmentVideoBinding.inflate(inflater, container, false);
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

        binding.recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });
        binding.switchCameraButton.setOnClickListener(v -> switchCamera());
        binding.toPhotoButton.setOnClickListener(v -> navController.navigate(R.id.action_videoFragment_to_photoFragment));
        binding.toGalleryButton.setOnClickListener(v -> navController.navigate(R.id.action_videoFragment_to_galleryFragment));

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
     * Этот метод настраивает предварительный просмотр и сценарии захвата видео и привязывает их к жизненному циклу фрагмента.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Начинает запись видео и сохраняет его в хранилище медиафайлов устройства.
     * Этот метод изменяет внешний вид кнопки записи, запускает хронометр и начинает захват видео.
     */
    private void startRecording() {
        isRecording = true;
        binding.recordButton.setBackgroundResource(R.drawable.ic_capture_recording);

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
        }

        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions
                .Builder(requireContext().getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        PendingRecording pendingRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), mediaStoreOutputOptions);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            pendingRecording.withAudioEnabled();
        }

        binding.recordDuration.setBase(SystemClock.elapsedRealtime());
        binding.recordDuration.setVisibility(View.VISIBLE);
        binding.recordDuration.start();


        recording = pendingRecording.start(ContextCompat.getMainExecutor(requireContext()),
                (Consumer<VideoRecordEvent>) videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                        if (!finalizeEvent.hasError()) {
                            String msg = "Video capture succeeded: " +
                                    finalizeEvent.getOutputResults().getOutputUri();
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, msg);
                        } else {
                            if (recording != null) {
                                recording.close();
                                recording = null;
                            }
                            Log.e(TAG, "Video capture ends with error: " +
                                    finalizeEvent.getError());
                        }
                    }
                });
    }

    /**
     * Останавливает текущую запись видео.
     * Этот метод сбрасывает внешний вид кнопки записи, останавливает хронометр и завершает запись.
     */
    private void stopRecording() {
        isRecording = false;
        binding.recordButton.setBackgroundResource(R.drawable.ic_capture);
        binding.recordDuration.stop();
        binding.recordDuration.setVisibility(View.GONE);
        if (recording != null) {
            recording.stop();
            recording = null;
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