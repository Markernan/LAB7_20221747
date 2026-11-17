package com.pucp.bicipucp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pucp.bicipucp.databinding.ActivityRegistroBinding;

public class RegistroActivity extends AppCompatActivity {

    private ActivityRegistroBinding b;
    private AuthService auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegistroBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        auth = new AuthService(this);
        b.btnRegistrar.setOnClickListener(v -> registrar());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void registrar() {
        String correo = texto(b.etCorreo.getText());
        String pass = b.etPassword.getText() == null ? "" : b.etPassword.getText().toString();
        String codigo = texto(b.etCodigo.getText());
        String pin = texto(b.etPin.getText());

        b.tilCorreo.setError(null);
        b.tilPassword.setError(null);
        b.tilCodigo.setError(null);
        b.tilPin.setError(null);
        boolean valido = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            b.tilCorreo.setError("Ingresa un correo válido");
            valido = false;
        }
        if (pass.length() < 6) {
            b.tilPassword.setError("Mínimo 6 caracteres");
            valido = false;
        }
        if (!codigo.matches("\\d{8}")) {
            b.tilCodigo.setError("El código PUCP tiene 8 dígitos");
            valido = false;
        }
        if (!pin.matches("\\d{4}")) {
            b.tilPin.setError("El PIN tiene 4 dígitos");
            valido = false;
        }
        if (!valido) {
            return;
        }

        cargando(true); // botón deshabilitado + ProgressBar; la llamada HTTP corre en un hilo de fondo
        auth.registrar(correo, pass, codigo, pin, new AuthService.Callback<Void>() {
            @Override
            public void ok(Void r) {
                cargando(false);
                Intent i = new Intent(RegistroActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }

            @Override
            public void error(String mensaje) {
                cargando(false);
                Snackbar.make(b.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void cargando(boolean si) {
        b.progreso.setVisibility(si ? View.VISIBLE : View.GONE);
        b.btnRegistrar.setEnabled(!si);
    }

    private static String texto(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
