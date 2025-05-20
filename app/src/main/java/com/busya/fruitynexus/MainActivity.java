package com.busya.fruitynexus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout spinningFruitContainer;
    private TextView gameTitleTextView;
    private ImageView fruit1, fruit2, fruit3, fruit4, fruit5;
    private List<ImageView> orbitalFruitImageViews;

    private ObjectAnimator containerRotationAnimator;
    private List<ObjectAnimator> fruitRotationAnimators = new ArrayList<>();

    private long currentAnimationDuration = 15000;
    private static final long BASE_ANIMATION_DURATION = 15000;
    private static final long MIN_ANIMATION_DURATION = 1800;
    private static final long ACCELERATION_DECREMENT = 1000;
    private static final long DEACCELERATION_DELAY_MS = 1000;
    private static final long DEACCELERATION_ANIMATION_DURATION_MS = 2000;

    private Handler handler = new Handler();
    private Runnable deaccelerationRunnable;

    private TextView highScoreTextView;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "GameSettings";
    private static final String HIGH_SCORE_KEY = "high_score";
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
        setContentView(R.layout.activity_main);

        spinningFruitContainer = findViewById(R.id.spinningFruitContainer);
        gameTitleTextView = findViewById(R.id.gameTitle);

        fruit1 = findViewById(R.id.fruit1);
        fruit2 = findViewById(R.id.fruit2);
        fruit3 = findViewById(R.id.fruit3);
        fruit4 = findViewById(R.id.fruit4);
        fruit5 = findViewById(R.id.fruit5);

        orbitalFruitImageViews = new ArrayList<>(Arrays.asList(fruit1, fruit2, fruit3, fruit4));

        setupInitialAnimations();

        fruit5.setOnClickListener(v -> accelerateSpin());

        gameTitleTextView.setOnClickListener(v -> animateTitleJump());

        gameTitleTextView.setText(getString(R.string.app_name));

        highScoreTextView = findViewById(R.id.highScoreTextView);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        updateHighScoreDisplay();

        ImageButton playButton = findViewById(R.id.play_button);
        ImageButton settingsButton = findViewById(R.id.settings_button);
        ImageButton exitButton = findViewById(R.id.exit_button);

        playButton.setContentDescription(getString(R.string.play_button));
        settingsButton.setContentDescription(getString(R.string.settings_button));
        exitButton.setContentDescription(getString(R.string.exit_button));

        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        exitButton.setOnClickListener(v -> finishAffinity());
    }

    private void setupInitialAnimations() {
        containerRotationAnimator = ObjectAnimator.ofFloat(spinningFruitContainer, "rotation", 0f, 360f);
        containerRotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        containerRotationAnimator.setInterpolator(new LinearInterpolator());
        containerRotationAnimator.setDuration(currentAnimationDuration);
        containerRotationAnimator.start();

        for (ImageView fruit : orbitalFruitImageViews) {
            ObjectAnimator fruitAnimator = ObjectAnimator.ofFloat(fruit, "rotation", 0f, -360f);
            fruitAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            fruitAnimator.setInterpolator(new LinearInterpolator());
            fruitAnimator.setDuration(currentAnimationDuration);
            fruitAnimator.start();
            fruitRotationAnimators.add(fruitAnimator);
        }
    }

    private void accelerateSpin() {
        if (deaccelerationRunnable != null) {
            handler.removeCallbacks(deaccelerationRunnable);
        }

        if (currentAnimationDuration > MIN_ANIMATION_DURATION) {
            currentAnimationDuration = Math.max(MIN_ANIMATION_DURATION, currentAnimationDuration - ACCELERATION_DECREMENT);
            updateAnimationSpeeds(currentAnimationDuration, 0);
        }

        deaccelerationRunnable = () -> slowDownSpin();
        handler.postDelayed(deaccelerationRunnable, DEACCELERATION_DELAY_MS);
    }

    private void slowDownSpin() {
        ValueAnimator deaccelerationAnimator = ValueAnimator.ofInt(
                (int) currentAnimationDuration, (int) BASE_ANIMATION_DURATION
        );
        deaccelerationAnimator.setDuration(DEACCELERATION_ANIMATION_DURATION_MS);
        deaccelerationAnimator.setInterpolator(new LinearInterpolator());

        deaccelerationAnimator.addUpdateListener(animator -> {
            long animatedDuration = ((Integer) animator.getAnimatedValue()).longValue();
            updateAnimationSpeeds(animatedDuration, 0);
        });

        deaccelerationAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimationDuration = BASE_ANIMATION_DURATION;
                updateAnimationSpeeds(BASE_ANIMATION_DURATION, 0);
            }
        });
        deaccelerationAnimator.start();
    }

    private void updateAnimationSpeeds(long newDuration, long startDelay) {
        containerRotationAnimator.setDuration(newDuration);
        containerRotationAnimator.setStartDelay(startDelay);
        if (!containerRotationAnimator.isRunning()) {
            containerRotationAnimator.start();
        } else {
            containerRotationAnimator.setCurrentPlayTime(
                    (long) (containerRotationAnimator.getAnimatedFraction() * newDuration)
            );
        }

        for (ObjectAnimator fruitAnimator : fruitRotationAnimators) {
            fruitAnimator.setDuration(newDuration);
            fruitAnimator.setStartDelay(startDelay);
            if (!fruitAnimator.isRunning()) {
                fruitAnimator.start();
            } else {
                fruitAnimator.setCurrentPlayTime(
                        (long) (fruitAnimator.getAnimatedFraction() * newDuration)
                );
            }
        }
    }

    private void animateTitleJump() {
        ObjectAnimator jumpAnimator = ObjectAnimator.ofFloat(gameTitleTextView, "translationY", 0f, -30f, 0f);
        jumpAnimator.setDuration(500);
        jumpAnimator.setInterpolator(new BounceInterpolator());
        jumpAnimator.start();
    }

    private void updateHighScoreDisplay() {
        int highScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0);
        highScoreTextView.setText(getString(R.string.high_score_text, highScore));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (containerRotationAnimator != null && containerRotationAnimator.isPaused()) {
            containerRotationAnimator.resume();
        }
        for (ObjectAnimator animator : fruitRotationAnimators) {
            if (animator != null && animator.isPaused()) {
                animator.resume();
            }
        }
        updateHighScoreDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (containerRotationAnimator != null && containerRotationAnimator.isRunning()) {
            containerRotationAnimator.pause();
        }
        for (ObjectAnimator animator : fruitRotationAnimators) {
            if (animator != null && animator.isRunning()) {
                animator.pause();
            }
        }
        if (deaccelerationRunnable != null) {
            handler.removeCallbacks(deaccelerationRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (containerRotationAnimator != null) {
            containerRotationAnimator.cancel();
        }
        for (ObjectAnimator animator : fruitRotationAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        if (deaccelerationRunnable != null) {
            handler.removeCallbacks(deaccelerationRunnable);
        }
        handler.removeCallbacksAndMessages(null);
    }
}