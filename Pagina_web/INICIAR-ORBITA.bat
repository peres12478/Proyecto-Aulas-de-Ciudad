@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Instala Java 17 o superior y agregalo al PATH.
  pause
  exit /b 1
)
if not exist portal-orbita.jar (
  echo ERROR: No se encuentra portal-orbita.jar.
  pause
  exit /b 1
)
start "Orbita UCO - servidor" /D "%~dp0" java -jar portal-orbita.jar
timeout /t 2 /nobreak >nul
start "" http://localhost:8080
