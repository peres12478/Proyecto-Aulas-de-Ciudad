# ◉ ÓRBITA UCO

Portal académico de demostración: dominio orientado a objetos en **Java** + web de una sola página, conectados por una **API HTTP local**. Notas, insignias, chat y calendario de clases. Solo requiere el JDK (sin Maven, Gradle ni npm).

> ⚠️ Proyecto de clase con datos ficticios, sin afiliación oficial con la UCO. **No exponer a Internet.**

## Funciones

- **Notas e insignias:** el profesor califica (0.0–5.0) solo en su materia; el estudiante ve notas, promedio y 4 insignias recalculadas con sus notas actuales.
- **Cuentas:** registro con documento (TI/CC), acceso por correo o documento, contraseñas con PBKDF2 + sal.
- **Directorio docente:** búsqueda sin importar tildes ni mayúsculas.
- **Chat:** mensajes privados con sondeo cada 2,5 s y contador de no leídos persistente.
- **Calendario semanal:** el profesor publica clases; el servidor evita choques de docente y de salón.
- **Bitácora** de accesos y calificaciones (solo profesores).

## Inicio rápido

Requiere **Java 17+** (JDK completo si vas a compilar).

```sh
./INICIAR-ORBITA.sh          # macOS/Linux (antes: chmod +x)
INICIAR-ORBITA.bat           # Windows (doble clic)
java -jar portal-orbita.jar  # manual
```

Abre <http://localhost:8080>. Ejecuta **desde la raíz del proyecto**: `dist/` y `datos/` usan rutas relativas.

**Cuentas demo** (contraseña `Orbita2026!`): `valentina@uco.demo`, `samuel@uco.demo`, `luciana@uco.demo` (estudiantes) y `andres@uco.demo` (profesor, POO). Documentos `DEMO1001`–`DEMO1003` y `DEMO2001`. Al entrar, elige el rol de la cuenta. Código para registrar profesores: `ORBITA-DOCENTE-DEMO-2026`.

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

**Java es la fuente de verdad:** autenticación, roles, validación y persistencia viven en el servidor. POO aplicada: herencia, encapsulamiento (solo `Profesor` asigna notas), inmutabilidad, inyección por constructor y patrón Estrategia.

Código en `src/main/java/co/edu/uco/orbita/` (`modelo/`, `servicio/`, `web/ServidorOrbita.java`, `consola/Main.java`), pruebas en `src/test/`, frontend en `dist/`.

## API

Base `http://localhost:8080`. Todas exigen `X-Orbita-Token` salvo `register` y `login`. Errores: `{"error":"mensaje"}`.

| Método y ruta | Rol | Uso |
|---|---|---|
| `POST /api/register` · `/api/login` · `/api/logout` | Público / todos | Cuenta y sesión (`login` usa `identifier` + `password`) |
| `GET /api/state` | Todos | Usuario, notas, insignias, bitácora y estadísticas |
| `GET /api/professors` · `/api/people` | Todos | Directorio y contactos de chat |
| `GET` / `POST /api/messages` | Todos | Leer (`?with=id`) o enviar mensajes |
| `GET /api/unread` | Todos | No leídos por contacto |
| `GET /api/schedule` | Todos | Horarios |
| `POST /api/schedule` · `DELETE /api/schedule/{id}` | Profesor | Crear (`weekday`, `time`, `room`) o eliminar el propio |
| `PUT /api/profile` | Profesor | Edad 18–100, universidad ≤ 120, bio ≤ 500 |
| `POST /api/grades` | Profesor | `studentId`, `activity`, `value` |

Códigos: `200/201` ok · `400` datos inválidos · `401` sin sesión · `403` sin permiso · `404` no existe · `409` duplicado o choque · `500` error interno.

## Datos y configuración

`datos/` (se crea solo) guarda TSV con campos en Base64: `usuarios-orbita`, `notas-orbita`, `chats-orbita`, `chats-leidos-orbita` y `horarios-orbita`. **Sesiones y bitácora no persisten.** Para reiniciar la demo, detén el servidor y borra `datos/`.

- Puerto: `java -Dorbita.port=9090 -jar portal-orbita.jar` (por defecto 8080).
- Código docente: variable `ORBITA_CODIGO_DOCENTE`.

## Compilar y probar

```sh
mkdir -p build/classes && find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 -d build/classes @build/sources.txt
jar cfm portal-orbita.jar MANIFEST.MF -C build/classes .
sh test.sh   # pruebas de dominio y autorización (no cubren HTTP, chat ni calendario)
sh run.sh    # demo de consola
```

## Seguridad y limitaciones

- **Sí:** PBKDF2-HMAC-SHA256 (120 000 iteraciones), comparaciones en tiempo constante, permisos por rol y propiedad en el servidor, protección contra *path traversal*, escape de HTML, escucha solo en `127.0.0.1`.
- **No:** HTTPS, expiración de sesión, límite de intentos, cifrado de datos ni verificación real de docentes (el código es una barrera local). El JSON se lee con regex, no con un parser.
- **Bug conocido:** al reiniciar, las notas se reaplican a nombre del profesor demo (POO), porque `notas-orbita.tsv` no guarda profesor ni materia.
- `GestorAcademico` usa una sola sesión interna (frágil con peticiones simultáneas) y las sesiones guardan la contraseña en memoria.
- Las 5 notas sembradas no se persisten; la bitácora se pierde al reiniciar.

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `javac: not found` / pide Java 17 | Instala un JDK 17+ y agrégalo al `PATH` |
| `Address already in use` | Usa `-Dorbita.port=9090` |
| Página 404 | Ejecuta desde la raíz del proyecto |
| "Sesión vencida" | El servidor se reinició: vuelve a entrar |
| "Selecciona el rol correcto" | Cambia el selector Estudiante/Profesor |
