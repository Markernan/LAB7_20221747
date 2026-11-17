package com.pucp.bicipucp;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Cálculo de los segundos que le quedan al comando de desbloqueo. */
final class Tiempo {

    private Tiempo() { }

    /**
     * Segundos restantes = expiraEn - (ahora - timestamp_aprobacion). Si el timestamp del servidor y la hora del
     * dispositivo no coinciden (zonas horarias distintas), usa la hora local guardada al recibir la respuesta.
     */
    static long restantes(String timestampIso, long recibidoMs, int expiraEn) {
        long ahora = System.currentTimeMillis();
        Long desdeServidor = null;
        try {
            desdeServidor = LocalDateTime.parse(timestampIso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignorada) {
            // timestamp ausente o con formato inesperado
        }
        long inicio;
        if (desdeServidor == null) {
            inicio = recibidoMs;
        } else if (recibidoMs > 0 && Math.abs(desdeServidor - recibidoMs) > 60_000) {
            inicio = recibidoMs;
        } else {
            inicio = desdeServidor;
        }
        long transcurridos = (ahora - inicio) / 1000;
        return Math.max(0, expiraEn - transcurridos);
    }
}
