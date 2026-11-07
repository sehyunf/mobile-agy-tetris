package com.example.agytetris.model;

/**
 * Represents an active tetromino piece with position (x, y) and rotation matrix.
 */
public class Tetromino {
    private final TetrominoType type;
    private int[][] matrix;
    private int x;
    private int y;

    public Tetromino(TetrominoType type) {
        this.type = type;
        this.matrix = type.getBaseMatrix();
        // Initial spawn position: centered horizontally at top
        this.x = 5 - (matrix[0].length / 2);
        this.y = 0;
    }

    public Tetromino(TetrominoType type, int[][] matrix, int x, int y) {
        this.type = type;
        this.matrix = matrix;
        this.x = x;
        this.y = y;
    }

    public TetrominoType getType() {
        return type;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getColor() {
        return type.getColor();
    }

    /**
     * Rotates matrix clockwise (90 degrees).
     */
    public static int[][] rotateMatrixClockwise(int[][] src) {
        int rows = src.length;
        int cols = src[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = src[r][c];
            }
        }
        return rotated;
    }

    /**
     * Rotates matrix counter-clockwise (90 degrees).
     */
    public static int[][] rotateMatrixCounterClockwise(int[][] src) {
        int rows = src.length;
        int cols = src[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[cols - 1 - c][r] = src[r][c];
            }
        }
        return rotated;
    }

    public void rotateClockwise() {
        this.matrix = rotateMatrixClockwise(this.matrix);
    }

    public void rotateCounterClockwise() {
        this.matrix = rotateMatrixCounterClockwise(this.matrix);
    }

    public Tetromino copy() {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] newMatrix = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(matrix[r], 0, newMatrix[r], 0, cols);
        }
        return new Tetromino(type, newMatrix, x, y);
    }
}
