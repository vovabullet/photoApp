package com.example.camerapromax.gallery;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.camerapromax.databinding.FragmentGalleryBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент для отображения галереи медиафайлов (изображений и видео).
 * Этот фрагмент использует RecyclerView для отображения медиафайлов в виде сетки.
 * Он позволяет пользователям просматривать и удалять медиафайлы.
 */
public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private GalleryAdapter adapter;
    private final List<MediaFile> mediaFiles = new ArrayList<>();

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
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Вызывается сразу после возврата из {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, но до восстановления сохраненного состояния в представление.
     * Этот метод инициализирует RecyclerView и загружает медиафайлы из хранилища устройства.
     *
     * @param view               Представление, возвращенное методом {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState Если не null, этот фрагмент восстанавливается из предыдущего сохраненного состояния, как указано здесь.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadMediaFiles();
    }

    /**
     * Настраивает RecyclerView с GridLayoutManager и GalleryAdapter.
     * Также определяет слушатель кликов для элементов галереи, позволяя пользователям просматривать или удалять их.
     */
    private void setupRecyclerView() {
        adapter = new GalleryAdapter(mediaFiles, mediaFile -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Choose an action")
                    .setItems(new CharSequence[]{"View", "Delete"}, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(mediaFile.getUri(), mediaFile.getType());
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } else {
                            deleteMediaFile(mediaFile);
                        }
                    })
                    .show();
        });
        binding.galleryRecyclerview.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.galleryRecyclerview.setAdapter(adapter);
    }

    /**
     * Загружает медиафайлы (изображения и видео) из внешнего хранилища устройства с помощью ContentResolver.
     * Загруженные файлы добавляются в список {@code mediaFiles}, и адаптер уведомляется об изменении данных.
     */
    private void loadMediaFiles() {
        mediaFiles.clear();
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE
        };
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " +
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        try (Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String mimeType = cursor.getString(mimeTypeColumn);
                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Files.getContentUri("external"),
                            String.valueOf(id)
                    );
                    mediaFiles.add(new MediaFile(contentUri, name, mimeType));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Удаляет медиафайл из хранилища устройства.
     * Этот метод показывает диалог подтверждения перед удалением файла.
     *
     * @param mediaFile {@link MediaFile}, который нужно удалить.
     */
    private void deleteMediaFile(MediaFile mediaFile) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete file")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        requireContext().getContentResolver().delete(mediaFile.getUri(), null, null);
                        int position = mediaFiles.indexOf(mediaFile);
                        if (position != -1) {
                            mediaFiles.remove(position);
                            adapter.notifyItemRemoved(position);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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