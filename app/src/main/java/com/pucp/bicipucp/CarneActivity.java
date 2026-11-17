package com.pucp.bicipucp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.pucp.bicipucp.databinding.ActivityCarneBinding;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** "Mi Carné IoT": credencial digital con foto en Firebase Storage (/credenciales_bicipucp/UID.jpg). */
public class CarneActivity extends AppCompatActivity {

    private static final ExecutorService HILO_FONDO = Executors.newFixedThreadPool(2);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private ActivityCarneBinding b;
    private FirebaseUser usuario;
    private DatabaseReference refPerfil;
    private ValueEventListener escucha;
    private String fotoMostrada;

    private final androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest> galeria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    subirFoto(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            finish();
            return;
        }
        b = ActivityCarneBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setTitle(R.string.mi_carne);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        refPerfil = BiciApp.baseDeDatos().getReference("usuarios").child(usuario.getUid());
        b.btnSubirFoto.setOnClickListener(v -> galeria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (refPerfil == null) {
            return;
        }
        escucha = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                b.tvCorreo.setText(valor(s.child("correo").getValue(String.class)));
                b.tvCodigo.setText(valor(s.child("codigo").getValue(String.class)));
                String token = s.child("iot_auth_token").getValue(String.class);
                b.tvToken.setText(valor(token));
                b.tvAprobado.setText(valor(s.child("timestamp_aprobacion").getValue(String.class)).replace('T', ' '));
                String url = s.child("foto_url").getValue(String.class);
                if (url != null && !url.isEmpty() && !url.equals(fotoMostrada)) {
                    cargarFoto(url);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Snackbar.make(b.getRoot(), "No se pudo leer Firebase: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        };
        refPerfil.addValueEventListener(escucha);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (refPerfil != null && escucha != null) {
            refPerfil.removeEventListener(escucha);
        }
    }

    private static String valor(String s) {
        return s == null ? "—" : s;
    }

    /** Comprime en un hilo de fondo y sube estrictamente a credenciales_bicipucp/<UID>.jpg. */
    private void subirFoto(Uri uri) {
        cargando(true);
        HILO_FONDO.execute(() -> {
            final byte[] datos;
            try {
                datos = ImagenUtil.comprimirJpeg(getContentResolver(), uri, 1024, 80);
            } catch (Exception e) {
                PRINCIPAL.post(() -> falla("No se pudo procesar la imagen: " + e.getMessage()));
                return;
            }
            PRINCIPAL.post(() -> {
                StorageReference ref = BiciApp.almacenamiento().getReference()
                        .child("credenciales_bicipucp/" + usuario.getUid() + ".jpg");
                ref.putBytes(datos, new StorageMetadata.Builder().setContentType("image/jpeg").build())
                        .addOnSuccessListener(t -> ref.getDownloadUrl()
                                .addOnSuccessListener(url -> {
                                    Toast.makeText(this, url.toString(), Toast.LENGTH_LONG).show();
                                    refPerfil.child("foto_url").setValue(url.toString())
                                            .addOnSuccessListener(x -> cargando(false))
                                            .addOnFailureListener(e -> falla("No se guardó foto_url: " + e.getMessage()));
                                })
                                .addOnFailureListener(e -> falla("No se obtuvo la URL: " + e.getMessage())))
                        .addOnFailureListener(e -> falla("No se pudo subir la foto: " + e.getMessage()));
            });
        });
    }

    /** Descarga la imagen desde la URL guardada en la base de datos y la pinta en el ImageView. */
    private void cargarFoto(String url) {
        fotoMostrada = url;
        HILO_FONDO.execute(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(15000);
                try (InputStream in = con.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp != null) {
                        PRINCIPAL.post(() -> {
                            if (!isDestroyed()) {
                                b.ivFoto.setImageBitmap(bmp);
                            }
                        });
                    }
                } finally {
                    con.disconnect();
                }
            } catch (Exception e) {
                fotoMostrada = null; // permitir reintentar
            }
        });
    }

    private void falla(String mensaje) {
        cargando(false);
        Snackbar.make(b.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void cargando(boolean si) {
        b.progreso.setVisibility(si ? View.VISIBLE : View.GONE);
        b.btnSubirFoto.setEnabled(!si);
    }
}
