package com.example.camerapromax.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.camerapromax.R;
import com.example.camerapromax.databinding.GalleryItemBinding;
import java.util.List;

/**
 * An adapter for displaying media files in a RecyclerView.
 * This adapter is responsible for creating and binding views for each media file in the list.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private final List<MediaFile> mediaFiles;
    private final OnItemClickListener onItemClickListener;

    /**
     * Interface for handling clicks on items in the RecyclerView.
     */
    public interface OnItemClickListener {
        /**
         * Called when a media file item is clicked.
         *
         * @param mediaFile The clicked {@link MediaFile}.
         */
        void onItemClick(MediaFile mediaFile);
    }

    /**
     * Constructs a new GalleryAdapter.
     *
     * @param mediaFiles        The list of media files to display.
     * @param onItemClickListener The listener for item clicks.
     */
    public GalleryAdapter(List<MediaFile> mediaFiles, OnItemClickListener onItemClickListener) {
        this.mediaFiles = mediaFiles;
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * Called when RecyclerView needs a new {@link GalleryViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new GalleryViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GalleryViewHolder(GalleryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder   The GalleryViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        holder.bind(mediaFiles.get(position));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mediaFiles.size();
    }

    /**
     * A ViewHolder for displaying a single media file item.
     */
    class GalleryViewHolder extends RecyclerView.ViewHolder {

        private final GalleryItemBinding binding;

        /**
         * Constructs a new GalleryViewHolder.
         *
         * @param binding The view binding for the gallery item layout.
         */
        public GalleryViewHolder(GalleryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a media file to the view.
         *
         * @param mediaFile The {@link MediaFile} to bind.
         */
        public void bind(MediaFile mediaFile) {
            Glide.with(itemView.getContext())
                    .load(mediaFile.getUri())
                    .into(binding.mediaThumbnail);

            if (mediaFile.getType().startsWith("video")) {
                binding.playIcon.setVisibility(View.VISIBLE);
            } else {
                binding.playIcon.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> onItemClickListener.onItemClick(mediaFile));
        }
    }
}