package com.pucp.bicipucp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.pucp.bicipucp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private AuthService auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            irAPrincipal();
            return;
        }
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = new AuthService(this);
        b.etServidor.setText(Config.servidor(this));

        b.btnIngresar.setOnClickListener(v -> ingresar());
        b.btnIrRegistro.setOnClickListener(v -> {
            Config.guardarServidor(this, texto(b.etServidor.getText()));
            startActivity(new Intent(this, RegistroActivity.class));
        });
    }

    private void ingresar() {
        String correo = texto(b.etCorreo.getText());
        String pass = texto(b.etPassword.getText());
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            b.etCorreo.setError("Correo inválido");
            return;
        }
        if (pass.isEmpty()) {
            b.etPassword.setError("Ingresa tu contraseña");
            return;
        }
        Config.guardarServidor(this, texto(b.etServidor.getText()));
        cargando(true);
        auth.login(correo, pass, new AuthService.Callback<Void>() {
            @Override
            public void ok(Void r) {
                cargando(false);
                irAPrincipal();
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
        b.btnIngresar.setEnabled(!si);
        b.btnIrRegistro.setEnabled(!si);
    }

    private void irAPrincipal() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private static String texto(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
