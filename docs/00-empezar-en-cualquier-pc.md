# Empezar en cualquier PC (backend + app)

> El **SHA-1** sale de la llave de depuración de **cada PC**. En una PC nueva hay que sacarlo y registrarlo en
> Firebase otra vez (solo afecta a Google Sign-In; correo/contraseña, Database y Storage funcionan sin eso).

## 1. Instalar
| Programa | Para qué |
|---|---|
| **Git** | clonar el repo (<https://git-scm.com/download/win>) |
| **Android Studio** | app, SDK API 35 y emulador (trae su propio JDK 21) |
| **JDK 17 o 21** | solo para correr el backend fuera de Android Studio (Temurin: <https://adoptium.net>) |

Emulador recomendado: Pixel 8 Pro, API 34 o superior.

## 2. Clonar
```bash
git clone https://github.com/Markernan/pract-eureka.git
cd pract-eureka
```

## 3. Firebase (credenciales fuera de Git)
1. Saca el SHA-1 de **esta** PC: doble clic en `docs/scripts/obtener-huellas.bat`.
2. Sigue [`CONECTAR-FIREBASE.md`](CONECTAR-FIREBASE.md): registrar la app `com.pucp.bicipucp` con ese SHA-1,
   habilitar correo/contraseña, crear Database y Storage, pegar `database.rules.json` y `storage.rules`.
3. Descarga `google-services.json` y cópialo a `app/` (plantilla de referencia: `docs/plantillas/`).

## 4. Backend
Doble clic en `iniciar-backend.bat` (abre 3 ventanas; Eureka arranca primero).
- Eureka: <http://localhost:8761> — deben aparecer `PUCP-VALIDADOR-SERVICE` y `ORQUESTADOR-SERVICE` en **UP**
  (tarda ~1 minuto; es normal que al inicio no aparezcan).
- Si algún puerto está ocupado (8761, 8001, 8080), ciérralo o cambia `server.port` en su `application.yml`.

## 5. App
Abre la **raíz** del repo en Android Studio → Run.
- Emulador: usa `http://10.0.2.2:8080` (ya es el valor por defecto).
- Celular real: PC y celular en el mismo Wi-Fi; escribe `http://<IP de la PC>:8080` en el campo **Servidor**
  del login (IP con `ipconfig`) y permite el puerto 8080 en el Firewall de Windows.

## 6. Errores frecuentes
| Síntoma | Causa / solución |
|---|---|
| "No se pudo conectar con el servidor" | Backend apagado, IP incorrecta o firewall. Prueba `http://<IP>:8080` en el navegador del celular. |
| Eureka sin servicios | Espera ~1 min tras arrancar; confirma que Eureka estaba encendido primero. |
| `google-services.json is missing` | Falta copiarlo a `app/`. |
| Foto no sube: "permission denied" | No publicaste `storage.rules` o no habilitaste Storage. |
| Perfil no guarda / `Permission denied` | No publicaste `database.rules.json`. |
| `Unsupported class file major version` | Usa JDK 17 o 21 (`java -version`). |
