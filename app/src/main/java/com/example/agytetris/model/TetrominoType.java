package com.example.agytetris.model;

/**
 * Defines the 7 standard tetrominoes with their standard shapes and vibrant arcade ARGB colors.
 */
public enum TetrominoType {
    I(new int[][]{
            {0, 0, 0, 0},
            {1, 1, 1, 1},
            {0, 0, 0, 0},
            {0, 0, 0, 0}
    }, 0xFF00E5FF), // Cyan

    J(new int[][]{
            {1, 0, 0},
            {1, 1, 1},
            {0, 0, 0}
    }, 0xFF2979FF), // Blue

    L(new int[][]{
            {0, 0, 1},
            {1, 1, 1},
            {0, 0, 0}
    }, 0xFFFF9100), // Orange

    O(new int[][]{
            {1, 1},
            {1, 1}
    }, 0xFFFFD600), // Yellow

    S(new int[][]{
            {0, 1, 1},
            {1, 1, 0},
            {0, 0, 0}
    }, 0xFF00E676), // Green

    T(new int[][]{
            {0, 1, 0},
            {1, 1, 1},
            {0, 0, 0}
    }, 0xFFD500F9), // Purple

    Z(new int[][]{
            {1, 1, 0},
            {0, 1, 1},
            {0, 0, 0}
    }, 0xFFFF1744); // Red

    private final int[][] baseMatrix;
    private final int color;

    TetrominoType(int[][] baseMatrix, int color) {
        this.baseMatrix = baseMatrix;
        this.color = color;
    }

    public int[][] getBaseMatrix() {
        int rows = baseMatrix.length;
        int cols = baseMatrix[0].length;
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(baseMatrix[r], 0, copy[r], 0, cols);
        }
        return copy;
    }

    public int getColor() {
        return color;
    }
}
