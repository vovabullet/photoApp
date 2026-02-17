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
                            // Intent — это сообщение Android системе: "Я хочу выполнить действие X с данными Y"
                            Intent intent = new Intent(Intent.ACTION_VIEW); // "Я хочу ПРОСМОТРЕТЬ что-то"
                            // передается: URI — где лежит файл, MIME type — что это за файл
                            // Зачем нужен MIME type? Android должен понять: КАКОЕ приложение может открыть этот файл
                            intent.setDataAndType(mediaFile.getUri(), mediaFile.getType());
                            // "Я временно разрешаю приложению, которое откроет intent, прочитать этот URI"
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            // Android:
                            // 1. смотрит action (VIEW)
                            // 2. смотрит MIME type
                            // 3. ищет приложение, которое умеет открыть.
                            startActivity(intent);
                        } else {
                            deleteMediaFile(mediaFile);
                        }
                    })
                    .show();
        });
        // GridLayoutManager получает context чтобы: узнать плотность пикселей экрана и рассчитать размеры элементов
        binding.galleryRecyclerview.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.galleryRecyclerview.setAdapter(adapter);
    }

    /**
     * Загружает медиафайлы (изображения и видео) из внешнего хранилища устройства с помощью ContentResolver.
     * Загруженные файлы добавляются в список {@code mediaFiles}, и адаптер уведомляется об изменении данных.
     */
    /* Полная схема работы
        1. очистить список
        2. сформировать запрос
        3. ContentResolver → MediaStore
        4. получить Cursor
        5. пройтись по строкам
        6. создать URI
        7. добавить в список
        8. обновить UI
     */
    private void loadMediaFiles() {
        // Удаляем старые данные (избегание дубликатов)
        mediaFiles.clear();
        // MediaStore - системная база данных
        String[] projection = {
                // какие столбцы нужно вернуть
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE
        };
        // фильтр (игнорируем: аудио, документы, другие файлы)
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " +
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" +
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        // Cursor - результат запроса
        // ContentProvider — это слой доступа к данным. В данном случае: MediaStore = ContentProvider
        try (Cursor cursor = requireContext().getContentResolver().query( // try-with-resources - Автоматически cursor.close()
                // "external" = внешнее хранилище
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                // Cursor хранит данные как таблицу, по этому необходимо узнать индекс столбца
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);

                while (cursor.moveToNext()) {
                    // Получаем значения колонок. Это ссылка через ContentProvider
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String mimeType = cursor.getString(mimeTypeColumn);
                    // создание URI
                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Files.getContentUri("external"),
                            String.valueOf(id)
                    );
                    // формируем модель для UI
                    mediaFiles.add(new MediaFile(contentUri, name, mimeType));
                }
            }
        }
        // обновление RecyclerView
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