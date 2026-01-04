package com.example.camerapromax.gallery;

import android.net.Uri;

/**
 * Класс данных, представляющий медиафайл.
 * Этот класс содержит URI, имя и MIME-тип медиафайла.
 */
public class MediaFile {
    private final Uri uri;
    private final String name;
    private final String type;

    /**
     * Создает новый MediaFile.
     *
     * @param uri  URI медиафайла.
     * @param name Имя медиафайла.
     * @param type MIME-тип медиафайла.
     */
    public MediaFile(Uri uri, String name, String type) {
        this.uri = uri;
        this.name = name;
        this.type = type;
    }

    /**
     * Получает URI медиафайла.
     *
     * @return URI медиафайла.
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Получает имя медиафайла.
     *
     * @return Имя медиафайла.
     */
    public String getName() {
        return name;
    }

    /**
     * Получает MIME-тип медиафайла.
     *
     * @return MIME-тип медиафайла.
     */
    public String getType() {
        return type;
    }
}