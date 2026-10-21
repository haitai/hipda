package net.jejer.hipda.ui.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A background image drawable.
 *
 * The bitmap is drawn with center-crop scaling, so it always fills the view
 * without distortion, whatever the aspect ratio of the source image is.
 *
 * A translucent scrim is drawn on top of the image, so that the foreground
 * text of the app stays readable whatever the image looks like.
 */
public class BackgroundImageDrawable extends Drawable {

    private final Bitmap mBitmap;
    private final int mScrimColor;
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mMatrix = new Matrix();
    private final RectF mScrimRect = new RectF();
    private int mAlpha = 255;

    public BackgroundImageDrawable(@NonNull Bitmap bitmap, int scrimColor) {
        mBitmap = bitmap;
        mScrimColor = scrimColor;
        mScrimPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateMatrix(bounds);
    }

    /**
     * Compute the scale/translation needed to fill the given bounds with the
     * bitmap, keeping its aspect ratio and cropping the overflowing part.
     */
    private void updateMatrix(Rect bounds) {
        if (mBitmap == null || mBitmap.isRecycled() || bounds.isEmpty())
            return;

        float scale = Math.max(bounds.width() / (float) mBitmap.getWidth(),
                bounds.height() / (float) mBitmap.getHeight());
        float scaledWidth = mBitmap.getWidth() * scale;
        float scaledHeight = mBitmap.getHeight() * scale;
        float left = bounds.left + (bounds.width() - scaledWidth) / 2f;
        float top = bounds.top + (bounds.height() - scaledHeight) / 2f;

        mMatrix.reset();
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate(left, top);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || mBitmap == null || mBitmap.isRecycled())
            return;

        mBitmapPaint.setAlpha(mAlpha);
        canvas.drawBitmap(mBitmap, mMatrix, mBitmapPaint);

        int scrimAlpha = Color.alpha(mScrimColor) * mAlpha / 255;
        if (scrimAlpha > 0) {
            mScrimPaint.setColor(mScrimColor);
            mScrimPaint.setAlpha(scrimAlpha);
            mScrimRect.set(bounds);
            canvas.drawRect(mScrimRect, mScrimPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mBitmapPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
