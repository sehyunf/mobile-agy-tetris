package com.example.agytetris;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.agytetris.engine.GameEngine;
import com.example.agytetris.model.TetrominoType;
import com.example.agytetris.view.PiecePreviewView;
import com.example.agytetris.view.TetrisBoardView;

import java.util.List;

/**
 * Main Activity that coordinates the Tetris UI, input controls, game engine,
 * high score persistence, and haptic feedback.
 */
public class MainActivity extends AppCompatActivity implements GameEngine.GameListener {

    private static final String PREFS_NAME = "AGY_TETRIS_PREFS";
    private static final String KEY_HIGH_SCORE = "KEY_HIGH_SCORE";

    private GameEngine gameEngine;
    private SharedPreferences prefs;
    private Vibrator vibrator;

    // Views
    private TetrisBoardView tetrisBoardView;
    private PiecePreviewView holdPreviewView;
    private PiecePreviewView nextPreviewView;

    private TextView tvScore;
    private TextView tvHighScore;
    private TextView tvLevel;
    private TextView tvLines;

    private LinearLayout overlayGameOver;
    private LinearLayout overlayPause;
    private TextView tvFinalScore;
    private TextView tvNewRecordBadge;

    private Button btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply Edge-to-edge system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initViews();
        initGameEngine();
        initControls();

        // Start game
        gameEngine.startNewGame();
    }

    private void initViews() {
        tetrisBoardView = findViewById(R.id.tetrisBoardView);
        holdPreviewView = findViewById(R.id.holdPreviewView);
        nextPreviewView = findViewById(R.id.nextPreviewView);

        tvScore = findViewById(R.id.tvScore);
        tvHighScore = findViewById(R.id.tvHighScore);
        tvLevel = findViewById(R.id.tvLevel);
        tvLines = findViewById(R.id.tvLines);

        overlayGameOver = findViewById(R.id.overlayGameOver);
        overlayPause = findViewById(R.id.overlayPause);
        tvFinalScore = findViewById(R.id.tvFinalScore);
        tvNewRecordBadge = findViewById(R.id.tvNewRecordBadge);

        btnPause = findViewById(R.id.btnPause);
    }

    private void initGameEngine() {
        gameEngine = new GameEngine();
        gameEngine.setListener(this);

        int savedHighScore = prefs.getInt(KEY_HIGH_SCORE, 0);
        gameEngine.setHighScore(savedHighScore);
        tvHighScore.setText(String.valueOf(savedHighScore));
    }

    private void initControls() {
        findViewById(R.id.btnLeft).setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.moveLeft();
        });

        findViewById(R.id.btnRight).setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.moveRight();
        });

        findViewById(R.id.btnSoftDrop).setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.softDrop();
        });

        findViewById(R.id.btnRotateCW).setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.rotateClockwise();
        });

        findViewById(R.id.btnHardDrop).setOnClickListener(v -> {
            performHaptic(v, true);
            gameEngine.hardDrop();
        });

        findViewById(R.id.btnHold).setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.hold();
        });

        holdPreviewView.setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.hold();
        });

        btnPause.setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.togglePause();
        });

        findViewById(R.id.btnResume).setOnClickListener(v -> {
            performHaptic(v, false);
            gameEngine.resumeGame();
        });

        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            performHaptic(v, false);
            overlayPause.setVisibility(View.GONE);
            gameEngine.startNewGame();
        });

        findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            performHaptic(v, false);
            overlayGameOver.setVisibility(View.GONE);
            gameEngine.startNewGame();
        });
    }

    private void performHaptic(View view, boolean strong) {
        if (view != null) {
            view.performHapticFeedback(
                    strong ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.KEYBOARD_TAP
            );
        }
    }

    private void triggerCustomVibration(int durationMs) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(durationMs);
            }
        }
    }

    // ==========================================
    // GameEngine.GameListener callbacks
    // ==========================================

    @Override
    public void onGameStateChanged(GameEngine.GameState state) {
        runOnUiThread(() -> {
            switch (state) {
                case RUNNING:
                    overlayPause.setVisibility(View.GONE);
                    overlayGameOver.setVisibility(View.GONE);
                    btnPause.setText("⏸");
                    break;
                case PAUSED:
                    overlayPause.setVisibility(View.VISIBLE);
                    btnPause.setText("▶");
                    break;
                case GAME_OVER:
                    int finalScore = gameEngine.getScore();
                    int highScore = gameEngine.getHighScore();
                    tvFinalScore.setText(getString(R.string.score_label) + ": " + finalScore);

                    if (finalScore >= highScore && finalScore > 0) {
                        tvNewRecordBadge.setVisibility(View.VISIBLE);
                        prefs.edit().putInt(KEY_HIGH_SCORE, finalScore).apply();
                    } else {
                        tvNewRecordBadge.setVisibility(View.GONE);
                    }

                    overlayGameOver.setVisibility(View.VISIBLE);
                    triggerCustomVibration(250);
                    break;
                case READY:
                    break;
            }
        });
    }

    @Override
    public void onScoreChanged(int score, int highScore, int level, int lines) {
        runOnUiThread(() -> {
            tvScore.setText(String.valueOf(score));
            tvHighScore.setText(String.valueOf(highScore));
            tvLevel.setText(String.valueOf(level));
            tvLines.setText(String.valueOf(lines));

            // Persist high score if beaten
            int savedHigh = prefs.getInt(KEY_HIGH_SCORE, 0);
            if (score > savedHigh) {
                prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
            }
        });
    }

    @Override
    public void onBoardUpdated() {
        runOnUiThread(() -> {
            tetrisBoardView.setGameData(gameEngine.getBoard(), gameEngine.getCurrentPiece());
        });
    }

    @Override
    public void onHoldPieceUpdated(TetrominoType holdPiece) {
        runOnUiThread(() -> {
            holdPreviewView.setPieceType(holdPiece);
        });
    }

    @Override
    public void onNextPiecesUpdated(List<TetrominoType> nextPieces) {
        runOnUiThread(() -> {
            if (nextPieces != null && !nextPieces.isEmpty()) {
                nextPreviewView.setPieceType(nextPieces.get(0));
            } else {
                nextPreviewView.setPieceType(null);
            }
        });
    }

    @Override
    public void onLinesCleared(int count) {
        runOnUiThread(() -> {
            // Trigger dynamic screen shake on the board
            if (tetrisBoardView != null) {
                tetrisBoardView.triggerShake(count);
            }

            // Punchy scale animation on lines & score indicators
            if (tvLines != null) {
                tvLines.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100)
                        .withEndAction(() -> tvLines.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                        .start();
            }
            if (tvScore != null) {
                tvScore.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                        .withEndAction(() -> tvScore.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                        .start();
            }

            // Haptic feedback proportional to lines cleared (e.g. Quad Tetris has a bigger buzz)
            triggerCustomVibration(count >= 4 ? 120 : 60);
        });
    }

    @Override
    public void onPieceLocked() {
        // Small feedback on piece lock
    }

    @Override
    public void onHardDrop() {
        runOnUiThread(() -> {
            if (tetrisBoardView != null) {
                tetrisBoardView.triggerDropShake();
            }
            triggerCustomVibration(45);
        });
    }

    // ==========================================
    // Lifecycle Management
    // ==========================================

    @Override
    protected void onPause() {
        super.onPause();
        if (gameEngine != null && gameEngine.getState() == GameEngine.GameState.RUNNING) {
            gameEngine.pauseGame();
        }
    }
}
