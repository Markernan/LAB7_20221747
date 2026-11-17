package pe.pucp.orquestador;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente declarativo: Feign resuelve "pucp-validador-service" a través de Eureka. */
@FeignClient(name = "pucp-validador-service")
public interface ValidadorClient {

    @GetMapping("/validar/candado/{pin}")
    Boolean validarCandado(@PathVariable("pin") String pin);
}
