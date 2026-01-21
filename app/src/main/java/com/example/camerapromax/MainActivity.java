package com.example.camerapromax;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.view.WindowCompat;

/**
 * Главная активность приложения.
 * Эта активность служит хостом для компонента навигации и включает отображение от края до края.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Вызывается при первом создании активности.
     * Этот метод настраивает отображение от края до края и загружает основной макет.
     *
     * @param savedInstanceState Если активность повторно инициализируется после
     *                           предыдущего завершения, то этот Bundle содержит данные, которые
     *                           были последний раз предоставлены в {@link #onSaveInstanceState}.
     *                           <b><i>Примечание: В противном случае он равен null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
    }
}