
## Tareas Miguel Ángel Pérez García- José Manuel Valencia Gallego
# ORBITA UCO
 ## LLave para el registro de profesores : ORBITA-DOCENTE-DEMO-2026
Portal académico de demostración: dominio orientado a objetos en **Java** + web de una sola página, conectados por una **API HTTP local**. Notas, insignias, chat y calendario de clases. Solo requiere el JDK (sin Maven, Gradle ni npm).

## Funciones

- **Notas e insignias:** el profesor califica (0.0–5.0) solo en su materia; el estudiante ve notas, promedio y 4 insignias recalculadas con sus notas actuales.
- **Cuentas:** registro con documento (TI/CC), acceso por correo o documento, contraseñas con PBKDF2 + sal.
- **Directorio docente:** búsqueda sin importar tildes ni mayúsculas.
- **Chat:** mensajes privados con sondeo cada 2,5 s y contador de no leídos persistente.
- **Calendario semanal:** el profesor publica clases; el servidor evita choques de docente y de salón.
- **Bitácora** de accesos y calificaciones (solo profesores).
## Comando para ejecutar , ojo debe estar en la carpeta donde este el ejecutable
.\INICIAR-ORBITA.bat
## Inicio rápido

Requiere **Java 17+** (JDK completo si vas a compilar).



Abre <http://localhost:8080>. Ejecuta **desde la raíz del proyecto**
## Reglas clave

| Tema | Regla |
|---|---|
| Notas | 0.0–5.0, actividad ≤ 80 caracteres, sin repetir actividad por materia y estudiante; sin notas el promedio queda vacío |
| Insignias | **Primer despegue** (≥ 1 nota) · **Órbita de excelencia** (prom. ≥ 4.0) · **Cerebrito UCO** (≥ 4.5) · **Master Code** (3+ notas de POO con prom. ≥ 4.5) |
| Registro | Nombre 3–80, contraseña 8–128, documento 5–20 (`A-Z0-9-`); correo y documento únicos; estudiantes eligen 1 de 4 ingenierías; profesores indican área, 1 de 20 materias y el código docente |
| Chat | Estudiante → solo profesores; profesor → estudiantes y profesores; 1–2 000 caracteres; solo lo ven los 2 participantes |
| Calendario | Día 1 (lunes)–7; hora `HH:mm`; clases de **1 hora**; choque de docente o salón → `409`; la materia sale del perfil; solo el dueño elimina |

## Arquitectura

```text
Navegador (dist/: index.html, app.js, styles.css)
   │ fetch · JSON · cabecera X-Orbita-Token
ServidorOrbita (HttpServer, 127.0.0.1:8080) → /api/* y archivos estáticos
   │
GestorAcademico → Persona (abstracta) ← Estudiante, Profesor · Nota (record) · HorarioClase
   │             → PoliticaInsignias ← InsigniasOrbita · Bitacora ← BitacoraMemoria
datos/*.tsv
```


## API

Base `http://localhost:8080`. 



## Datos y configuración
persisten.** Para reiniciar la demo, detén el servidor y borra `datos/`.


- Código docente: variable `ORBITA_CODIGO_DOCENTE`.
=======
# Proyecto-Aulas-de-Ciudad
Tareas Miguel Ángel Pérez García- José Manuel Valencia Gallego
Este Proyecto permite la creación de Profesores / Estudiantes donde el límite no está definido (puede agregar cuantos estudiantes quieras) además de que asigna la nota de la respectiva materia (no solo de una, si no de las que sean necesarias).

