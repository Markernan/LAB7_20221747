package pe.pucp.orquestador;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class BiciController {

    private final ValidadorClient validadorClient;
    private final String validadorUrl;
    /** Instanciado de forma manual, como pide el enunciado. */
    private final RestTemplate restTemplate = new RestTemplate();

    public BiciController(ValidadorClient validadorClient, @Value("${validador.url}") String validadorUrl) {
        this.validadorClient = validadorClient;
        this.validadorUrl = validadorUrl;
    }

    @PostMapping("/bici/solicitar-desbloqueo")
    public ResponseEntity<Map<String, Object>> solicitarDesbloqueo(@RequestBody DesbloqueoRequest req) {
        String codigo = req.codigo() == null ? "" : req.codigo().trim();
        String pin = req.pin() == null ? "" : req.pin().trim();

        boolean alumnoOk;
        boolean candadoOk;
        try {
            // 1) Validación del alumno: RestTemplate
            Boolean a = restTemplate.getForObject(validadorUrl + "/validar/alumno/{codigo}", Boolean.class, codigo);
            alumnoOk = Boolean.TRUE.equals(a);
            // 2) Validación del candado: Feign
            candadoOk = Boolean.TRUE.equals(validadorClient.validarCandado(pin));
        } catch (Exception e) {
            return respuesta(HttpStatus.SERVICE_UNAVAILABLE,
                    Map.of("mensaje", "No se pudo contactar al servicio validador. Intente nuevamente."));
        }

        if (alumnoOk && candadoOk) {
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "APROBADO");
            ok.put("iot_auth_token", "PUCP-BIKE-" + UUID.randomUUID());
            ok.put("desbloqueo_expira_en", 120);
            ok.put("timestamp_aprobacion", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
            return respuesta(HttpStatus.OK, ok);
        }

        String motivo;
        if (!alumnoOk && !candadoOk) {
            motivo = "El código de alumno no existe en la base de datos y el PIN del candado es inválido (dígitos repetidos consecutivos).";
        } else if (!alumnoOk) {
            motivo = "El código de alumno no existe en la base de datos";
        } else {
            motivo = "El PIN del candado es inválido: no debe tener dígitos repetidos consecutivos";
        }
        return respuesta(HttpStatus.BAD_REQUEST, Map.of("mensaje", motivo));
    }

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus s, Map<String, ?> cuerpo) {
        return ResponseEntity.status(s).body(new LinkedHashMap<String, Object>(cuerpo));
    }
}
