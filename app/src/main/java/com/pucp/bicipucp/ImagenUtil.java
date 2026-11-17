package com.pucp.bicipucp;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class ImagenUtil {

    private ImagenUtil() { }

    /** Reduce a un lado máximo de {@code maxLado} px, corrige la rotación EXIF y comprime a JPEG. */
    static byte[] comprimirJpeg(ContentResolver cr, Uri uri, int maxLado, int calidad) throws IOException {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, limites);
        }
        int muestra = 1;
        while (Math.max(limites.outWidth, limites.outHeight) / (muestra * 2) >= maxLado) {
            muestra *= 2;
        }
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = muestra;
        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, op);
        }
        if (bmp == null) {
            throw new IOException("No se pudo leer la imagen");
        }
        int grados = 0;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in != null) {
                int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                grados = o == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                        : o == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : o == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            }
        } catch (IOException ignorada) {
            // sin EXIF
        }
        float escala = Math.min(1f, (float) maxLado / Math.max(bmp.getWidth(), bmp.getHeight()));
        Matrix m = new Matrix();
        m.postScale(escala, escala);
        m.postRotate(grados);
        Bitmap final_ = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final_.compress(Bitmap.CompressFormat.JPEG, calidad, bos);
        return bos.toByteArray();
    }
}
