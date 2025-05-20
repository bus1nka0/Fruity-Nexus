package com.busya.fruitynexus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameActivity extends AppCompatActivity {

    private GridLayout gameGrid;
    private int rows = 10;
    private int cols = 6;
    private int cellSize = 80;
    private int padding = 2;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameGrid = findViewById(R.id.gameGrid);
        gameGrid.setColumnCount(cols);
        gameGrid.setRowCount(rows);

        float density = getResources().getDisplayMetrics().density;
        cellSize = (int) (cellSize * density);
        padding = (int) (padding * density);

        gameBoard = new int[rows][cols];
        populateGameBoard();
        displayGameBoard();
    }

    private void populateGameBoard() {
        Random random = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                gameBoard[i][j] = possibleFruitDrawables[random.nextInt(possibleFruitDrawables.length)];
            }
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

                cell.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
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
                                    swapElements(row, col, targetRow, targetCol);
                                    List<Set<Point>> matches = findAllMatches();
                                    if (!matches.isEmpty()) {
                                        animateAndRemoveMatches(matches);
                                    } else {
                                        Toast.makeText(GameActivity.this, "Нет совпадений!", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                selectedView = null;
                                selectedRow = -1;
                                selectedCol = -1;
                                return true;
                        }
                        return false;
                    }
                });

                gameGrid.addView(cell);
            }
        }
    }

    private void swapElements(int row1, int col1, int row2, int col2) {
        if (row2 >= 0 && row2 < rows && col2 >= 0 && col2 < cols) {
            int tempDrawable = gameBoard[row1][col1];
            gameBoard[row1][col1] = gameBoard[row2][col2];
            gameBoard[row2][col2] = tempDrawable;
            displayGameBoard();
        }
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

        for (Set<Point> match : matches) {
            for (Point p : match) {
                if (!pointsToRemove.contains(p)) {
                    pointsToRemove.add(p);
                    View viewToAnimate = cellViews.get(p);
                    if (viewToAnimate != null) {
                        viewToAnimate.animate()
                                .alpha(0f)
                                .setDuration(animationDuration)
                                .setStartDelay(animationCounter * 50)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {

                                        gameBoard[p.row][p.col] = 0;
                                        if (pointsToRemove.indexOf(p) == pointsToRemove.size() - 1) {
                                            moveAndFill();
                                            displayGameBoard();
                                            List<Set<Point>> newMatches = findAllMatches();
                                            if (!newMatches.isEmpty()) {
                                                animateAndRemoveMatches(newMatches);
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
        }
    }

    private void moveAndFill() {
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
        }

        Random random = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (gameBoard[i][j] == 0) {
                    gameBoard[i][j] = possibleFruitDrawables[random.nextInt(possibleFruitDrawables.length)];
                }
            }
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
}