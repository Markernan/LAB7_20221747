package com.pucp.bicipucp;

import android.content.Context;
import android.content.SharedPreferences;

/** URL del microservicio orquestador (editable en la pantalla de inicio de sesión). */
final class Config {

    private static final String PREFS = "bicipucp";
    private static final String CLAVE_SERVIDOR = "servidor";

    private Config() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String servidor(Context c) {
        return prefs(c).getString(CLAVE_SERVIDOR, BuildConfig.BACKEND_URL);
    }

    static void guardarServidor(Context c, String url) {
        String u = url == null ? "" : url.trim();
        if (u.isEmpty()) {
            u = BuildConfig.BACKEND_URL;
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        prefs(c).edit().putString(CLAVE_SERVIDOR, u).apply();
    }
}
