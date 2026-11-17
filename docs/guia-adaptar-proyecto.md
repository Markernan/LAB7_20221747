# Adaptar el proyecto (otro nombre, otro Firebase, otros puertos)

## A. Cambiar solo el Firebase (lo más común)
No toques código. Con la app `com.pucp.bicipucp` registrada en el **nuevo** proyecto:
1. Reemplaza `app/google-services.json` por el del proyecto nuevo.
2. En ese proyecto: habilita correo/contraseña, crea Database + Storage y pega `database.rules.json` / `storage.rules`.
3. Registra el SHA-1 de tu PC (`docs/scripts/obtener-huellas.bat`).
Todo lo demás (Auth, perfil, foto) usa el proyecto que diga el JSON.

## B. Cambiar el título / nombre visible
- `app/src/main/res/values/strings.xml` → `app_name` y los textos (`carne_titulo`, etc.).
- Colores: `values/colors.xml`; tema: `values/themes.xml`; logo: `drawable/ic_logo_bici.xml`.

## C. Cambiar el paquete (`com.pucp.bicipucp` → otro)
1. Android Studio: clic derecho en el paquete `com.pucp.bicipucp` → **Refactor → Rename**.
2. `app/build.gradle`: `namespace` y `applicationId`.
3. En Firebase, registra una app Android con el **nuevo** paquete y descarga su `google-services.json`
   (el paquete no se puede editar en una app ya registrada).

## D. Cambiar rutas o campos de Firebase
| Qué | Dónde |
|---|---|
| Perfil `usuarios/{uid}` y sus campos | `AuthService.java` (`registrar`, `camposAprobacion`), `MainActivity`, `CarneActivity` |
| Ruta de la foto `credenciales_bicipucp/<UID>.jpg` | `CarneActivity.subirFoto` **y** `storage.rules` (deben coincidir) |
| Tamaño/calidad de la foto | `CarneActivity.subirFoto` → `ImagenUtil.comprimirJpeg(…, 1024, 80)` |
| Duración de la cuenta regresiva | Viene del backend (`desbloqueo_expira_en`, en `BiciController`) |

## E. Backend
| Qué | Dónde |
|---|---|
| Puertos | `application.yml` de cada servicio (Eureka 8761, validador 8001, orquestador 8080) |
| Reglas del código de alumno / PIN | `pucp-validador-service/.../ValidadorController.java` |
| URL del validador para RestTemplate | `orquestador-service/src/main/resources/application.yml` → `validador.url` |
| Nombre del servicio Feign | `@FeignClient(name = "pucp-validador-service")` debe coincidir con `spring.application.name` del validador |
| IP del backend en la app | campo "Servidor" del login, o `-PbackendUrl=http://IP:8080` al compilar |
