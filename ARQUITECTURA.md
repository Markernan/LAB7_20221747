# ARQUITECTURA — BiciPUCP

## 1. ¿Qué patrón arquitectónico global se aplica y por qué?

**Patrón de Microservicios** (Clase 12): un conjunto de componentes pequeños, autónomos y de función única que
colaboran para cumplir una tarea mayor, en lugar de un backend monolítico.

| Componente | Puerto | Rol según la Clase 12 |
|---|---|---|
| `eureka-server` | 8761 | **Registro / Discovery Server**: registro, descubrimiento y monitoreo de servicios. |
| `pucp-validador-service` | 8001 | Microservicio de función única (reglas de alumno y candado) + Actuator. |
| `orquestador-service` | 8080 | Único punto de acceso para Android; consume al validador (RestTemplate y Feign). |

Por qué este patrón:

- **Función única e independencia:** las reglas (código `20xxxxxx`, PIN sin dígitos repetidos) cambian sin tocar al
  orquestador ni a la app.
- **Registro y auto-descubrimiento:** cada servicio se registra en Eureka; el orquestador **no conoce la IP** del
  validador, Feign lo localiza por nombre (`pucp-validador-service`) y Spring Cloud LoadBalancer (sucesor de Ribbon)
  reparte la carga si hay varias instancias.
- **Seguridad centralizada:** la app solo habla con `http://<IP>:8080/bici/solicitar-desbloqueo` y **no registra** al
  usuario en Firebase hasta recibir `200 OK`.
- **Tolerancia a fallos y escalado:** si el validador cae, el orquestador responde un error controlado (`503` con
  `mensaje`) y no se cuelga; cada servicio puede replicarse por separado.

## 2. ¿Cómo cumple el validador la restricción "Stateless" de REST?

Stateless = el servidor **no guarda información de sesión; cada solicitud es independiente**.
`pucp-validador-service`:

- No usa sesión HTTP, cookies ni atributos mutables: el controlador solo tiene métodos puros.
- Cada petición es **autocontenida**: todo viaja en la URL (`/validar/alumno/{codigo}`, `/validar/candado/{pin}`) y
  la respuesta depende únicamente de ese parámetro; mismo dato, mismo resultado, en cualquier instancia.
- Por eso puede **escalar horizontalmente** detrás de Eureka sin compartir memoria entre instancias.
- Usa `GET` de forma semántica (consulta de un recurso, idempotente y sin efectos secundarios).

El estado de la aplicación (perfil, token, `timestamp_aprobacion`, foto) vive en **Firebase**, no en los servicios.
