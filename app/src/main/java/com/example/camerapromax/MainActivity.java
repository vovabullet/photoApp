package com.example.camerapromax;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.view.WindowCompat;

/**
 * The main activity of the application.
 * This activity serves as the host for the navigation component and enables edge-to-edge display.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     * This method sets up the edge-to-edge display and inflates the main layout.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
    }
}