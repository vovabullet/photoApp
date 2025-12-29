package com.example.cameracoursework.gallery;

import android.net.Uri;

/**
 * A data class representing a media file.
 * This class holds the URI, name, and MIME type of a media file.
 */
public class MediaFile {
    private final Uri uri;
    private final String name;
    private final String type;

    /**
     * Constructs a new MediaFile.
     *
     * @param uri  The URI of the media file.
     * @param name The name of the media file.
     * @param type The MIME type of the media file.
     */
    public MediaFile(Uri uri, String name, String type) {
        this.uri = uri;
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the URI of the media file.
     *
     * @return The URI of the media file.
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Gets the name of the media file.
     *
     * @return The name of the media file.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the MIME type of the media file.
     *
     * @return The MIME type of the media file.
     */
    public String getType() {
        return type;
    }
}