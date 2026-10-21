package net.jejer.hipda.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.core.graphics.ColorUtils;
import androidx.exifinterface.media.ExifInterface;

import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.ui.widget.BackgroundImageDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Manage the background image of the app.
 *
 * The image picked by the user is copied into the private storage of the app,
 * so that it stays available even if the original file is moved or deleted,
 * and no runtime permission is needed to read it later.
 */
public class BackgroundHelper {

    private static final String DIR_NAME = "background";
    private static final String FILE_PREFIX = "bg_";
    private static final String FILE_SUFFIX = ".jpg";
    private static final String TEMP_FILE_NAME = "temp_import";

    /** Max size (in pixel) of the image kept in the private storage. */
    private static final int MAX_STORED_SIZE = 1600;
    /** Max size (in pixel) of the bitmap decoded for displaying. */
    private static final int MAX_DISPLAY_SIZE = 1440;

    private static Bitmap sDisplayBitmap;
    private static String sDisplayKey;

    private BackgroundHelper() {
    }

    public static File getBackgroundDir() {
        return Utils.getFilesSubDir(DIR_NAME);
    }

    /**
     * @return the file of the background image in use, null if there is none.
     */
    public static File getBackgroundFile() {
        String name = HiSettingsHelper.getInstance().getBackgroundImage();
        if (TextUtils.isEmpty(name))
            return null;
        File file = new File(getBackgroundDir(), name);
        return file.exists() ? file : null;
    }

    /**
     * @return true if a background image is set and still available.
     */
    public static boolean isEnabled() {
        return getBackgroundFile() != null;
    }

    /**
     * Build the drawable used as background of the app.
     *
     * @return the drawable, or null if no background image is available.
     */
    public static Drawable buildDrawable(Context context) {
        Bitmap bitmap = getDisplayBitmap();
        if (bitmap == null || context == null)
            return null;

        int scrimColor = ColorUtils.setAlphaComponent(
                ColorHelper.getWindowBackgroundColor(context), getMaskAlpha());
        return new BackgroundImageDrawable(bitmap, scrimColor);
    }

    /**
     * Apply the background image to the given view, if one is set.
     *
     * The view keeps its original background when no image is set, so that the
     * default solid color of the current theme is used instead.
     *
     * @return true if the background image was applied.
     */
    public static boolean applyTo(View view, Context context) {
        if (view == null)
            return false;

        Drawable drawable = buildDrawable(context);
        if (drawable == null)
            return false;

        view.setBackground(drawable);
        return true;
    }

    /**
     * Copy the image of the given uri into the private storage of the app.
     *
     * The uri is read right away, so that it stays readable even if the
     * permission granted by the picker is only valid for a short while.
     *
     * @return the file holding the imported image, null on failure.
     */
    public static File copyToTempFile(Uri uri) {
        if (uri == null)
            return null;
        try {
            File temp = new File(getBackgroundDir(), TEMP_FILE_NAME);
            Utils.copy(uri, temp);
            return temp;
        } catch (Exception e) {
            Logger.e(e);
            return null;
        }
    }

    /**
     * Scale down the given image and store it as the background image.
     *
     * @return the name of the stored file, null on failure.
     */
    public static String importFromFile(File temp) {
        if (temp == null || !temp.exists())
            return null;

        Bitmap bitmap = null;
        try {
            int rotation = readRotation(temp);
            bitmap = decodeScaled(temp, MAX_STORED_SIZE);
            if (bitmap == null)
                return null;

            if (rotation != 0)
                bitmap = rotate(bitmap, rotation);

            String name = FILE_PREFIX + System.currentTimeMillis() + FILE_SUFFIX;
            File target = new File(getBackgroundDir(), name);
            try (OutputStream out = new FileOutputStream(target)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }

            clearCache();
            return name;
        } catch (Exception e) {
            Logger.e(e);
            return null;
        } finally {
            if (bitmap != null && !bitmap.isRecycled())
                bitmap.recycle();
            if (temp.exists())
                temp.delete();
        }
    }

    /**
     * Remove the background image of the app.
     */
    public static void clear() {
        File file = getBackgroundFile();
        if (file != null && file.exists() && !file.delete())
            Logger.e("failed to delete " + file.getAbsolutePath());
        clearCache();
    }

    /**
     * Delete the background images that are not the one currently in use.
     */
    public static void deleteUnusedImages() {
        String current = HiSettingsHelper.getInstance().getBackgroundImage();
        File[] files = getBackgroundDir().listFiles();
        if (files == null)
            return;
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(FILE_PREFIX) && !name.equals(current) && file.exists() && !file.delete())
                Logger.e("failed to delete " + file.getAbsolutePath());
        }
    }

    private static void clearCache() {
        sDisplayBitmap = null;
        sDisplayKey = null;
    }

    /**
     * @return the alpha (0-255) of the scrim drawn on top of the image.
     */
    private static int getMaskAlpha() {
        int percent = HiSettingsHelper.getInstance().getBackgroundMask();
        if (percent < 0)
            percent = 0;
        if (percent > 100)
            percent = 100;
        return Math.round(percent * 255 / 100f);
    }

    private static Bitmap getDisplayBitmap() {
        File file = getBackgroundFile();
        if (file == null)
            return null;

        String key = file.getAbsolutePath() + ":" + file.lastModified();
        if (sDisplayBitmap != null && key.equals(sDisplayKey) && !sDisplayBitmap.isRecycled())
            return sDisplayBitmap;

        Bitmap bitmap = decodeScaled(file, MAX_DISPLAY_SIZE);
        if (bitmap == null)
            return null;

        sDisplayBitmap = bitmap;
        sDisplayKey = key;
        return bitmap;
    }

    private static Bitmap decodeScaled(File file, int maxSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0)
            return null;

        options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, maxSize);
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private static int calculateInSampleSize(int width, int height, int maxSize) {
        int sampleSize = 1;
        while (height / (sampleSize * 2) >= maxSize || width / (sampleSize * 2) >= maxSize) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static Bitmap rotate(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap)
            bitmap.recycle();
        return rotated;
    }

    @SuppressWarnings("deprecation")
    private static int readRotation(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }
}
