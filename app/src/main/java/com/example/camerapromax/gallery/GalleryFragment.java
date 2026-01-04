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
 * A fragment for displaying a gallery of media files (images and videos).
 * This fragment uses a RecyclerView to display the media files in a grid layout.
 * It allows users to view and delete media files.
 */
public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private GalleryAdapter adapter;
    private final List<MediaFile> mediaFiles = new ArrayList<>();

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned, but before any saved state has been restored in to the view.
     * This method initializes the RecyclerView and loads the media files from the device's storage.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadMediaFiles();
    }

    /**
     * Sets up the RecyclerView with a GridLayoutManager and the GalleryAdapter.
     * It also defines the click listener for the gallery items, allowing users to view or delete them.
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
     * Loads media files (images and videos) from the device's external storage using a ContentResolver.
     * The loaded files are added to the {@code mediaFiles} list and the adapter is notified of the data change.
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
     * Deletes a media file from the device's storage.
     * This method shows a confirmation dialog before deleting the file.
     *
     * @param mediaFile The {@link MediaFile} to be deleted.
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
     * Called when the view previously created by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has been detached from the fragment.
     * The next time the fragment needs to be displayed, a new view will be created.
     * This sets the binding to null to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}