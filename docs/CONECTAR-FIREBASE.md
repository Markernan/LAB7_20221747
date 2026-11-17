# Conectar BiciPUCP a Firebase

La app usa **Authentication** (correo/contraseña), **Realtime Database** (perfil en `usuarios/{uid}`) y
**Storage** (`credenciales_bicipucp/<UID>.jpg`). `app/google-services.json` no se sube a Git: créalo así.

1. [Firebase console](https://console.firebase.google.com) con la cuenta PUCP → tu proyecto (o crea uno).
2. **Agregar app Android**: paquete `com.pucp.bicipucp`.
   - Huella SHA-1 de tu PC: `./gradlew signingReport` (variante `debug`) o `docs/scripts` de tu proyecto anterior.
3. **Authentication → Método de acceso**: habilita **Correo electrónico/contraseña**.
4. **Realtime Database → Crear base de datos** (modo bloqueado) → pestaña **Reglas** → pega
   [`database.rules.json`](../database.rules.json) → **Publicar**.
   (Incluye también las reglas de `pronosticos` por si el proyecto se comparte con la práctica anterior.)
5. **Storage → Comenzar**. Firebase puede pedir activar el plan **Blaze** (hay cuota gratuita) para crear el bucket.
   Luego pestaña **Reglas** → pega [`storage.rules`](../storage.rules) → **Publicar**.
6. Descarga **google-services.json** (⚙ → Configuración del proyecto → Tus apps) **después** de los pasos anteriores
   y cópialo en `app/`.
7. Abre la carpeta raíz en Android Studio y ejecuta.

> ¿Otra PC u otra cuenta? Mira [`00-empezar-en-cualquier-pc.md`](00-empezar-en-cualquier-pc.md) y
> [`guia-adaptar-proyecto.md`](guia-adaptar-proyecto.md).

## IP del backend
- Emulador Android: `http://10.0.2.2:8080` (por defecto).
- Celular real (misma red Wi-Fi que la PC): escribe `http://<IP de tu PC>:8080` en el campo **Servidor** de la pantalla
  de inicio de sesión (o compila con `-PbackendUrl=http://192.168.1.50:8080`). Permite el puerto 8080 en el firewall.

## Probar sin credenciales (emuladores de Firebase)
```bash
npm install -g firebase-tools        # requiere JDK 21
firebase emulators:start --only auth,database,storage --project demo-bicipucp
./gradlew assembleDebug -PuseEmulator=true
```
(El `google-services.json` solo necesita existir; el de prueba con proyecto `demo-bicipucp` sirve.)
