package com.example.agytetris.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.example.agytetris.model.Board;
import com.example.agytetris.model.Tetromino;

/**
 * Custom View that renders the 10x20 Tetris board, locked blocks,
 * the ghost piece (landing preview), and the active falling piece.
 * Includes dynamic screen shake effects on line clears.
 */
public class TetrisBoardView extends View {

    private Board board;
    private Tetromino currentPiece;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF cellRect = new RectF();
    private final RectF boardRect = new RectF();

    private float cellSize = 0f;
    private float boardOffsetX = 0f;
    private float boardOffsetY = 0f;

    private ValueAnimator shakeAnimator;

    public TetrisBoardView(Context context) {
        super(context);
        init();
    }

    public TetrisBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TetrisBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint.setColor(Color.parseColor("#121420"));
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.parseColor("#2A2D3E"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        gridPaint.setColor(Color.parseColor("#1C1E2D"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.2f);

        blockHighlightPaint.setColor(Color.parseColor("#40FFFFFF")); // 25% white highlight
        blockHighlightPaint.setStyle(Paint.Style.FILL);

        blockShadowPaint.setColor(Color.parseColor("#40000000")); // 25% black shadow
        blockShadowPaint.setStyle(Paint.Style.FILL);

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(2.5f);
    }

    public void setGameData(Board board, Tetromino currentPiece) {
        this.board = board;
        this.currentPiece = currentPiece;
        invalidate();
    }

    /**
     * Triggers dynamic screen shake effect proportional to the number of lines cleared.
     * @param linesCount number of lines cleared (1 to 4)
     */
    public void triggerShake(int linesCount) {
        if (shakeAnimator != null && shakeAnimator.isRunning()) {
            shakeAnimator.cancel();
        }

        float density = getContext().getResources().getDisplayMetrics().density;
        // Base amplitude: 1 line = ~9dp, 2 lines = ~14dp, 3 lines = ~19dp, 4 lines (Tetris!) = ~25dp
        float maxOffset = (5f + linesCount * 4.5f) * density;
        long duration = Math.min(480, 180 + linesCount * 70);
        float cycles = 5f + linesCount * 1.5f;
        float maxRotation = linesCount >= 3 ? (linesCount == 4 ? 2.5f : 1.2f) : 0f;

        shakeAnimator = ValueAnimator.ofFloat(1f, 0f);
        shakeAnimator.setDuration(duration);
        shakeAnimator.setInterpolator(new DecelerateInterpolator());
        shakeAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue(); // 1.0 down to 0.0
            float sinVal = (float) Math.sin(fraction * Math.PI * 2 * cycles);
            float cosVal = (float) Math.cos(fraction * Math.PI * 2 * cycles * 0.7f);

            setTranslationX(sinVal * maxOffset * fraction);
            setTranslationY(cosVal * (maxOffset * 0.6f) * fraction);
            if (maxRotation > 0f) {
                setRotation(sinVal * maxRotation * fraction);
            }
        });
        shakeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetTransforms();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                resetTransforms();
            }

            private void resetTransforms() {
                setTranslationX(0f);
                setTranslationY(0f);
                setRotation(0f);
            }
        });
        shakeAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (shakeAnimator != null) {
            shakeAnimator.cancel();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float paddingLeft = getPaddingLeft();
        float paddingRight = getPaddingRight();
        float paddingTop = getPaddingTop();
        float paddingBottom = getPaddingBottom();

        float usableW = w - paddingLeft - paddingRight;
        float usableH = h - paddingTop - paddingBottom;

        // Board is 10 wide by 20 high
        cellSize = Math.min(usableW / Board.WIDTH, usableH / Board.HEIGHT);

        float totalBoardW = cellSize * Board.WIDTH;
        float totalBoardH = cellSize * Board.HEIGHT;

        boardOffsetX = paddingLeft + (usableW - totalBoardW) / 2f;
        boardOffsetY = paddingTop + (usableH - totalBoardH) / 2f;

        boardRect.set(boardOffsetX, boardOffsetY, boardOffsetX + totalBoardW, boardOffsetY + totalBoardH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (board == null) return;

        // 1. Draw Board Background with rounded rect
        float cornerRadius = cellSize * 0.25f;
        canvas.drawRoundRect(boardRect, cornerRadius, cornerRadius, bgPaint);
        canvas.drawRoundRect(boardRect, cornerRadius, cornerRadius, borderPaint);

        // 2. Draw Grid Lines
        for (int c = 1; c < Board.WIDTH; c++) {
            float x = boardOffsetX + c * cellSize;
            canvas.drawLine(x, boardOffsetY, x, boardOffsetY + Board.HEIGHT * cellSize, gridPaint);
        }
        for (int r = 1; r < Board.HEIGHT; r++) {
            float y = boardOffsetY + r * cellSize;
            canvas.drawLine(boardOffsetX, y, boardOffsetX + Board.WIDTH * cellSize, y, gridPaint);
        }

        // 3. Draw Locked Blocks on Board
        int[][] grid = board.getGrid();
        for (int r = 0; r < Board.HEIGHT; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                int color = grid[r][c];
                if (color != 0) {
                    drawBlock(canvas, c, r, color, 255);
                }
            }
        }

        // 4. Draw Ghost Piece (landing preview)
        if (currentPiece != null) {
            int ghostY = board.getGhostY(currentPiece);
            if (ghostY != currentPiece.getY()) {
                drawGhostPiece(canvas, currentPiece, ghostY);
            }
        }

        // 5. Draw Active Falling Piece
        if (currentPiece != null) {
            int[][] matrix = currentPiece.getMatrix();
            int color = currentPiece.getColor();
            int startX = currentPiece.getX();
            int startY = currentPiece.getY();

            for (int r = 0; r < matrix.length; r++) {
                for (int c = 0; c < matrix[r].length; c++) {
                    if (matrix[r][c] != 0) {
                        int boardX = startX + c;
                        int boardY = startY + r;
                        if (boardY >= 0 && boardY < Board.HEIGHT && boardX >= 0 && boardX < Board.WIDTH) {
                            drawBlock(canvas, boardX, boardY, color, 255);
                        }
                    }
                }
            }
        }
    }

    private void drawBlock(Canvas canvas, int col, int row, int color, int alpha) {
        float left = boardOffsetX + col * cellSize + 1.5f;
        float top = boardOffsetY + row * cellSize + 1.5f;
        float right = left + cellSize - 3f;
        float bottom = top + cellSize - 3f;

        cellRect.set(left, top, right, bottom);
        float radius = cellSize * 0.2f;

        blockPaint.setColor(color);
        blockPaint.setAlpha(alpha);
        blockPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cellRect, radius, radius, blockPaint);

        // Highlight upper edge
        RectF highlightRect = new RectF(left + 2f, top + 2f, right - 2f, top + (cellSize * 0.35f));
        canvas.drawRoundRect(highlightRect, radius * 0.8f, radius * 0.8f, blockHighlightPaint);

        // Subtle shadow on bottom edge
        RectF shadowRect = new RectF(left + 2f, bottom - (cellSize * 0.25f), right - 2f, bottom - 2f);
        canvas.drawRoundRect(shadowRect, radius * 0.8f, radius * 0.8f, blockShadowPaint);
    }

    private void drawGhostPiece(Canvas canvas, Tetromino piece, int ghostY) {
        int[][] matrix = piece.getMatrix();
        int color = piece.getColor();
        int startX = piece.getX();

        ghostPaint.setColor(color);
        ghostPaint.setAlpha(100);

        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                if (matrix[r][c] != 0) {
                    int boardX = startX + c;
                    int boardY = ghostY + r;
                    if (boardY >= 0 && boardY < Board.HEIGHT && boardX >= 0 && boardX < Board.WIDTH) {
                        float left = boardOffsetX + boardX * cellSize + 2f;
                        float top = boardOffsetY + boardY * cellSize + 2f;
                        float right = left + cellSize - 4f;
                        float bottom = top + cellSize - 4f;

                        cellRect.set(left, top, right, bottom);
                        float radius = cellSize * 0.2f;
                        canvas.drawRoundRect(cellRect, radius, radius, ghostPaint);
                    }
                }
            }
        }
    }
}
