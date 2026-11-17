package com.pucp.bicipucp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pucp.bicipucp.databinding.ActivityMainBinding;

/** Centro de control: máquina de estados Activo (0-120 s) / Expirado, basada en timestamp_aprobacion. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;
    private AuthService auth;
    private DatabaseReference refPerfil;
    private ValueEventListener escucha;

    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable tic = this::actualizarCuenta;

    private String correo;
    private String codigo;
    private String timestamp;
    private long recibidoMs;
    private int expiraEn = 120;
    private boolean datosListos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            volverALogin();
            return;
        }
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = new AuthService(this);
        refPerfil = BiciApp.baseDeDatos().getReference("usuarios").child(u.getUid());

        b.btnNuevoDesbloqueo.setOnClickListener(v -> pedirPin());
        b.btnCerrarSesion.setOnClickListener(v -> {
            auth.cerrarSesion();
            volverALogin();
        });
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
                correo = s.child("correo").getValue(String.class);
                codigo = s.child("codigo").getValue(String.class);
                timestamp = s.child("timestamp_aprobacion").getValue(String.class);
                Long ms = s.child("aprobado_en_ms").getValue(Long.class);
                recibidoMs = ms == null ? 0 : ms;
                Integer e = s.child("desbloqueo_expira_en").getValue(Integer.class);
                expiraEn = e == null ? 120 : e;
                datosListos = timestamp != null;
                b.tvUsuario.setText(correo == null ? "" : correo + (codigo == null ? "" : "  ·  " + codigo));
                actualizarCuenta();
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
        reloj.removeCallbacks(tic);
        if (refPerfil != null && escucha != null) {
            refPerfil.removeEventListener(escucha);
        }
    }

    /** Se ejecuta cada 500 ms mientras la pantalla está visible; recalcula desde el reloj, así no acumula errores. */
    private void actualizarCuenta() {
        reloj.removeCallbacks(tic);
        if (!datosListos) {
            b.tvCuenta.setText("--");
            b.tvEstado.setText("Cargando…");
            return;
        }
        long restantes = Tiempo.restantes(timestamp, recibidoMs, expiraEn);
        b.tvCuenta.setText(String.valueOf(restantes));
        if (restantes > 0) {
            b.tvCuenta.setTextColor(getColor(R.color.activo));
            b.tvEstado.setText(R.string.cuenta_regresiva_activa);
            b.btnNuevoDesbloqueo.setVisibility(View.GONE);
            reloj.postDelayed(tic, 500);
        } else {
            b.tvCuenta.setTextColor(getColor(R.color.expirado));
            b.tvEstado.setText(R.string.cuenta_regresiva_expirada);
            b.btnNuevoDesbloqueo.setVisibility(View.VISIBLE);
        }
    }

    private void pedirPin() {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        et.setHint(R.string.hint_pin);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pedir_pin_titulo)
                .setView(et)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String pin = et.getText().toString().trim();
                    if (!pin.matches("\\d{4}")) {
                        Snackbar.make(b.getRoot(), "El PIN debe tener 4 dígitos", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    reenergizar(pin);
                })
                .show();
    }

    private void reenergizar(String pin) {
        if (codigo == null) {
            return;
        }
        b.btnNuevoDesbloqueo.setEnabled(false);
        b.progreso.setVisibility(View.VISIBLE);
        auth.renovarDesbloqueo(codigo, pin, new AuthService.Callback<AuthService.Aprobacion>() {
            @Override
            public void ok(AuthService.Aprobacion a) {
                b.progreso.setVisibility(View.GONE);
                b.btnNuevoDesbloqueo.setEnabled(true);
                // El listener de Firebase recibe el nuevo timestamp y reinicia la cuenta desde 120.
            }

            @Override
            public void error(String mensaje) {
                b.progreso.setVisibility(View.GONE);
                b.btnNuevoDesbloqueo.setEnabled(true);
                Snackbar.make(b.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.accion_carne) {
            startActivity(new Intent(this, CarneActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void volverALogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
