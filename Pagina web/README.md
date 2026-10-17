# ÓRBITA UCO — Aplicación académica integrada

Sistema educativo de demostración que conecta el dominio orientado a objetos de Java con una interfaz web mediante una API HTTP local. Usa únicamente el JDK (sin Maven, Gradle, npm ni bibliotecas Java externas).

> Proyecto académico con usuarios ficticios. No es una plataforma oficial de la UCO ni está diseñado para exponerse a Internet.

## Chats en tiempo real (actualización automática)

- Estudiantes pueden contactar a cualquier profesor registrado desde **Buscar profesores → Contactar por chat** o desde **Chats**.
- Profesores pueden buscar a otros docentes en **Directorio docente** y conversar con estudiantes y profesores desde **Chats**.
- La bandeja actualiza mensajes automáticamente cada 2,5 segundos mientras la vista de chats está abierta. Es chat casi en tiempo real mediante sondeo HTTP, sin WebSocket ni servicios externos.
- Los mensajes se guardan en `datos/chats-orbita.tsv` y se recuperan después de reiniciar el servidor.
- La pestaña **Chats** muestra una insignia verde con el total de mensajes pendientes de leer, y cada contacto muestra su propio contador. Se actualiza cada 2,5 segundos aunque estés en otra sección.
- Al abrir una conversación, sus mensajes pendientes se marcan como leídos. Las marcas se conservan en `datos/chats-leidos-orbita.tsv`, también después de reiniciar el servidor.
- Cada conversación está restringida a sus participantes. Los estudiantes solo pueden iniciar chats con profesores; los profesores pueden conversar con estudiantes y otros profesores.
- Los mensajes admiten texto de hasta 2.000 caracteres. Esta demo no implementa archivos adjuntos, notificaciones push ni cifrado de extremo a extremo.

## Calendario semanal de clases

- La pestaña **Calendario de clases** muestra una agenda de lunes a domingo, inicialmente vacía hasta que los docentes publiquen sus horarios.
- Cada profesor puede programar una clase indicando día de la semana, hora, salón y materia, y eliminar únicamente sus propios horarios.
- Los estudiantes pueden consultar el calendario sin modificarlo y filtrar por día, profesor o materia (por ejemplo, Termodinámica).
- Los horarios se guardan en `datos/horarios-orbita.tsv`, se conservan al reiniciar el servidor y se repiten semanalmente. No se asignan clases automáticamente ni se generan horarios por defecto.
- La entidad `HorarioClase` forma parte del modelo Java; la API valida el rol y la propiedad del horario antes de permitir cambios.

## Inicio rápido en Windows

1. Instala Java JDK 17 o superior y verifica `java -version`.
2. Descomprime la carpeta completa; no muevas el JAR fuera de ella porque necesita `dist/`.
3. Ejecuta `INICIAR-ORBITA.bat`.
4. Se iniciará el servidor Java y se abrirá `http://localhost:8080` en el navegador. Mantén abierta la ventana del servidor mientras uses el portal.
5. Para detenerlo, pulsa Ctrl+C en la ventana del servidor.

### Crear una cuenta nueva

En la pantalla inicial pulsa **Crear cuenta nueva**. El formulario solicita nombre, correo, tipo y número de documento y contraseña de al menos 8 caracteres. El acceso posterior acepta correo o número de documento. Los estudiantes seleccionan una de las cuatro ingenierías disponibles. Los profesores escriben su carrera/área, seleccionan la materia que dictan y deben introducir el código local de habilitación docente indicado más abajo. La cuenta se guarda en `datos/usuarios-orbita.tsv`; las contraseñas se almacenan como hash PBKDF2 con sal. Tras crearla, se inicia sesión automáticamente.

El código docente de esta demo es solo una barrera local de prueba, no una credencial del Ministerio ni una verificación oficial. En un sistema real, el rol de profesor debe aprobarlo un administrador institucional o un servicio de validación autorizado.

También puedes iniciar el servidor manualmente desde la carpeta del proyecto con `java -jar portal-orbita.jar`. Después abre `http://localhost:8080`.

### macOS / Linux

```sh
chmod +x INICIAR-ORBITA.sh
./INICIAR-ORBITA.sh
```

### Credenciales ficticias

Contraseña para las cuatro cuentas: `Orbita2026!`

| Rol | Nombre | Correo |
|---|---|---|
| Estudiante | Valentina Ríos | `valentina@uco.demo` |
| Estudiante | Samuel Gómez | `samuel@uco.demo` |
| Estudiante | Luciana Vélez | `luciana@uco.demo` |
| Profesor | Andrés Moreno | `andres@uco.demo` |

Selecciona el rol correspondiente al correo. Estas cuentas son solo para la demostración.

## Arquitectura

```text
INICIAR-ORBITA.bat / portal-orbita.jar
                  │
                  ▼
     ServidorOrbita (Java HttpServer)
                  │
       API HTTP local / JSON
          /api/register
          /api/login
          /api/state
          /api/grades
          /api/schedule
          /api/logout
                  │
                  ▼
      GestorAcademico (casos de uso)
       ├── Persona (abstracta)
       │    ├── Estudiante
       │    └── Profesor
       ├── Nota (record inmutable)
       ├── HorarioClase
       ├── PoliticaInsignias / InsigniasOrbita
       └── Bitacora / BitacoraMemoria
                  │
                  ▼
      datos/notas-orbita.tsv
      datos/usuarios-orbita.tsv (hashes de contraseña)
```

- **Java es la fuente de verdad** para autenticación, roles, autorización, validación de notas, cálculo de dominio e historial.
- **`dist/app.js`** presenta la interfaz e invoca la API con `fetch`; no mantiene una copia independiente de notas en `localStorage`.
- **`dist/index.html` y `dist/styles.css`** son la capa visual. El servidor Java sirve estos archivos y la API desde el mismo origen.
- Las notas nuevas se guardan en `datos/notas-orbita.tsv` y se vuelven a cargar al reiniciar. Las cuentas nuevas se guardan en `datos/usuarios-orbita.tsv`, con contraseñas derivadas mediante PBKDF2 y sal; la carpeta `datos/` se crea automáticamente.
- El favicon propio de Órbita UCO está en `dist/favicon.ico` y `dist/favicon.svg`, para que el icono de la pestaña no dependa de otro portal.
- El servidor escucha solo en `127.0.0.1` por defecto. No lo publiques en una red o Internet sin una revisión de seguridad.

## Flujo de registro de nota

1. El navegador envía estudiante, actividad y valor a `POST /api/grades`.
2. Java valida la sesión del profesor y los datos.
3. `GestorAcademico` llama a `Profesor.asignarNota`, que crea una `Nota` validada y la agrega al expediente del estudiante.
4. Java guarda el cambio en disco y devuelve el estado actualizado en JSON.
5. La web actualiza promedios, tablas e insignias usando la respuesta del servidor.

## Estructura principal

- `src/main/java/co/edu/uco/orbita/modelo/`: entidades del dominio y POO.
- `src/main/java/co/edu/uco/orbita/servicio/`: casos de uso, reglas e interfaces de servicios.
- `src/main/java/co/edu/uco/orbita/web/ServidorOrbita.java`: servidor HTTP, API y entrega de archivos web.
- `src/main/java/co/edu/uco/orbita/consola/Main.java`: demostración de consola original.
- `src/test/java/`: pruebas de dominio y autorización.
- `dist/`: HTML, CSS y JavaScript del portal.
- `portal-orbita.jar`: servidor Java compilado.
- `INICIAR-ORBITA.bat`: inicio de servidor y navegador en Windows.

## Compilar y probar

Para compilar manualmente desde la raíz del proyecto:

```sh
mkdir -p build/classes
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 -d build/classes @build/sources.txt
jar cfm portal-orbita.jar MANIFEST.MF -C build/classes .
```

Para ejecutar las pruebas originales:

```sh
sh test.sh
```

La aplicación está pensada para una demo local con datos ficticios. La sesión se mantiene en memoria del servidor y las notas sí persisten en disco; la bitácora de esta versión se reinicia cuando se reinicia el servidor.


## Mejoras de registro e identidad académica

- El registro solicita tipo y número de documento; el inicio de sesión acepta correo electrónico o número de documento.
- Los estudiantes solo pueden seleccionar Ingeniería en Sistemas, Ingeniería Electrónica, Ingeniería Ambiental o Ingeniería Industrial.
- Los profesores indican su carrera/área profesional y seleccionan una materia de una lista académica.
- El registro docente exige un código de habilitación local. Para esta demostración, el código predeterminado es `ORBITA-DOCENTE-DEMO-2026`. Se puede cambiar configurando la variable de entorno `ORBITA_CODIGO_DOCENTE` antes de iniciar el servidor. Este código es ficticio para la demo: no verifica ni representa una credencial oficial del Ministerio de Educación. Para un despliegue real debe sustituirse por un proceso de validación institucional.
- Los documentos y perfiles se guardan en `datos/usuarios-orbita.tsv`; las contraseñas se conservan como hashes con sal. Las cuentas antiguas con el formato anterior se pueden cargar por compatibilidad, pero solo las cuentas nuevas tienen documento asociado.
- La sección **Plan de vuelo académico** propone una misión de estudio según el progreso del estudiante.

### Cuentas de demostración

- Estudiante: `valentina@uco.demo` / `Orbita2026!` (documento de prueba: `DEMO1001`).
- Profesor: `andres@uco.demo` / `Orbita2026!` (documento de prueba: `DEMO2001`).
- Código de registro docente: `ORBITA-DOCENTE-DEMO-2026`.

Los documentos de demostración son ficticios. No introduzcas documentos reales en un entorno de prueba compartido.
