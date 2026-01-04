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
 * Адаптер для отображения медиафайлов в RecyclerView.
 * Этот адаптер отвечает за создание и привязку представлений для каждого медиафайла в списке.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private final List<MediaFile> mediaFiles;
    private final OnItemClickListener onItemClickListener;

    /**
     * Интерфейс для обработки кликов по элементам в RecyclerView.
     */
    public interface OnItemClickListener {
        /**
         * Вызывается при клике на элемент медиафайла.
         *
         * @param mediaFile Кликнутый {@link MediaFile}.
         */
        void onItemClick(MediaFile mediaFile);
    }

    /**
     * Создает новый GalleryAdapter.
     *
     * @param mediaFiles        Список медиафайлов для отображения.
     * @param onItemClickListener Слушатель кликов по элементам.
     */
    public GalleryAdapter(List<MediaFile> mediaFiles, OnItemClickListener onItemClickListener) {
        this.mediaFiles = mediaFiles;
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * Вызывается, когда RecyclerView необходим новый {@link GalleryViewHolder} данного типа для представления элемента.
     *
     * @param parent   ViewGroup, в которую будет добавлено новое представление после привязки к позиции адаптера.
     * @param viewType Тип представления нового представления.
     * @return Новый GalleryViewHolder, содержащий представление данного типа.
     */
    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GalleryViewHolder(GalleryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    /**
     * Вызывается RecyclerView для отображения данных в указанной позиции.
     *
     * @param holder   GalleryViewHolder, который должен быть обновлен для представления содержимого элемента в данной позиции набора данных.
     * @param position Позиция элемента в наборе данных адаптера.
     */
    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        holder.bind(mediaFiles.get(position));
    }

    /**
     * Возвращает общее количество элементов в наборе данных, хранящемся адаптером.
     *
     * @return Общее количество элементов в этом адаптере.
     */
    @Override
    public int getItemCount() {
        return mediaFiles.size();
    }

    /**
     * ViewHolder для отображения одного элемента медиафайла.
     */
    class GalleryViewHolder extends RecyclerView.ViewHolder {

        private final GalleryItemBinding binding;

        /**
         * Создает новый GalleryViewHolder.
         *
         * @param binding Привязка представления для макета элемента галереи.
         */
        public GalleryViewHolder(GalleryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Привязывает медиафайл к представлению.
         *
         * @param mediaFile {@link MediaFile} для привязки.
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