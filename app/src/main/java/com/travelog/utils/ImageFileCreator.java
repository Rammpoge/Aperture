package com.travelog.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageFileCreator {
    private static final String TAG = "ImageFileCreator";

    public static File createTempFileFromUri(Uri uri, Context context) {
        return compressAndCreateTempFile(uri, context, 80);
    }

    public static File compressAndCreateTempFile(Uri uri, Context context, int quality) {
        Log.d(TAG, "Compressing and creating temp file from URI: " + uri);
        
        String extension = getExtensionFromUri(uri, context);
        Bitmap.CompressFormat format = getCompressFormat(extension);

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
            
            options.inSampleSize = calculateInSampleSize(options, 2000, 2000);
            options.inJustDecodeBounds = false;
            
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
            if (bitmap == null) return null;

            File tempFile = File.createTempFile("upload", "." + extension, context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                bitmap.compress(format, quality, out);
            }
            bitmap.recycle();
            
            Log.d(TAG, "Compressed temp file created (" + extension + "): " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "compressAndCreateTempFile: failed: " + e.getMessage());
            return null;
        }
    }

    private static String getExtensionFromUri(Uri uri, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
        if (extension == null) {
            extension = "jpg"; // Default
        }
        return extension;
    }

    private static Bitmap.CompressFormat getCompressFormat(String extension) {
        if (extension.equalsIgnoreCase("png")) {
            return Bitmap.CompressFormat.PNG;
        } else if (extension.equalsIgnoreCase("webp")) {
            return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R 
                    ? Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP;
        }
        return Bitmap.CompressFormat.JPEG;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static File createTempFileFromBitmap(Bitmap bitmap, Context context) {
        try {
            File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            }
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }
}
