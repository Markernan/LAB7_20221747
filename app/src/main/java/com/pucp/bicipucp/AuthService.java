package com.pucp.bicipucp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capa de seguridad: valida con el orquestador (HTTP en un hilo de fondo) ANTES de tocar Firebase.
 * Todos los callbacks se entregan en el hilo principal.
 */
public class AuthService {

    public interface Callback<T> {
        void ok(T resultado);

        void error(String mensaje);
    }

    /** Respuesta 200 del orquestador. */
    public static class Aprobacion {
        public String token;
        public int expiraEn;
        public String timestamp;
        /** Hora del dispositivo al recibir la respuesta (respaldo si hay desfase de zona horaria). */
        public long recibidoMs;
    }

    private static final ExecutorService HILO_FONDO = Executors.newFixedThreadPool(2);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private final Context contexto;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public AuthService(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    // ---------------------------------------------------------------- Backend

    /** POST /bici/solicitar-desbloqueo con {"codigo","pin"}. 200 -> ok; 400 u otro fallo -> error(mensaje). */
    public void solicitarDesbloqueo(String codigo, String pin, Callback<Aprobacion> cb) {
        final String base = Config.servidor(contexto);
        HILO_FONDO.execute(() -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(base + "/bici/solicitar-desbloqueo").openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(8000);
                con.setReadTimeout(10000);
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setRequestProperty("Accept", "application/json");
                JSONObject cuerpo = new JSONObject().put("codigo", codigo).put("pin", pin);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(cuerpo.toString().getBytes(StandardCharsets.UTF_8));
                }
                int http = con.getResponseCode();
                String texto = leer(http >= 400 ? con.getErrorStream() : con.getInputStream());
                if (http == 200) {
                    JSONObject j = new JSONObject(texto);
                    Aprobacion a = new Aprobacion();
                    a.token = j.optString("iot_auth_token");
                    a.expiraEn = j.optInt("desbloqueo_expira_en", 120);
                    a.timestamp = j.optString("timestamp_aprobacion");
                    a.recibidoMs = System.currentTimeMillis();
                    entregarOk(cb, a);
                } else {
                    String msg = null;
                    try {
                        msg = new JSONObject(texto).optString("mensaje", null);
                    } catch (Exception ignorada) {
                        // cuerpo sin JSON
                    }
                    entregarError(cb, msg != null && !msg.isEmpty() ? msg : "Error del servidor (HTTP " + http + ")");
                }
            } catch (Exception e) {
                entregarError(cb, "No se pudo conectar con el servidor (" + base + "). Verifica que esté encendido.");
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        });
    }

    private static String leer(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream is = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        }
    }

    // ---------------------------------------------------------------- Registro / Login

    /** 1) valida en el backend; 2) solo si es 200, crea el usuario en Firebase y guarda su perfil. */
    public void registrar(String correo, String password, String codigo, String pin, Callback<Void> cb) {
        solicitarDesbloqueo(codigo, pin, new Callback<Aprobacion>() {
            @Override
            public void ok(Aprobacion a) {
                auth.createUserWithEmailAndPassword(correo, password)
                        .addOnSuccessListener(r -> {
                            FirebaseUser u = r.getUser();
                            if (u == null) {
                                cb.error("No se pudo crear el usuario.");
                                return;
                            }
                            Map<String, Object> perfil = new HashMap<>();
                            perfil.put("correo", correo);
                            perfil.put("codigo", codigo);
                            perfil.putAll(camposAprobacion(a));
                            BiciApp.baseDeDatos().getReference("usuarios").child(u.getUid()).setValue(perfil)
                                    .addOnSuccessListener(x -> cb.ok(null))
                                    .addOnFailureListener(e -> {
                                        u.delete(); // no dejar un usuario sin perfil
                                        cb.error("No se pudo guardar el perfil: " + e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> cb.error(traducir(e)));
            }

            @Override
            public void error(String mensaje) {
                cb.error(mensaje);
            }
        });
    }

    public void login(String correo, String password, Callback<Void> cb) {
        auth.signInWithEmailAndPassword(correo, password)
                .addOnSuccessListener(r -> cb.ok(null))
                .addOnFailureListener(e -> cb.error(traducir(e)));
    }

    /** Re-energización: pide de nuevo el desbloqueo y actualiza el documento con el nuevo timestamp. */
    public void renovarDesbloqueo(String codigo, String pin, Callback<Aprobacion> cb) {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            cb.error("Sesión no iniciada.");
            return;
        }
        solicitarDesbloqueo(codigo, pin, new Callback<Aprobacion>() {
            @Override
            public void ok(Aprobacion a) {
                BiciApp.baseDeDatos().getReference("usuarios").child(u.getUid()).updateChildren(camposAprobacion(a))
                        .addOnSuccessListener(x -> cb.ok(a))
                        .addOnFailureListener(e -> cb.error("No se pudo actualizar en Firebase: " + e.getMessage()));
            }

            @Override
            public void error(String mensaje) {
                cb.error(mensaje);
            }
        });
    }

    private static Map<String, Object> camposAprobacion(Aprobacion a) {
        Map<String, Object> m = new HashMap<>();
        m.put("iot_auth_token", a.token);
        m.put("desbloqueo_expira_en", a.expiraEn);
        m.put("timestamp_aprobacion", a.timestamp);
        m.put("aprobado_en_ms", a.recibidoMs);
        return m;
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    private static String traducir(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Ya existe una cuenta con ese correo.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es muy débil (mínimo 6 caracteres).";
        }
        if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Correo o contraseña incorrectos.";
        }
        if (e instanceof FirebaseNetworkException) {
            return "Sin conexión a Firebase. Revisa tu internet.";
        }
        return e.getMessage() == null ? "Error inesperado." : e.getMessage();
    }

    // ---------------------------------------------------------------- Hilo principal

    private static <T> void entregarOk(Callback<T> cb, T valor) {
        PRINCIPAL.post(() -> cb.ok(valor));
    }

    private static <T> void entregarError(Callback<T> cb, String mensaje) {
        PRINCIPAL.post(() -> cb.error(mensaje));
    }
}
