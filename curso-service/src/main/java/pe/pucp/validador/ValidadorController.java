package pe.pucp.validador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Reglas de negocio sin estado: cada respuesta depende solo del parámetro recibido. */
@RestController
public class ValidadorController {

    /** 8 dígitos exactos y prefijo "20". 20230145 -> true | 19984512 -> false */
    @GetMapping("/validar/alumno/{codigo}")
    public boolean validarAlumno(@PathVariable String codigo) {
        return codigo != null && codigo.matches("20\\d{6}");
    }

    /** 4 dígitos sin repetir el mismo número de forma consecutiva. 1234, 1010 -> true | 1123, 5555 -> false */
    @GetMapping("/validar/candado/{pin}")
    public boolean validarCandado(@PathVariable String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            return false;
        }
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) == pin.charAt(i - 1)) {
                return false;
            }
        }
        return true;
    }
}
