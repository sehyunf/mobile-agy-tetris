package com.example.agytetris;

import com.example.agytetris.model.Board;
import com.example.agytetris.model.Tetromino;
import com.example.agytetris.model.TetrominoType;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TetrisLogicTest {

    private Board board;

    @Before
    public void setUp() {
        board = new Board();
    }

    @Test
    public void testTetrominoTypesHaveValidMatricesAndColors() {
        TetrominoType[] types = TetrominoType.values();
        assertEquals(7, types.length);

        for (TetrominoType type : types) {
            int[][] matrix = type.getBaseMatrix();
            assertNotNull(matrix);
            assertTrue(matrix.length > 0);
            assertTrue(matrix[0].length > 0);
            assertNotEquals(0, type.getColor());
        }
    }

    @Test
    public void testTetrominoRotation() {
        Tetromino piece = new Tetromino(TetrominoType.T);
        int[][] originalMatrix = piece.getMatrix();

        // Rotate 4 times clockwise -> should return to original orientation
        piece.rotateClockwise();
        piece.rotateClockwise();
        piece.rotateClockwise();
        piece.rotateClockwise();

        assertArrayEquals(originalMatrix, piece.getMatrix());
    }

    @Test
    public void testBoardBoundaryCollision() {
        Tetromino piece = new Tetromino(TetrominoType.O); // 2x2 block
        piece.setX(0);
        piece.setY(0);
        assertTrue(board.isValidPosition(piece));

        // Moving left past left edge
        assertFalse(board.isValidPosition(-1, 0, piece.getMatrix()));

        // Moving right past right edge (width is 10, 2x2 at x=9 reaches x=10 => invalid)
        assertFalse(board.isValidPosition(9, 0, piece.getMatrix()));

        // Moving down past bottom edge (height is 20, 2x2 at y=19 reaches y=20 => invalid)
        assertFalse(board.isValidPosition(0, 19, piece.getMatrix()));
    }

    @Test
    public void testLineClearAndRowShift() {
        // Fill row 19 completely with a color
        int testColor = 0xFF00FF00;
        for (int c = 0; c < Board.WIDTH; c++) {
            board.setCell(19, c, testColor);
        }

        // Put a marker cell in row 18
        int markerColor = 0xFFFF0000;
        board.setCell(18, 4, markerColor);

        // Clear lines
        List<Integer> cleared = board.clearLines();
        assertEquals(1, cleared.size());
        assertEquals(Integer.valueOf(19), cleared.get(0));

        // Row 19 should now contain the marker cell from row 18
        assertEquals(markerColor, board.getCell(19, 4));
        // Row 0 should be empty
        assertEquals(0, board.getCell(0, 4));
    }

    @Test
    public void testGhostPieceDropCalculation() {
        Tetromino piece = new Tetromino(TetrominoType.I); // 4x4 matrix, line on row 1
        piece.setX(3);
        piece.setY(0);

        int ghostY = board.getGhostY(piece);
        // The I-piece matrix has its bar on row 1, so row 1 reaches row 19 when ghostY is 18
        assertTrue(ghostY > piece.getY());
        assertTrue(ghostY <= 18);
    }
}
