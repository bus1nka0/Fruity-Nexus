package com.busya.fruitynexus;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings); // Убедитесь, что у вас есть activity_settings.xml

        // Здесь можно добавить логику для настроек, например:
        // Switch soundToggle = findViewById(R.id.sound_toggle);
        // soundToggle.setOnCheckedChangeListener(...);
    }
}