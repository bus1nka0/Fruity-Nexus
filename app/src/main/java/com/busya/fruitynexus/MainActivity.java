package com.busya.fruitynexus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Убедитесь, что у вас есть activity_main.xml

        Button playButton = findViewById(R.id.play_button); // Добавьте кнопку с id "play_button" в activity_main.xml
        Button settingsButton = findViewById(R.id.settings_button); // Добавьте кнопку с id "settings_button" в activity_main.xml
        Button exitButton = findViewById(R.id.exit_button); // Добавьте кнопку с id "exit_button" в activity_main.xml

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity(); // Завершает работу приложения
            }
        });
    }
}