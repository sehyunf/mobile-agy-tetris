package com.example.agytetris.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.agytetris.model.TetrominoType;

/**
 * Compact View used to preview Next and Hold tetrominoes with centered auto-scaling.
 */
public class PiecePreviewView extends View {

    private TetrominoType pieceType;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF cellRect = new RectF();
    private final RectF boundsRect = new RectF();

    public PiecePreviewView(Context context) {
        super(context);
        init();
    }

    public PiecePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PiecePreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint.setColor(Color.parseColor("#121420"));
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.parseColor("#2A2D3E"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        highlightPaint.setColor(Color.parseColor("#40FFFFFF"));
        highlightPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(Color.parseColor("#40000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
    }

    public void setPieceType(@Nullable TetrominoType pieceType) {
        this.pieceType = pieceType;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        boundsRect.set(2f, 2f, w - 2f, h - 2f);
        float radius = 12f;
        canvas.drawRoundRect(boundsRect, radius, radius, bgPaint);
        canvas.drawRoundRect(boundsRect, radius, radius, borderPaint);

        if (pieceType == null) return;

        int[][] matrix = pieceType.getBaseMatrix();
        int rows = matrix.length;
        int cols = matrix[0].length;

        // Find actual non-empty bounds to center precisely
        int minR = rows, maxR = -1, minC = cols, maxC = -1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (matrix[r][c] != 0) {
                    if (r < minR) minR = r;
                    if (r > maxR) maxR = r;
                    if (c < minC) minC = c;
                    if (c > maxC) maxC = c;
                }
            }
        }

        if (maxR < 0) return;

        int activeRows = maxR - minR + 1;
        int activeCols = maxC - minC + 1;

        float padding = 16f;
        float usableW = w - padding * 2;
        float usableH = h - padding * 2;

        float cellSize = Math.min(usableW / activeCols, usableH / activeRows);
        // Cap cell size so single small pieces don't get absurdly huge
        cellSize = Math.min(cellSize, (Math.min(w, h) / 3.2f));

        float totalW = activeCols * cellSize;
        float totalH = activeRows * cellSize;

        float startX = (w - totalW) / 2f;
        float startY = (h - totalH) / 2f;

        int color = pieceType.getColor();
        blockPaint.setColor(color);
        blockPaint.setStyle(Paint.Style.FILL);

        for (int r = minR; r <= maxR; r++) {
            for (int c = minC; c <= maxC; c++) {
                if (matrix[r][c] != 0) {
                    float left = startX + (c - minC) * cellSize + 1.5f;
                    float top = startY + (r - minR) * cellSize + 1.5f;
                    float right = left + cellSize - 3f;
                    float bottom = top + cellSize - 3f;

                    cellRect.set(left, top, right, bottom);
                    float blockRadius = cellSize * 0.2f;
                    canvas.drawRoundRect(cellRect, blockRadius, blockRadius, blockPaint);

                    RectF hRect = new RectF(left + 2f, top + 2f, right - 2f, top + (cellSize * 0.35f));
                    canvas.drawRoundRect(hRect, blockRadius * 0.8f, blockRadius * 0.8f, highlightPaint);

                    RectF sRect = new RectF(left + 2f, bottom - (cellSize * 0.25f), right - 2f, bottom - 2f);
                    canvas.drawRoundRect(sRect, blockRadius * 0.8f, blockRadius * 0.8f, shadowPaint);
                }
            }
        }
    }
}
