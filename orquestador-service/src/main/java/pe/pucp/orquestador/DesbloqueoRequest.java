package pe.pucp.orquestador;

/** Cuerpo JSON que envía Android: {"codigo": "...", "pin": "..."} */
public record DesbloqueoRequest(String codigo, String pin) { }
