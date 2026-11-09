package com.s23plus.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Guardado transaccional de fotografías JPEG en el almacenamiento compartido de Android (MediaStore).
 * Todas las imágenes se almacenan en {@link CameraConfig#MEDIA_RELATIVE_PATH} ("Pictures/S23Camera")
 * utilizando el flag atómico IS_PENDING para evitar archivos corruptos o incompletos en la galería.
 */
public final class MediaStoreSaver {

    private static final int JPEG_QUALITY = 95;

    private MediaStoreSaver() {
    }

    public static final class SaveResult {
        public final Uri uri;
        public final String fileName;
        public final int byteCount;

        public SaveResult(Uri uri, String fileName, int byteCount) {
            this.uri = uri;
            this.fileName = fileName;
            this.byteCount = byteCount;
        }
    }

    /**
     * Guarda un arreglo de bytes JPEG (proveniente del ImageReader de Camera2) en MediaStore.
     */
    public static SaveResult saveJpegBytes(Context context, byte[] jpegBytes, String prefix) throws IOException {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IOException("Datos JPEG vacios.");
        }
        String fileName = buildFileName(prefix);
        ContentResolver resolver = context.getContentResolver();
        Uri itemUri = insertPendingImage(resolver, fileName);

        try (OutputStream out = resolver.openOutputStream(itemUri)) {
            if (out == null) {
                throw new IOException("No se pudo abrir OutputStream para MediaStore.");
            }
            out.write(jpegBytes);
            out.flush();
        } catch (IOException | RuntimeException e) {
            resolver.delete(itemUri, null, null);
            throw new IOException("Error al escribir bytes JPEG en MediaStore: " + e.getMessage(), e);
        }

        publishPendingImage(resolver, itemUri);
        return new SaveResult(itemUri, fileName, jpegBytes.length);
    }

    /**
     * Comprime y guarda un Bitmap en formato JPEG dentro de MediaStore.
     */
    public static SaveResult saveBitmap(Context context, Bitmap bitmap, String prefix) throws IOException {
        if (bitmap == null || bitmap.isRecycled()) {
            throw new IOException("Bitmap invalido o reciclado.");
        }
        String fileName = buildFileName(prefix);
        ContentResolver resolver = context.getContentResolver();
        Uri itemUri = insertPendingImage(resolver, fileName);

        try (OutputStream out = resolver.openOutputStream(itemUri)) {
            if (out == null) {
                throw new IOException("No se pudo abrir OutputStream para MediaStore.");
            }
            boolean compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            if (!compressed) {
                throw new IOException("Fallo en Bitmap.compress().");
            }
            out.flush();
        } catch (IOException | RuntimeException e) {
            resolver.delete(itemUri, null, null);
            throw new IOException("Error al guardar Bitmap en MediaStore: " + e.getMessage(), e);
        }

        publishPendingImage(resolver, itemUri);
        return new SaveResult(itemUri, fileName, bitmap.getAllocationByteCount());
    }

    private static String buildFileName(String prefix) {
        String safePrefix = (prefix == null || prefix.isEmpty()) ? "S23" : prefix;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        return safePrefix + "_" + timestamp + ".jpg";
    }

    private static Uri insertPendingImage(ContentResolver resolver, String fileName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, CameraConfig.MEDIA_RELATIVE_PATH);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("MediaStore.insert retorno null para " + fileName);
        }
        return uri;
    }

    private static void publishPendingImage(ContentResolver resolver, Uri itemUri) {
        ContentValues update = new ContentValues();
        update.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(itemUri, update, null, null);
    }
}
