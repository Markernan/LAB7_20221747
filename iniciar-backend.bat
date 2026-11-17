@echo off
rem Levanta los 3 microservicios (cada uno en su ventana). Requiere JDK 17 o superior.
rem Orden: primero Eureka; luego validador y orquestador (se registran solos).
start "eureka-server (8761)" cmd /k "cd /d %~dp0eureka-server && gradlew.bat bootRun"
timeout /t 25 /nobreak >nul
start "pucp-validador-service (8001)" cmd /k "cd /d %~dp0pucp-validador-service && gradlew.bat bootRun"
start "orquestador-service (8080)" cmd /k "cd /d %~dp0orquestador-service && gradlew.bat bootRun"
echo.
echo Consola de Eureka: http://localhost:8761
echo Espera ~1 minuto a que los servicios aparezcan como UP.
