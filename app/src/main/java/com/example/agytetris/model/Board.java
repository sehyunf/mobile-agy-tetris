package com.example.agytetris.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 10x20 Tetris playing field matrix.
 * Handles collision detection, wall kicks, piece locking, ghost position calculation, and line clears.
 */
public class Board {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 20;

    // 0 means empty cell, otherwise contains the ARGB color of the locked block
    private final int[][] grid;

    public Board() {
        this.grid = new int[HEIGHT][WIDTH];
    }

    public int[][] getGrid() {
        return grid;
    }

    public int getCell(int row, int col) {
        if (row >= 0 && row < HEIGHT && col >= 0 && col < WIDTH) {
            return grid[row][col];
        }
        return 0;
    }

    public void setCell(int row, int col, int color) {
        if (row >= 0 && row < HEIGHT && col >= 0 && col < WIDTH) {
            grid[row][col] = color;
        }
    }

    public void clear() {
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                grid[r][c] = 0;
            }
        }
    }

    /**
     * Checks if a tetromino in the specified position and rotation matrix is valid (in bounds and no collision).
     */
    public boolean isValidPosition(int x, int y, int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (matrix[r][c] != 0) {
                    int boardX = x + c;
                    int boardY = y + r;

                    // Check horizontal boundaries
                    if (boardX < 0 || boardX >= WIDTH) {
                        return false;
                    }
                    // Check bottom boundary
                    if (boardY >= HEIGHT) {
                        return false;
                    }
                    // Check collision with already placed blocks
                    if (boardY >= 0 && grid[boardY][boardX] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isValidPosition(Tetromino piece) {
        return isValidPosition(piece.getX(), piece.getY(), piece.getMatrix());
    }

    /**
     * Tries to move the piece by (dx, dy). Returns true if successful.
     */
    public boolean tryMove(Tetromino piece, int dx, int dy) {
        if (isValidPosition(piece.getX() + dx, piece.getY() + dy, piece.getMatrix())) {
            piece.setX(piece.getX() + dx);
            piece.setY(piece.getY() + dy);
            return true;
        }
        return false;
    }

    /**
     * Tries to rotate the piece with standard wall-kicks (kicks: 0, -1, +1, -2, +2, etc.).
     */
    public boolean tryRotate(Tetromino piece, boolean clockwise) {
        int[][] rotated = clockwise ?
                Tetromino.rotateMatrixClockwise(piece.getMatrix()) :
                Tetromino.rotateMatrixCounterClockwise(piece.getMatrix());

        // Basic wall-kick offsets to test
        int[][] kicks = {
                {0, 0},   // standard rotation
                {-1, 0},  // kick left
                {1, 0},   // kick right
                {-2, 0},  // kick left 2 (for I-piece near wall)
                {2, 0},   // kick right 2
                {0, -1}   // floor kick / upward
        };

        for (int[] kick : kicks) {
            int testX = piece.getX() + kick[0];
            int testY = piece.getY() + kick[1];
            if (isValidPosition(testX, testY, rotated)) {
                if (clockwise) {
                    piece.rotateClockwise();
                } else {
                    piece.rotateCounterClockwise();
                }
                piece.setX(testX);
                piece.setY(testY);
                return true;
            }
        }
        return false;
    }

    /**
     * Locks the given tetromino into the board grid.
     */
    public void lockPiece(Tetromino piece) {
        int[][] matrix = piece.getMatrix();
        int rows = matrix.length;
        int cols = matrix[0].length;
        int color = piece.getColor();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (matrix[r][c] != 0) {
                    int boardX = piece.getX() + c;
                    int boardY = piece.getY() + r;
                    if (boardY >= 0 && boardY < HEIGHT && boardX >= 0 && boardX < WIDTH) {
                        grid[boardY][boardX] = color;
                    }
                }
            }
        }
    }

    /**
     * Calculates the Y coordinate where the piece would land (ghost piece).
     */
    public int getGhostY(Tetromino piece) {
        int ghostY = piece.getY();
        while (isValidPosition(piece.getX(), ghostY + 1, piece.getMatrix())) {
            ghostY++;
        }
        return ghostY;
    }

    /**
     * Detects and clears all completely filled lines, shifting upper rows down.
     * Returns the list of row indices that were cleared.
     */
    public List<Integer> clearLines() {
        List<Integer> clearedRows = new ArrayList<>();
        for (int r = HEIGHT - 1; r >= 0; r--) {
            boolean isFull = true;
            for (int c = 0; c < WIDTH; c++) {
                if (grid[r][c] == 0) {
                    isFull = false;
                    break;
                }
            }

            if (isFull) {
                clearedRows.add(r);
                // Shift all rows above down by 1
                for (int rowAbove = r; rowAbove > 0; rowAbove--) {
                    System.arraycopy(grid[rowAbove - 1], 0, grid[rowAbove], 0, WIDTH);
                }
                // Clear top row
                for (int c = 0; c < WIDTH; c++) {
                    grid[0][c] = 0;
                }
                // Since row r now has content from row above, test it again
                r++;
            }
        }
        return clearedRows;
    }
}
