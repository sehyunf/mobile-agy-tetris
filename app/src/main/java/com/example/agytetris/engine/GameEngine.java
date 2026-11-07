package com.example.agytetris.engine;

import android.os.Handler;
import android.os.Looper;

import com.example.agytetris.model.Board;
import com.example.agytetris.model.Tetromino;
import com.example.agytetris.model.TetrominoType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Core Tetris game engine.
 * Handles the game loop, 7-bag randomizer, piece movement, hold mechanism,
 * level/speed progression, and scoring.
 */
public class GameEngine {

    public enum GameState {
        READY,
        RUNNING,
        PAUSED,
        GAME_OVER
    }

    public interface GameListener {
        void onGameStateChanged(GameState state);
        void onScoreChanged(int score, int highScore, int level, int lines);
        void onBoardUpdated();
        void onHoldPieceUpdated(TetrominoType holdPiece);
        void onNextPiecesUpdated(List<TetrominoType> nextPieces);
        void onLinesCleared(int count);
        void onPieceLocked();
        void onHardDrop();
    }

    private final Board board;
    private final Handler handler;
    private GameListener listener;

    private GameState state = GameState.READY;
    private Tetromino currentPiece;
    private TetrominoType holdPiece;
    private boolean canHold = true;

    private final LinkedList<TetrominoType> bagQueue = new LinkedList<>();
    private final LinkedList<TetrominoType> nextQueue = new LinkedList<>();
    private static final int NEXT_PREVIEW_COUNT = 3;

    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private int linesCleared = 0;

    private final Runnable gameTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (state == GameState.RUNNING) {
                step();
                handler.postDelayed(this, getTickInterval());
            }
        }
    };

    public GameEngine() {
        this.board = new Board();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    public Board getBoard() {
        return board;
    }

    public Tetromino getCurrentPiece() {
        return currentPiece;
    }

    public TetrominoType getHoldPiece() {
        return holdPiece;
    }

    public List<TetrominoType> getNextPieces() {
        return new ArrayList<>(nextQueue);
    }

    public GameState getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getHighScore() {
        return highScore;
    }

    public void setHighScore(int highScore) {
        this.highScore = highScore;
    }

    public int getLevel() {
        return level;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    /**
     * Starts or restarts a new game.
     */
    public void startNewGame() {
        handler.removeCallbacks(gameTickRunnable);
        board.clear();
        bagQueue.clear();
        nextQueue.clear();
        score = 0;
        level = 1;
        linesCleared = 0;
        holdPiece = null;
        canHold = true;

        refillBagIfNeeded();
        while (nextQueue.size() < NEXT_PREVIEW_COUNT) {
            nextQueue.add(takeFromBag());
        }

        spawnNextPiece();
        setState(GameState.RUNNING);
        notifyStats();
        notifyNextQueue();
        notifyHoldPiece();
        handler.postDelayed(gameTickRunnable, getTickInterval());
    }

    public void pauseGame() {
        if (state == GameState.RUNNING) {
            handler.removeCallbacks(gameTickRunnable);
            setState(GameState.PAUSED);
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            setState(GameState.RUNNING);
            handler.postDelayed(gameTickRunnable, getTickInterval());
        }
    }

    public void togglePause() {
        if (state == GameState.RUNNING) {
            pauseGame();
        } else if (state == GameState.PAUSED) {
            resumeGame();
        }
    }

    /**
     * Executes one gravitational downward step.
     */
    public synchronized void step() {
        if (state != GameState.RUNNING || currentPiece == null) {
            return;
        }

        if (!board.tryMove(currentPiece, 0, 1)) {
            lockAndAdvance();
        } else {
            notifyBoardUpdated();
        }
    }

    /**
     * User inputs
     */
    public synchronized boolean moveLeft() {
        if (state != GameState.RUNNING || currentPiece == null) return false;
        boolean moved = board.tryMove(currentPiece, -1, 0);
        if (moved) notifyBoardUpdated();
        return moved;
    }

    public synchronized boolean moveRight() {
        if (state != GameState.RUNNING || currentPiece == null) return false;
        boolean moved = board.tryMove(currentPiece, 1, 0);
        if (moved) notifyBoardUpdated();
        return moved;
    }

    public synchronized boolean rotateClockwise() {
        if (state != GameState.RUNNING || currentPiece == null) return false;
        boolean rotated = board.tryRotate(currentPiece, true);
        if (rotated) notifyBoardUpdated();
        return rotated;
    }

    public synchronized boolean rotateCounterClockwise() {
        if (state != GameState.RUNNING || currentPiece == null) return false;
        boolean rotated = board.tryRotate(currentPiece, false);
        if (rotated) notifyBoardUpdated();
        return rotated;
    }

    public synchronized boolean softDrop() {
        if (state != GameState.RUNNING || currentPiece == null) return false;
        boolean moved = board.tryMove(currentPiece, 0, 1);
        if (moved) {
            score += 1;
            notifyStats();
            notifyBoardUpdated();
        } else {
            lockAndAdvance();
        }
        return moved;
    }

    public synchronized void hardDrop() {
        if (state != GameState.RUNNING || currentPiece == null) return;
        int dropCells = 0;
        while (board.tryMove(currentPiece, 0, 1)) {
            dropCells++;
        }
        score += dropCells * 2;
        if (listener != null) {
            listener.onHardDrop();
        }
        lockAndAdvance();
    }

    public synchronized boolean hold() {
        if (state != GameState.RUNNING || currentPiece == null || !canHold) return false;

        TetrominoType currentType = currentPiece.getType();
        if (holdPiece == null) {
            holdPiece = currentType;
            spawnNextPiece();
        } else {
            TetrominoType temp = holdPiece;
            holdPiece = currentType;
            currentPiece = new Tetromino(temp);
        }
        canHold = false;
        notifyHoldPiece();
        notifyBoardUpdated();
        return true;
    }

    private void lockAndAdvance() {
        board.lockPiece(currentPiece);
        if (listener != null) {
            listener.onPieceLocked();
        }

        List<Integer> cleared = board.clearLines();
        if (!cleared.isEmpty()) {
            int count = cleared.size();
            linesCleared += count;

            int lineScore = 0;
            switch (count) {
                case 1: lineScore = 100 * level; break;
                case 2: lineScore = 300 * level; break;
                case 3: lineScore = 500 * level; break;
                case 4: lineScore = 800 * level; break;
                default: lineScore = 1000 * level; break;
            }
            score += lineScore;

            // Level up every 10 lines
            level = (linesCleared / 10) + 1;

            if (score > highScore) {
                highScore = score;
            }

            if (listener != null) {
                listener.onLinesCleared(count);
            }
        }

        canHold = true;
        spawnNextPiece();
        notifyStats();
        notifyBoardUpdated();
    }

    private void spawnNextPiece() {
        TetrominoType nextType = nextQueue.poll();
        nextQueue.add(takeFromBag());
        notifyNextQueue();

        currentPiece = new Tetromino(nextType);
        if (!board.isValidPosition(currentPiece)) {
            // Collision immediately on spawn => GAME OVER
            state = GameState.GAME_OVER;
            handler.removeCallbacks(gameTickRunnable);
            if (listener != null) {
                listener.onGameStateChanged(state);
            }
        }
    }

    private TetrominoType takeFromBag() {
        refillBagIfNeeded();
        return bagQueue.poll();
    }

    private void refillBagIfNeeded() {
        if (bagQueue.isEmpty()) {
            List<TetrominoType> bag = new ArrayList<>();
            Collections.addAll(bag, TetrominoType.values());
            Collections.shuffle(bag);
            bagQueue.addAll(bag);
        }
    }

    private long getTickInterval() {
        // Drop speed: Level 1 starts at 750ms, decreases with level down to minimum 80ms
        return Math.max(80, 750 - (level - 1) * 60L);
    }

    private void setState(GameState newState) {
        this.state = newState;
        if (listener != null) {
            listener.onGameStateChanged(newState);
        }
    }

    private void notifyStats() {
        if (listener != null) {
            listener.onScoreChanged(score, highScore, level, linesCleared);
        }
    }

    private void notifyBoardUpdated() {
        if (listener != null) {
            listener.onBoardUpdated();
        }
    }

    private void notifyNextQueue() {
        if (listener != null) {
            listener.onNextPiecesUpdated(new ArrayList<>(nextQueue));
        }
    }

    private void notifyHoldPiece() {
        if (listener != null) {
            listener.onHoldPieceUpdated(holdPiece);
        }
    }
}
