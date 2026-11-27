package com.deliciousdiamond.deepexplorer;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Draws a gradient hairline border that can be revealed from the right edge. */
final class GradientBorderDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors;
    private final float strokeWidth;
    private float fraction;
    private int alpha = 255;

    GradientBorderDrawable(int[] colors, float strokeWidth) {
        this.colors = colors;
        this.strokeWidth = strokeWidth;
    }

    void setFraction(float value) {
        fraction = Math.max(0f, Math.min(1f, value));
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0 || fraction <= 0f) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAlpha(alpha);
        paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                colors, null, Shader.TileMode.CLAMP));
        canvas.save();
        canvas.clipRect(bounds.left + (1f - fraction) * bounds.width(), bounds.top,
                bounds.right, bounds.bottom);
        float inset = strokeWidth / 2f;
        canvas.drawRect(bounds.left + inset, bounds.top + inset,
                bounds.right - inset, bounds.bottom - inset, paint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int value) {
        alpha = value;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
