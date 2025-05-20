package com.busya.fruitynexus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Switch vibrationSwitch;
    private TextView selectedLanguageTextView;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "GameSettings";
    private static final String VIBRATION_KEY = "vibration_enabled";
    private static final String LANGUAGE_KEY = "selected_language";

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences sharedPreferences = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String langCode = sharedPreferences.getString(LANGUAGE_KEY, "en");

        Locale locale = new Locale(langCode);
        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        selectedLanguageTextView = findViewById(R.id.selectedLanguageTextView);

        boolean vibrationEnabled = sharedPreferences.getBoolean(VIBRATION_KEY, true);
        vibrationSwitch.setChecked(vibrationEnabled);

        updateSelectedLanguageDisplay();

        vibrationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(VIBRATION_KEY, isChecked);
                editor.apply();
            }
        });

        findViewById(R.id.languageSettingLayout).setOnClickListener(v -> {
            String currentLang = sharedPreferences.getString(LANGUAGE_KEY, "en");
            String newLang;
            if ("en".equals(currentLang)) {
                newLang = "ru";
            } else {
                newLang = "en";
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(LANGUAGE_KEY, newLang);
            editor.apply();

            updateSelectedLanguageDisplay();

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        TextView settingsTitle = findViewById(R.id.settingsTitle);
        if (settingsTitle != null) {
            settingsTitle.setText(getString(R.string.settings_button));
        }
    }

    private void updateSelectedLanguageDisplay() {
        String savedLang = sharedPreferences.getString(LANGUAGE_KEY, "en");
        if ("ru".equals(savedLang)) {
            selectedLanguageTextView.setText(getString(R.string.language_russian));
        } else {
            selectedLanguageTextView.setText(getString(R.string.language_english));
        }
    }

    public static void vibrate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean vibrationEnabled = prefs.getBoolean(VIBRATION_KEY, true);
    }
}