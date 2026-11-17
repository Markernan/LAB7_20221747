package com.pucp.bicipucp;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

public class BiciApp extends Application {

    private static final String HOST_EMULADOR = "10.0.2.2";

    private static FirebaseDatabase baseDeDatos;
    private static FirebaseStorage almacenamiento;

    @Override
    public void onCreate() {
        super.onCreate();
        baseDeDatos = FirebaseDatabase.getInstance();
        almacenamiento = FirebaseStorage.getInstance();
        // Solo con -PuseEmulator=true (pruebas sin credenciales). En el flujo normal esto no se ejecuta.
        if (BuildConfig.USE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator(HOST_EMULADOR, 9099);
            baseDeDatos.useEmulator(HOST_EMULADOR, 9000);
            almacenamiento.useEmulator(HOST_EMULADOR, 9199);
        }
    }

    /** Única instancia de Realtime Database (ver nota de useEmulator en la guía). */
    public static FirebaseDatabase baseDeDatos() {
        return baseDeDatos;
    }

    public static FirebaseStorage almacenamiento() {
        return almacenamiento;
    }
}
