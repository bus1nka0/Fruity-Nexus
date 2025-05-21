package com.busya.fruitynexus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private GridLayout gameGrid;
    private int rows = 10;
    private int cols = 5;
    private int cellSize = 75;
    private int padding = 1;

    private int[][] gameBoard;
    private int[] possibleFruitDrawables = {
            R.drawable.yablochko,
            R.drawable.arbuzik,
            R.drawable.vinogradik,
            R.drawable.bananchiki,
            R.drawable.sliva
    };
    private Map<Point, View> cellViews = new HashMap<>();

    private float startX, startY;
    private View selectedView = null;
    private int selectedRow = -1, selectedCol = -1;

    private TextView movesCountTextView;
    private int movesCount = 0;

    private TextView scoreTextView;
    private int currentScore = 0;
    private ValueAnimator scoreAnimator;

    private static final int POINTS_PER_BLOCK = 10;
    private static final long SCORE_ANIMATION_DURATION = 500;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "GameSettings";
    private static final String HIGH_SCORE_KEY = "high_score";
    private static final String LANGUAGE_KEY = "selected_language";

    private Handler scoreCheckHandler;
    private Runnable scoreCheckRunnable;
    private static final long SCORE_CHECK_INTERVAL_MS = 500;

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
        setContentView(R.layout.activity_game);

        gameGrid = findViewById(R.id.gameGrid);
        gameGrid.setColumnCount(cols);
        gameGrid.setRowCount(rows);

        movesCountTextView = findViewById(R.id.movesCountTextView);
        scoreTextView = findViewById(R.id.scoreTextView);

        updateMovesCountDisplay();
        updateScoreDisplay(0);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        float density = getResources().getDisplayMetrics().density;
        cellSize = (int) (cellSize * density);
        padding = (int) (padding * density);

        gameBoard = new int[rows][cols];
        populateGameBoard();
        resolveInitialMatches();

        displayGameBoard();

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        scoreCheckHandler = new Handler();
        scoreCheckRunnable = new Runnable() {
            @Override
            public void run() {
                int displayedScore = 0;
                try {
                    String scoreText = scoreTextView.getText().toString();
                    scoreText = scoreText.replaceAll("[^\\d]", "");
                    if (!scoreText.isEmpty()) {
                        displayedScore = Integer.parseInt(scoreText);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                if (currentScore != displayedScore && (scoreAnimator == null || !scoreAnimator.isRunning())) {
                    addScore(currentScore - displayedScore);
                }
                scoreCheckHandler.postDelayed(this, SCORE_CHECK_INTERVAL_MS);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        scoreCheckHandler.post(scoreCheckRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkAndUpdateHighScore();
        scoreCheckHandler.removeCallbacks(scoreCheckRunnable);
    }

    private void populateGameBoard() {
        Random random = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int newFruit;
                do {
                    newFruit = possibleFruitDrawables[random.nextInt(possibleFruitDrawables.length)];
                } while ((j >= 2 && gameBoard[i][j - 1] == newFruit && gameBoard[i][j - 2] == newFruit) ||
                        (i >= 2 && gameBoard[i - 1][j] == newFruit && gameBoard[i - 2][j] == newFruit));
                gameBoard[i][j] = newFruit;
            }
        }
    }

    private void resolveInitialMatches() {
        List<Set<Point>> currentMatches;
        int maxAttempts = 100;
        int attempt = 0;
        int totalBlocksRemoved = 0;

        do {
            currentMatches = findAllMatches();
            if (!currentMatches.isEmpty()) {
                Set<Point> allPointsToRemove = new HashSet<>();
                for (Set<Point> match : currentMatches) {
                    allPointsToRemove.addAll(match);
                }
                totalBlocksRemoved += allPointsToRemove.size();
                for (Point p : allPointsToRemove) {
                    gameBoard[p.row][p.col] = 0;
                }
                moveAndFillWithoutRecursion();
            }
            attempt++;
        } while (!currentMatches.isEmpty() && attempt < maxAttempts);

        if (totalBlocksRemoved > 0) {
            addScore(totalBlocksRemoved * POINTS_PER_BLOCK);
        }
    }

    private void displayGameBoard() {
        gameGrid.removeAllViews();
        cellViews.clear();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                View cell = new View(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                params.setMargins(padding, padding, padding, padding);
                cell.setLayoutParams(params);
                cell.setBackgroundResource(gameBoard[i][j]);

                final int row = i;
                final int col = j;
                Point point = new Point(row, col);
                cellViews.put(point, cell);

                cell.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            startY = event.getY();
                            selectedView = v;
                            selectedRow = row;
                            selectedCol = col;
                            return true;
                        case MotionEvent.ACTION_UP:
                            float endX = event.getX();
                            float endY = event.getY();
                            float deltaX = endX - startX;
                            float deltaY = endY - startY;
                            float minSwipeDistance = cellSize / 2;

                            int targetRow = row;
                            int targetCol = col;

                            if (Math.abs(deltaX) > minSwipeDistance && Math.abs(deltaX) > Math.abs(deltaY)) {
                                if (deltaX > 0) targetCol++;
                                else targetCol--;
                            } else if (Math.abs(deltaY) > minSwipeDistance && Math.abs(deltaY) > Math.abs(deltaX)) {
                                if (deltaY > 0) targetRow++;
                                else targetRow--;
                            }

                            if (targetRow >= 0 && targetRow < rows && targetCol >= 0 && targetCol < cols &&
                                    (targetRow != row || targetCol != col)) {

                                int temp = gameBoard[row][col];
                                gameBoard[row][col] = gameBoard[targetRow][targetCol];
                                gameBoard[targetRow][targetCol] = temp;

                                List<Set<Point>> matches = findAllMatches();

                                if (!matches.isEmpty()) {
                                    movesCount++;
                                    updateMovesCountDisplay();
                                    SettingsActivity.vibrate(GameActivity.this);
                                    displayGameBoard();
                                    animateAndRemoveMatches(matches);
                                } else {
                                    temp = gameBoard[row][col];
                                    gameBoard[row][col] = gameBoard[targetRow][targetCol];
                                    gameBoard[targetRow][targetCol] = temp;
                                    displayGameBoard();
                                }
                            }

                            selectedView = null;
                            selectedRow = -1;
                            selectedCol = -1;
                            return true;
                    }
                    return false;
                });

                gameGrid.addView(cell);
            }
        }
    }

    private void swapElements(int row1, int col1, int row2, int col2) {
        displayGameBoard();
    }


    private List<Set<Point>> findAllMatches() {
        List<Set<Point>> allMatches = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols - 2; j++) {
                if (gameBoard[i][j] != 0 &&
                        gameBoard[i][j] == gameBoard[i][j + 1] &&
                        gameBoard[i][j] == gameBoard[i][j + 2]) {
                    Set<Point> match = new HashSet<>();
                    match.add(new Point(i, j));
                    match.add(new Point(i, j + 1));
                    match.add(new Point(i, j + 2));
                    int k = j + 3;
                    while (k < cols && gameBoard[i][k] != 0 && gameBoard[i][k] == gameBoard[i][j]) {
                        match.add(new Point(i, k));
                        k++;
                    }
                    allMatches.add(match);
                }
            }
        }

        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows - 2; i++) {
                if (gameBoard[i][j] != 0 &&
                        gameBoard[i][j] == gameBoard[i + 1][j] &&
                        gameBoard[i][j] == gameBoard[i + 2][j]) {
                    Set<Point> match = new HashSet<>();
                    match.add(new Point(i, j));
                    match.add(new Point(i + 1, j));
                    match.add(new Point(i + 2, j));
                    int k = i + 3;
                    while (k < rows && gameBoard[k][j] != 0 && gameBoard[k][j] == gameBoard[i][j]) {
                        match.add(new Point(k, j));
                        k++;
                    }
                    allMatches.add(match);
                }
            }
        }
        return allMatches;
    }

    private void animateAndRemoveMatches(List<Set<Point>> matches) {
        long animationDuration = 300;
        final List<Point> pointsToRemove = new ArrayList<>();
        int animationCounter = 0;
        int blocksRemovedInThisChain = 0;

        for (Set<Point> match : matches) {
            for (Point p : match) {
                if (!pointsToRemove.contains(p)) {
                    pointsToRemove.add(p);
                    blocksRemovedInThisChain++;
                    View viewToAnimate = cellViews.get(p);

                    if (viewToAnimate != null) {
                        int tempBlocksRemovedInThisChain = blocksRemovedInThisChain;
                        viewToAnimate.animate()
                                .alpha(0f)
                                .setDuration(animationDuration)
                                .setStartDelay(animationCounter * 50)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        gameBoard[p.row][p.col] = 0;

                                        if (pointsToRemove.indexOf(p) == pointsToRemove.size() - 1) {
                                            addScore(tempBlocksRemovedInThisChain * POINTS_PER_BLOCK);

                                            moveAndFill();
                                            displayGameBoard();

                                            List<Set<Point>> newMatches = findAllMatches();
                                            if (!newMatches.isEmpty()) {
                                                SettingsActivity.vibrate(GameActivity.this);
                                                animateAndRemoveMatches(newMatches);
                                            } else {
                                                checkAndUpdateHighScore();
                                            }
                                        }
                                    }
                                });
                        animationCounter++;
                    }
                }
            }
        }
        if (pointsToRemove.isEmpty()) {
            moveAndFill();
            displayGameBoard();
            checkAndUpdateHighScore();
        }
    }

    private void moveAndFill() {
        Random random = new Random();
        for (int j = 0; j < cols; j++) {
            int emptyRow = rows - 1;
            for (int i = rows - 1; i >= 0; i--) {
                if (gameBoard[i][j] != 0) {
                    gameBoard[emptyRow][j] = gameBoard[i][j];
                    if (i != emptyRow) {
                        gameBoard[i][j] = 0;
                    }
                    emptyRow--;
                }
            }
            for (int i = 0; i <= emptyRow; i++) {
                gameBoard[i][j] = possibleFruitDrawables[random.nextInt(possibleFruitDrawables.length)];
            }
        }
    }

    private void moveAndFillWithoutRecursion() {
        Random random = new Random();
        for (int j = 0; j < cols; j++) {
            int emptyRow = rows - 1;
            for (int i = rows - 1; i >= 0; i--) {
                if (gameBoard[i][j] != 0) {
                    gameBoard[emptyRow][j] = gameBoard[i][j];
                    if (i != emptyRow) {
                        gameBoard[i][j] = 0;
                    }
                    emptyRow--;
                }
            }
            for (int i = 0; i <= emptyRow; i++) {
                gameBoard[i][j] = possibleFruitDrawables[random.nextInt(possibleFruitDrawables.length)];
            }
        }
    }

    private void updateMovesCountDisplay() {
        movesCountTextView.setText(getString(R.string.moves_text, movesCount));
    }

    private void addScore(int pointsToAdd) {
        int startScoreDisplayed = 0;

        if (scoreAnimator != null && scoreAnimator.isRunning()) {
            scoreAnimator.cancel();
            try {
                String scoreText = scoreTextView.getText().toString();
                scoreText = scoreText.replaceAll("[^\\d]", "");
                if (!scoreText.isEmpty()) {
                    startScoreDisplayed = Integer.parseInt(scoreText);
                }
            } catch (NumberFormatException e) {
                startScoreDisplayed = currentScore;
            }
        } else {
            try {
                String scoreText = scoreTextView.getText().toString();
                scoreText = scoreText.replaceAll("[^\\d]", "");
                if (!scoreText.isEmpty()) {
                    startScoreDisplayed = Integer.parseInt(scoreText);
                }
            } catch (NumberFormatException e) {
                startScoreDisplayed = currentScore;
            }
        }

        currentScore += pointsToAdd;

        scoreAnimator = ValueAnimator.ofInt(startScoreDisplayed, currentScore);
        scoreAnimator.setDuration(SCORE_ANIMATION_DURATION);
        scoreAnimator.setInterpolator(new LinearInterpolator());

        scoreAnimator.addUpdateListener(animator -> {
            int animatedValue = (int) animator.getAnimatedValue();
            scoreTextView.setText(getString(R.string.score_text, animatedValue));
        });

        scoreAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scoreTextView.setText(getString(R.string.score_text, currentScore));
            }
        });

        scoreAnimator.start();
    }

    private void updateScoreDisplay(int score) {
        currentScore = score;
        scoreTextView.setText(getString(R.string.score_text, currentScore));
    }

    private void checkAndUpdateHighScore() {
        int currentHighScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0);
        if (currentScore > currentHighScore) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(HIGH_SCORE_KEY, currentScore);
            editor.apply();
        }
    }

    private static class Point {
        int row;
        int col;

        public Point(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return row == point.row && col == point.col;
        }

        @Override
        public int hashCode() {
            int result = row;
            result = 31 * result + col;
            return result;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }
        if (scoreCheckHandler != null) {
            scoreCheckHandler.removeCallbacks(scoreCheckRunnable);
        }
    }
}