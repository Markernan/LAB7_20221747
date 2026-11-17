# pract-eureka — BiciPUCP (Smart Bike Sharing IoT)

Laboratorio: Firebase (Auth y Storage) + ecosistema de microservicios Spring Boot.

| Carpeta | Qué es |
|---|---|
| `app/` | App Android (Java): registro validado por el backend, cuenta regresiva de 120 s, "Mi Carné IoT" con foto en Storage |
| `eureka-server/` | Servidor de descubrimiento (puerto **8761**) |
| `pucp-validador-service/` | Reglas + Actuator (puerto **8001**): `/validar/alumno/{codigo}`, `/validar/candado/{pin}` |
| `orquestador-service/` | `POST /bici/solicitar-desbloqueo` (puerto **8080**): RestTemplate + Feign |
| `ARQUITECTURA.md` | Respuestas teóricas (pregunta 3.1) |

## Ejecutar
1. **Backend** (JDK 17+): doble clic en `iniciar-backend.bat` (o `./gradlew bootRun` en cada carpeta, Eureka primero).
   Eureka: <http://localhost:8761> · Salud: <http://localhost:8001/actuator/health>
2. **Firebase**: sigue [`docs/CONECTAR-FIREBASE.md`](docs/CONECTAR-FIREBASE.md) (crea `app/google-services.json`).
3. Abre la raíz en Android Studio y ejecuta.

Más guías: [`docs/00-empezar-en-cualquier-pc.md`](docs/00-empezar-en-cualquier-pc.md) (PC nueva, SHA-1, IP del celular) y
[`docs/guia-adaptar-proyecto.md`](docs/guia-adaptar-proyecto.md) (cambiar nombre, paquete, Firebase o puertos).

Pruebas rápidas del backend:
```bash
curl -X POST localhost:8080/bici/solicitar-desbloqueo -H "Content-Type: application/json" -d '{"codigo":"20230145","pin":"1234"}'
```
Responde `200` con `APROBADO` + token; con código/PIN inválidos, `400` con `{"mensaje": "..."}`.
