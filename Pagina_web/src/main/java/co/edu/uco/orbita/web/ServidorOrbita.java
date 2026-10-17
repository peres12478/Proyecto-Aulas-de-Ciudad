package co.edu.uco.orbita.web;

import co.edu.uco.orbita.modelo.*;
import co.edu.uco.orbita.servicio.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Servidor web de Órbita UCO. La API usa los casos de uso del dominio Java. */
public final class ServidorOrbita {
    private static final int PORT = Integer.getInteger("orbita.port", 8080);
    private static final String SUBJECT = "Programación Orientada a Objetos";
    private static final List<String> CARRERAS = List.of("Ingeniería en Sistemas", "Ingeniería Electrónica", "Ingeniería Ambiental", "Ingeniería Industrial");
    private static final List<String> MATERIAS = List.of("Matemáticas", "Física", "Química", "Cálculo Diferencial", "Cálculo Integral", "Álgebra Lineal", "Estadística", "Programación Orientada a Objetos", "Estructuras de Datos", "Bases de Datos", "Algoritmos", "Circuitos Eléctricos", "Electrónica Digital", "Termodinámica", "Mecánica", "Biología", "Ecología", "Gestión de Operaciones", "Investigación de Operaciones", "Dibujo Técnico");
    // Código local de demostración. En producción debe reemplazarse por verificación institucional real.
    private static final String CODIGO_DOCENTE = System.getenv().getOrDefault("ORBITA_CODIGO_DOCENTE", "ORBITA-DOCENTE-DEMO-2026");
    private static final Path DATA = Path.of("datos", "notas-orbita.tsv");
    private static final Path USERS_DATA = Path.of("datos", "usuarios-orbita.tsv");
    private static final Path CHAT_DATA = Path.of("datos", "chats-orbita.tsv");
    private static final Path CHAT_READ_DATA = Path.of("datos", "chats-leidos-orbita.tsv");
    private static final Path SCHEDULE_DATA = Path.of("datos", "horarios-orbita.tsv");
    private final List<HorarioClase> horarios = new CopyOnWriteArrayList<>();
    private final List<Mensaje> mensajes = new CopyOnWriteArrayList<>();
    // Marca de lectura por cuenta y conversación; se conserva entre reinicios.
    private final Map<String, String> lecturasChat = new ConcurrentHashMap<>();
    private final GestorAcademico gestor = new GestorAcademico(new BitacoraMemoria(), new InsigniasOrbita());
    private final Map<String, Cuenta> sesiones = new ConcurrentHashMap<>();
    private final Map<String, String> nombres = new HashMap<>();
    private final Map<String, String> correos = new HashMap<>();
    private final Map<String, String> roles = new ConcurrentHashMap<>();
    private final Map<String, Persona> personas = new ConcurrentHashMap<>();
    private final Map<String, String> tiposDocumento = new ConcurrentHashMap<>();
    private final Map<String, String> documentos = new ConcurrentHashMap<>();
    private final Map<String, String> carreras = new ConcurrentHashMap<>();
    private final Map<String, String> materias = new ConcurrentHashMap<>();
    private final Map<String, String> edades = new ConcurrentHashMap<>();
    private final Map<String, String> universidades = new ConcurrentHashMap<>();
    private final Map<String, String> biografias = new ConcurrentHashMap<>();

    private record Cuenta(String id, String correo, String password, String rol) {}
    private record Mensaje(String id, String senderId, String recipientId, String text, String timestamp) {}
    private ServidorOrbita() throws Exception {
        seed();
        cargarUsuarios();
        cargarNotas();
        cargarMensajes();
        cargarLecturasChat();
        cargarHorarios();
    }
    private void seed() {
        agregar(new Estudiante("E01", "Valentina Ríos", "valentina@uco.demo", "Orbita2026!", "Ingeniería en Sistemas"), "Orbita2026!", "student", "TI", "DEMO1001", "Ingeniería en Sistemas", "");
        agregar(new Estudiante("E02", "Samuel Gómez", "samuel@uco.demo", "Orbita2026!", "Ingeniería en Sistemas"), "Orbita2026!", "student", "TI", "DEMO1002", "Ingeniería en Sistemas", "");
        agregar(new Estudiante("E03", "Luciana Vélez", "luciana@uco.demo", "Orbita2026!", "Ingeniería en Sistemas"), "Orbita2026!", "student", "CC", "DEMO1003", "Ingeniería en Sistemas", "");
        agregar(new Profesor("P01", "Andrés Moreno", "andres@uco.demo", "Orbita2026!", "Ingeniería de Software", SUBJECT), "Orbita2026!", "professor", "CC", "DEMO2001", "Ingeniería de Software", SUBJECT);
        calificar("E01", "Laboratorio de clases", 4.7);
        calificar("E01", "Reto de herencia", 4.4);
        calificar("E01", "Proyecto SOLID", 4.8);
        calificar("E02", "Laboratorio de clases", 3.8);
        calificar("E02", "Reto de herencia", 4.1);
        gestor.logout();
    }
    private void agregar(Persona p, String password, String rol) {
        agregar(p, password, rol, "", "", p instanceof Estudiante e ? e.getCarrera() : ((Profesor)p).getEspecialidad(), p instanceof Profesor pr ? pr.getMateria() : "");
    }
    private void agregar(Persona p, String password, String rol, String tipoDocumento, String documento, String carrera, String materia) {
        gestor.registrar(p); personas.put(p.getId(), p); nombres.put(p.getId(), p.getNombre()); correos.put(p.getId(), p.getCorreo()); roles.put(p.getId(), rol);
        if (tipoDocumento != null && !tipoDocumento.isBlank()) tiposDocumento.put(p.getId(), tipoDocumento);
        if (documento != null && !documento.isBlank()) documentos.put(p.getId(), documento.toUpperCase(Locale.ROOT));
        if (carrera != null) carreras.put(p.getId(), carrera);
        if (materia != null && !materia.isBlank()) materias.put(p.getId(), materia);
    }
    private synchronized void cargarUsuarios() throws IOException {
        if (!Files.exists(USERS_DATA)) return;
        for (String line : Files.readAllLines(USERS_DATA, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            try {
                String[] p = line.split("\\t", -1);
                String id, nombre, correo, rol, credencial, tipoDoc = "", documento = "", carrera, materia = "", edad = "", universidad = "", bio = "";
                if (p.length == 6) { // compatibilidad con la versión anterior del proyecto
                    id=dec(p[0]); nombre=dec(p[1]); correo=dec(p[2]); rol=dec(p[3]); credencial=dec(p[4]);
                    String perfil=dec(p[5]);
                    if (rol.equals("student")) carrera=perfil;
                    else { String[] datos=perfil.split("\\|",2); carrera=datos[0]; materia=datos.length==2?datos[1]:SUBJECT; }
                } else if (p.length == 9 || p.length == 12) {
                    id=dec(p[0]); nombre=dec(p[1]); correo=dec(p[2]); rol=dec(p[3]); credencial=dec(p[4]);
                    tipoDoc=dec(p[5]); documento=dec(p[6]); carrera=dec(p[7]); materia=dec(p[8]);
                    if (p.length == 12) { edad=dec(p[9]); universidad=dec(p[10]); bio=dec(p[11]); }
                } else continue;
                // Los usuarios de demostración ya se crean en seed(); al restaurar su perfil
                // conservamos la identidad inicial y recuperamos únicamente los datos opcionales.
                if (personas.containsKey(id)) {
                    if (!edad.isBlank()) edades.put(id, edad);
                    if (!universidad.isBlank()) universidades.put(id, universidad);
                    if (!bio.isBlank()) biografias.put(id, bio);
                    continue;
                }
                Persona persona;
                if (rol.equals("student")) persona = new Estudiante(id, nombre, correo, credencial, carrera, true);
                else if (rol.equals("professor")) persona = new Profesor(id, nombre, correo, credencial, carrera, materia.isBlank()?SUBJECT:materia, true);
                else continue;
                agregar(persona, "", rol, tipoDoc, documento, carrera, materia);
                if (!edad.isBlank()) edades.put(id, edad);
                if (!universidad.isBlank()) universidades.put(id, universidad);
                if (!bio.isBlank()) biografias.put(id, bio);
            } catch (RuntimeException ex) { System.err.println("Usuario persistido inválido; se omite."); }
        }
    }
    private synchronized void guardarUsuario(Persona persona, String rol, String tipoDoc, String documento, String carrera, String materia) throws IOException {
        Files.createDirectories(USERS_DATA.getParent());
        String line = String.join("\t", enc(persona.getId()), enc(persona.getNombre()), enc(persona.getCorreo()), enc(rol), enc(persona.credencialPersistida()), enc(tipoDoc), enc(documento), enc(carrera), enc(materia)) + System.lineSeparator();
        Files.writeString(USERS_DATA, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    private synchronized void guardarTodosUsuarios() throws IOException {
        Files.createDirectories(USERS_DATA.getParent());
        StringBuilder all = new StringBuilder();
        for (String id : nombres.keySet()) {
            Persona persona = personas.get(id);
            String line = String.join("\t", enc(id), enc(nombres.getOrDefault(id,"")), enc(correos.getOrDefault(id,"")), enc(roles.getOrDefault(id,"")), enc(persona.credencialPersistida()), enc(tiposDocumento.getOrDefault(id,"")), enc(documentos.getOrDefault(id,"")), enc(carreras.getOrDefault(id,"")), enc(materias.getOrDefault(id,"")), enc(edades.getOrDefault(id,"")), enc(universidades.getOrDefault(id,"")), enc(biografias.getOrDefault(id,"")));
            all.append(line).append(System.lineSeparator());
        }
        Files.writeString(USERS_DATA, all.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    private static String enc(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
    private void calificar(String id, String actividad, double valor) {
        gestor.login("andres@uco.demo", "Orbita2026!"); gestor.asignarNota(id, actividad, valor);
    }
    private synchronized void cargarNotas() throws IOException {
        if (!Files.exists(DATA)) return;
        for (String line : Files.readAllLines(DATA, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            try {
                String[] p = line.split("\\t", -1);
                String id = p[0], actividad = new String(Base64.getDecoder().decode(p[1]), StandardCharsets.UTF_8);
                double valor = Double.parseDouble(p[2]);
                gestor.login("andres@uco.demo", "Orbita2026!");
                try { gestor.asignarNota(id, actividad, valor); } catch (IllegalArgumentException ignored) { }
                gestor.logout();
            } catch (RuntimeException ex) { System.err.println("Registro persistido inválido; se omite."); }
        }
    }
    private synchronized void guardarNota(String id, String actividad, double valor) throws IOException {
        Files.createDirectories(DATA.getParent());
        String encoded = Base64.getEncoder().encodeToString(actividad.getBytes(StandardCharsets.UTF_8));
        Files.writeString(DATA, id + "\t" + encoded + "\t" + valor + System.lineSeparator(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public static void main(String[] args) throws Exception {
        ServidorOrbita app = new ServidorOrbita();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/", app::api);
        server.createContext("/", app::staticFiles);
        server.setExecutor(Executors.newCachedThreadPool());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
        server.start();
        System.out.println("ÓRBITA UCO está disponible en http://localhost:" + PORT);
        System.out.println("Datos persistentes: " + DATA.toAbsolutePath());
        System.out.println("Abre el navegador en http://localhost:" + PORT);
    }
    private void api(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        try {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/api/register") && ex.getRequestMethod().equals("POST")) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String nombre=field(body,"name").trim(), email=field(body,"email").trim().toLowerCase(Locale.ROOT);
                String password=field(body,"password"), rol=field(body,"role");
                String tipoDoc=field(body,"documentType").trim().toUpperCase(Locale.ROOT);
                String documento=field(body,"documentNumber").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
                String carrera=field(body,"career").trim(), materia=field(body,"subject").trim();
                String codigoDocente=field(body,"teacherCode");
                if(nombre.length()<3||nombre.length()>80||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||password.length()<8||password.length()>128||!(rol.equals("student")||rol.equals("professor"))) {
                    send(ex,400,"{\"error\":\"Completa nombre, correo válido, rol y contraseña de al menos 8 caracteres.\"}"); return;
                }
                if(!List.of("TI","CC").contains(tipoDoc)||!documento.matches("[A-Z0-9-]{5,20}")) { send(ex,400,"{\"error\":\"Selecciona tipo de documento y escribe un número válido (5 a 20 caracteres).\"}"); return; }
                if(correos.values().stream().anyMatch(c->c.equalsIgnoreCase(email))) { send(ex,409,"{\"error\":\"Ya existe una cuenta con ese correo.\"}"); return; }
                if(documentos.values().stream().anyMatch(d->d.equalsIgnoreCase(documento))) { send(ex,409,"{\"error\":\"Ya existe una cuenta con ese documento.\"}"); return; }
                if(rol.equals("student")) {
                    if(!CARRERAS.contains(carrera)) { send(ex,400,"{\"error\":\"Selecciona una de las cuatro ingenierías disponibles.\"}"); return; }
                    materia="";
                } else {
                    if(!MessageDigest.isEqual(CODIGO_DOCENTE.getBytes(StandardCharsets.UTF_8),codigoDocente.getBytes(StandardCharsets.UTF_8))) { send(ex,403,"{\"error\":\"El código de habilitación docente no es válido. En esta demo usa el código local indicado en el README.\"}"); return; }
                    if(carrera.length()<3||carrera.length()>100||!MATERIAS.contains(materia)) { send(ex,400,"{\"error\":\"Escribe la carrera universitaria y selecciona una materia del listado.\"}"); return; }
                }
                String id=(rol.equals("student")?"E":"P")+Long.toString(System.currentTimeMillis(),36).toUpperCase(Locale.ROOT);
                Persona persona=rol.equals("student")?new Estudiante(id,nombre,email,password,carrera):new Profesor(id,nombre,email,password,carrera,materia);
                try {
                    agregar(persona,password,rol,tipoDoc,documento,carrera,materia);
                    guardarUsuario(persona,rol,tipoDoc,documento,carrera,materia);
                    send(ex,201,"{\"ok\":true,\"id\":"+q(id)+"}");
                } catch(IOException io) { nombres.remove(id);correos.remove(id);roles.remove(id);personas.remove(id);documentos.remove(id);tiposDocumento.remove(id);carreras.remove(id);materias.remove(id);send(ex,500,"{\"error\":\"No se pudo guardar la cuenta.\"}"); }
                  catch(IllegalArgumentException dup) { send(ex,409,"{\"error\":\"No fue posible crear la cuenta. Revisa los datos.\"}"); }
                return;
            }
            if (path.equals("/api/login") && ex.getRequestMethod().equals("POST")) {
                String body=new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
                String identifier=field(body,"identifier").trim(), email=field(body,"email").trim(), password=field(body,"password");
                if(identifier.isBlank()) identifier=email;
                final String loginIdentifier=identifier;
                String normalized=loginIdentifier.replaceAll("\\s+", "");
                String id=correos.entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(loginIdentifier)).map(Map.Entry::getKey).findFirst()
                    .orElseGet(()->documentos.entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(normalized)).map(Map.Entry::getKey).findFirst().orElse(""));
                String correo=id.isBlank()?"":correos.get(id);
                if(id.isEmpty()||!gestorLoginValid(correo,password)) { send(ex,401,"{\"error\":\"Correo o documento y contraseña incorrectos.\"}"); return; }
                String token=UUID.randomUUID().toString(); sesiones.put(token,new Cuenta(id,correo,password,roles.get(id)));
                send(ex,200,"{\"token\":"+q(token)+",\"user\":"+userJson(id)+"}"); return;
            }
            String token = ex.getRequestHeaders().getFirst("X-Orbita-Token");
            Cuenta cuenta = token == null ? null : sesiones.get(token);
            if (cuenta == null) { send(ex, 401, "{\"error\":\"Sesión vencida. Ingresa nuevamente.\"}"); return; }
            if (path.equals("/api/logout") && ex.getRequestMethod().equals("POST")) { sesiones.remove(token); send(ex, 200, "{\"ok\":true}"); return; }
            if (path.equals("/api/state") && ex.getRequestMethod().equals("GET")) { send(ex, 200, stateJson(cuenta)); return; }
            if (path.equals("/api/professors") && ex.getRequestMethod().equals("GET")) {
                if (!cuenta.rol.equals("student") && !cuenta.rol.equals("professor")) { send(ex, 403, "{\"error\":\"No tienes permiso para consultar el directorio.\"}"); return; }
                StringBuilder result = new StringBuilder("[ "); int count = 0;
                for (String id : nombres.keySet()) if ("professor".equals(roles.get(id))) { if (count++ > 0) result.append(','); result.append(userJson(id)); }
                result.append(']'); send(ex, 200, result.toString()); return;
            }
            if (path.equals("/api/people") && ex.getRequestMethod().equals("GET")) {
                StringBuilder result = new StringBuilder("["); int count = 0;
                for (String id : nombres.keySet()) {
                    if (id.equals(cuenta.id)) continue;
                    String otherRole = roles.get(id);
                    boolean allowed = cuenta.rol.equals("student") ? "professor".equals(otherRole) : ("student".equals(otherRole) || "professor".equals(otherRole));
                    if (!allowed) continue;
                    if (count++ > 0) result.append(','); result.append(userJson(id));
                }
                result.append(']'); send(ex, 200, result.toString()); return;
            }
            if (path.equals("/api/messages") && ex.getRequestMethod().equals("GET")) {
                String query = ex.getRequestURI().getRawQuery(); String otherId = "";
                if (query != null) for (String part : query.split("&")) { String[] pair=part.split("=",2); if(pair.length==2 && pair[0].equals("with")) otherId=URLDecoder.decode(pair[1], StandardCharsets.UTF_8); }
                if (!canChat(cuenta, otherId)) { send(ex, 403, "{\"error\":\"No puedes abrir esta conversación.\"}"); return; }
                try { marcarConversacionLeida(cuenta.id, otherId); }
                catch (IOException io) { send(ex, 500, "{\"error\":\"No se pudo actualizar la lectura de mensajes.\"}"); return; }
                send(ex, 200, mensajesJson(cuenta.id, otherId)); return;
            }
            if (path.equals("/api/unread") && ex.getRequestMethod().equals("GET")) {
                send(ex, 200, noLeidosJson(cuenta)); return;
            }
            if (path.equals("/api/messages") && ex.getRequestMethod().equals("POST")) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String recipientId = field(body, "recipientId").trim(), messageText = field(body, "text").trim();
                if (!canChat(cuenta, recipientId)) { send(ex, 403, "{\"error\":\"No puedes enviar mensajes a este usuario.\"}"); return; }
                if (messageText.isBlank() || messageText.length() > 2000) { send(ex, 400, "{\"error\":\"El mensaje debe tener entre 1 y 2000 caracteres.\"}"); return; }
                Mensaje message = new Mensaje(UUID.randomUUID().toString(), cuenta.id, recipientId, messageText, Instant.now().toString());
                mensajes.add(message);
                try { guardarMensaje(message); send(ex, 201, mensajeJson(message)); }
                catch (IOException io) { mensajes.remove(message); send(ex, 500, "{\"error\":\"No se pudo guardar el mensaje.\"}"); }
                return;
            }
            if (path.equals("/api/schedule") && ex.getRequestMethod().equals("GET")) {
                StringBuilder out = new StringBuilder("["); int n = 0;
                for (HorarioClase h : horarios) {
                    if (n++ > 0) out.append(',');
                    out.append("{\"id\":").append(q(h.getId())).append(",\"professorId\":").append(q(h.getProfesorId()))
                       .append(",\"professorName\":").append(q(nombres.getOrDefault(h.getProfesorId(), "Docente")))
                       .append(",\"weekday\":").append(h.getDiaSemana()).append(",\"time\":").append(q(h.getHora()))
                       .append(",\"room\":").append(q(h.getSalon())).append(",\"subject\":").append(q(h.getMateria())).append('}');
                }
                out.append(']'); send(ex, 200, out.toString()); return;
            }
            if (path.equals("/api/schedule") && ex.getRequestMethod().equals("POST")) {
                if (!cuenta.rol.equals("professor")) { send(ex, 403, "{\"error\":\"Solo los profesores pueden crear o modificar sus horarios.\"}"); return; }
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String dayText=field(body,"weekday"), time=field(body,"time").trim(), room=field(body,"room").trim();
                int day; try { day=Integer.parseInt(dayText); } catch(Exception e) { day=0; }
                String subject = materias.getOrDefault(cuenta.id, "").trim();
                if(day<1||day>7||!time.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")||room.isBlank()||room.length()>80||subject.isBlank()) {
                    send(ex,400,"{\"error\":\"Selecciona un día, hora válida y salón. La materia debe estar asignada al profesor.\"}"); return;
                }
                HorarioClase h=new HorarioClase(UUID.randomUUID().toString(),cuenta.id,day,time,room,subject);
                synchronized (horarios) {
                    int start = minutosDesdeMedianoche(time), end = start + 60; // Cada bloque ocupa una hora.
                    for (HorarioClase existente : horarios) {
                        if (existente.getDiaSemana() != day) continue;
                        int otherStart = minutosDesdeMedianoche(existente.getHora()), otherEnd = otherStart + 60;
                        boolean overlap = start < otherEnd && otherStart < end;
                        if (overlap && existente.getProfesorId().equals(cuenta.id)) {
                            send(ex,409,"{\"error\":\"Ya tienes una clase en ese horario. Elige otra hora.\"}"); return;
                        }
                        if (overlap && existente.getSalon().trim().equalsIgnoreCase(room)) {
                            send(ex,409,"{\"error\":\"Ese salón ya está ocupado durante ese horario. Elige otro salón u otra hora.\"}"); return;
                        }
                    }
                    horarios.add(h);
                    try { guardarHorarios(); send(ex,201,"{\"ok\":true,\"id\":"+q(h.getId())+"}"); }
                    catch(IOException io) { horarios.remove(h); send(ex,500,"{\"error\":\"No se pudo guardar el horario.\"}"); }
                }
                return;
            }
            if (path.startsWith("/api/schedule/") && ex.getRequestMethod().equals("DELETE")) {
                if (!cuenta.rol.equals("professor")) { send(ex,403,"{\"error\":\"Solo los profesores pueden eliminar sus horarios.\"}"); return; }
                String id=URLDecoder.decode(path.substring("/api/schedule/".length()),StandardCharsets.UTF_8);
                HorarioClase found=horarios.stream().filter(h->h.getId().equals(id)).findFirst().orElse(null);
                if(found==null){send(ex,404,"{\"error\":\"Horario no encontrado.\"}");return;}
                if(!found.getProfesorId().equals(cuenta.id)){send(ex,403,"{\"error\":\"Solo puedes eliminar tus propios horarios.\"}");return;}
                horarios.remove(found); try{guardarHorarios();send(ex,200,"{\"ok\":true}");}catch(IOException io){horarios.add(found);send(ex,500,"{\"error\":\"No se pudo actualizar el calendario.\"}");} return;
            }
            if (path.equals("/api/profile") && ex.getRequestMethod().equals("PUT")) {
                if (!cuenta.rol.equals("professor")) { send(ex, 403, "{\"error\":\"Solo los profesores pueden editar su perfil académico.\"}"); return; }
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String age = field(body, "age").trim(), university = field(body, "university").trim(), bio = field(body, "bio").trim();
                if (!age.isBlank()) { try { int n = Integer.parseInt(age); if (n < 18 || n > 100) throw new NumberFormatException(); } catch (NumberFormatException err) { send(ex, 400, "{\"error\":\"La edad debe estar entre 18 y 100, o dejarse vacía.\"}"); return; } }
                if (university.length() > 120 || bio.length() > 500) { send(ex, 400, "{\"error\":\"La universidad admite 120 caracteres y la biografía 500.\"}"); return; }
                edades.put(cuenta.id, age); universidades.put(cuenta.id, university); biografias.put(cuenta.id, bio);
                try { guardarTodosUsuarios(); send(ex, 200, "{\"ok\":true,\"user\":" + userJson(cuenta.id) + "}"); }
                catch (IOException err) { send(ex, 500, "{\"error\":\"No se pudo guardar el perfil académico.\"}"); }
                return;
            }
            if (path.equals("/api/grades") && ex.getRequestMethod().equals("POST")) {
                if (!cuenta.rol.equals("professor")) { send(ex, 403, "{\"error\":\"Solo un profesor puede registrar notas\"}"); return; }
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String id = field(body, "studentId"), activity = field(body, "activity");
                double value;
                try { value = Double.parseDouble(field(body, "value")); } catch (Exception e) { send(ex, 400, "{\"error\":\"Nota inválida\"}"); return; }
                if (!roles.getOrDefault(id, "").equals("student") || activity.isBlank() || activity.length() > 80 || !Double.isFinite(value) || value < 0 || value > 5) { send(ex, 400, "{\"error\":\"Verifica estudiante, actividad y nota entre 0 y 5\"}"); return; }
                try {
                    gestor.login(cuenta.correo, cuenta.password); gestor.asignarNota(id, activity, value); gestor.logout();
                    guardarNota(id, activity, value);
                    send(ex, 201, stateJson(cuenta));
                } catch (SecurityException e) { gestor.logout(); send(ex, 403, "{\"error\":" + q(e.getMessage()) + "}"); }
                  catch (IllegalArgumentException e) { gestor.logout(); send(ex, 400, "{\"error\":" + q(e.getMessage()) + "}"); }
                return;
            }
            send(ex, 404, "{\"error\":\"Ruta API no encontrada\"}");
        } catch (Exception e) { send(ex, 500, "{\"error\":\"Error interno del servidor\"}"); }
    }
    private synchronized boolean gestorLoginValid(String email, String password) {
        try { gestor.login(email, password); gestor.logout(); return true; } catch (RuntimeException e) { gestor.logout(); return false; }
    }
    private boolean canChat(Cuenta cuenta, String otherId) {
        if (otherId == null || otherId.isBlank() || otherId.equals(cuenta.id) || !personas.containsKey(otherId)) return false;
        String otherRole = roles.get(otherId);
        return cuenta.rol.equals("student") ? "professor".equals(otherRole)
            : cuenta.rol.equals("professor") && ("student".equals(otherRole) || "professor".equals(otherRole));
    }
    private synchronized void cargarMensajes() throws IOException {
        if (!Files.exists(CHAT_DATA)) return;
        for (String line : Files.readAllLines(CHAT_DATA, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            try { String[] p=line.split("\\t",-1); if(p.length==5) mensajes.add(new Mensaje(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),dec(p[4]))); }
            catch (RuntimeException ignored) { System.err.println("Se omitió un mensaje persistido inválido."); }
        }
    }
    private synchronized void guardarMensaje(Mensaje m) throws IOException {
        Files.createDirectories(CHAT_DATA.getParent());
        String line=String.join("\t",enc(m.id),enc(m.senderId),enc(m.recipientId),enc(m.text),enc(m.timestamp))+System.lineSeparator();
        Files.writeString(CHAT_DATA,line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private static int minutosDesdeMedianoche(String hora) {
        String[] partes = hora.split(":");
        return Integer.parseInt(partes[0]) * 60 + Integer.parseInt(partes[1]);
    }
    private synchronized void cargarHorarios() throws IOException {
        if (!Files.exists(SCHEDULE_DATA)) return;
        for(String line:Files.readAllLines(SCHEDULE_DATA,StandardCharsets.UTF_8)) if(!line.isBlank()) try {
            String[] p=line.split("\\t",-1); if(p.length==6) horarios.add(new HorarioClase(dec(p[0]),dec(p[1]),Integer.parseInt(p[2]),dec(p[3]),dec(p[4]),dec(p[5])));
        } catch(RuntimeException ignored) { System.err.println("Se omitió un horario inválido."); }
    }
    private synchronized void guardarHorarios() throws IOException {
        Files.createDirectories(SCHEDULE_DATA.getParent()); StringBuilder out=new StringBuilder();
        for(HorarioClase h:horarios) out.append(enc(h.getId())).append('\t').append(enc(h.getProfesorId())).append('\t').append(h.getDiaSemana()).append('\t').append(enc(h.getHora())).append('\t').append(enc(h.getSalon())).append('\t').append(enc(h.getMateria())).append(System.lineSeparator());
        Path tmp=SCHEDULE_DATA.resolveSibling(SCHEDULE_DATA.getFileName()+".tmp"); Files.writeString(tmp,out,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        try{Files.move(tmp,SCHEDULE_DATA,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,SCHEDULE_DATA,StandardCopyOption.REPLACE_EXISTING);}
    }
    private String claveLectura(String userId, String otherId) { return userId + "\t" + otherId; }
    private synchronized void cargarLecturasChat() throws IOException {
        if (!Files.exists(CHAT_READ_DATA)) return;
        for (String line : Files.readAllLines(CHAT_READ_DATA, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            try { String[] p=line.split("\\t",-1); if(p.length==3) lecturasChat.put(dec(p[0])+"\t"+dec(p[1]),dec(p[2])); }
            catch (RuntimeException ignored) { System.err.println("Se omitió una marca de lectura inválida."); }
        }
    }
    private synchronized void guardarLecturasChat() throws IOException {
        Files.createDirectories(CHAT_READ_DATA.getParent());
        StringBuilder out = new StringBuilder();
        lecturasChat.forEach((key, timestamp) -> { String[] ids=key.split("\\t",-1); if(ids.length==2) out.append(enc(ids[0])).append('\t').append(enc(ids[1])).append('\t').append(enc(timestamp)).append(System.lineSeparator()); });
        Path temp=CHAT_READ_DATA.resolveSibling(CHAT_READ_DATA.getFileName()+".tmp");
        Files.writeString(temp,out,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        try { Files.move(temp,CHAT_READ_DATA,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp,CHAT_READ_DATA,StandardCopyOption.REPLACE_EXISTING); }
    }
    private synchronized void marcarConversacionLeida(String userId, String otherId) throws IOException {
        String newest = null;
        for (Mensaje m : mensajes) if ((m.senderId.equals(userId)&&m.recipientId.equals(otherId)) || (m.senderId.equals(otherId)&&m.recipientId.equals(userId))) {
            if (newest == null || m.timestamp.compareTo(newest)>0) newest=m.timestamp;
        }
        if (newest == null) return;
        String key=claveLectura(userId,otherId), previous=lecturasChat.get(key);
        if (previous == null || newest.compareTo(previous)>0) { lecturasChat.put(key,newest); guardarLecturasChat(); }
    }
    private String noLeidosJson(Cuenta cuenta) {
        StringBuilder contacts=new StringBuilder("{"); int total=0, emitted=0;
        for (String otherId : nombres.keySet()) {
            if (!canChat(cuenta,otherId)) continue;
            String lastRead=lecturasChat.getOrDefault(claveLectura(cuenta.id,otherId),""); int count=0;
            for (Mensaje m : mensajes) if (m.recipientId.equals(cuenta.id) && m.senderId.equals(otherId) && m.timestamp.compareTo(lastRead)>0) count++;
            if (count>0) { if(emitted++>0) contacts.append(','); contacts.append(q(otherId)).append(':').append(count); total+=count; }
        }
        contacts.append('}'); return "{\"total\":"+total+",\"byContact\":"+contacts+"}";
    }
    private String mensajesJson(String currentId, String otherId) {
        StringBuilder b=new StringBuilder("["); int count=0;
        for(Mensaje m:mensajes) if((m.senderId.equals(currentId)&&m.recipientId.equals(otherId))||(m.senderId.equals(otherId)&&m.recipientId.equals(currentId))) {
            if(count++>0)b.append(','); b.append(mensajeJson(m));
        }
        return b.append(']').toString();
    }
    private String mensajeJson(Mensaje m) { return "{\"id\":"+q(m.id)+",\"senderId\":"+q(m.senderId)+",\"recipientId\":"+q(m.recipientId)+",\"text\":"+q(m.text)+",\"timestamp\":"+q(m.timestamp)+"}"; }
    private String stateJson(Cuenta cuenta) {
        gestor.login(cuenta.correo, cuenta.password);
        try {
            StringBuilder students = new StringBuilder("[");
            List<Estudiante> list = cuenta.rol.equals("professor") ? gestor.listarEstudiantes() : List.of(gestor.consultarEstudiante(cuenta.id));
            for (int i=0;i<list.size();i++) { Estudiante s=list.get(i); if(i>0)students.append(','); students.append("{\"id\":").append(q(s.getId())).append(",\"name\":").append(q(s.getNombre())).append(",\"email\":").append(q(s.getCorreo())).append(",\"role\":\"student\",\"career\":").append(q(carreras.getOrDefault(s.getId(), s.getCarrera()))).append("}"); }
            students.append(']');
            if (cuenta.rol.equals("student")) students = new StringBuilder("[" + userJson(cuenta.id) + "]");
            StringBuilder grades = new StringBuilder("["); int count=0;
            for (Estudiante s : list) {
                if (cuenta.rol.equals("student") && !s.getId().equals(cuenta.id)) continue;
                for (Nota n : s.getNotas()) { if(count++>0)grades.append(','); grades.append("{\"studentId\":").append(q(s.getId())).append(",\"activity\":").append(q(n.actividad())).append(",\"value\":").append(n.valor()).append(",\"subject\":").append(q(n.materia())).append(",\"professorId\":").append(q(n.profesorId())).append(",\"date\":").append(q(n.fecha().toString())).append('}'); }
            }
            grades.append(']');
            StringBuilder badges = new StringBuilder("{");
            for (int i=0;i<list.size();i++) { Estudiante st=list.get(i); if(i>0)badges.append(','); badges.append(q(st.getId())).append(':').append(stringArray(gestor.insignias(st.getId()))); }
            badges.append('}');
            List<String> history = cuenta.rol.equals("professor") ? gestor.historial() : List.of();
            StringBuilder h = new StringBuilder("["); for(int i=0;i<history.size();i++){if(i>0)h.append(',');h.append("{\"date\":").append(q(Instant.now().toString())).append(",\"text\":").append(q(history.get(i))).append('}');}h.append(']');
            int studentCount = 0, totalGrades = 0; double totalValues = 0;
            for (Persona persona : personas.values()) if (persona instanceof Estudiante st) { studentCount++; for (Nota note : st.getNotas()) { totalValues += note.valor(); totalGrades++; } }
            String globalAverage = totalGrades == 0 ? "null" : String.format(Locale.ROOT, "%.4f", totalValues / totalGrades);
            String globalStats = "{\"studentCount\":"+studentCount+",\"gradeCount\":"+totalGrades+",\"average\":"+globalAverage+"}";
            return "{\"user\":"+userJson(cuenta.id)+",\"users\":"+students+",\"grades\":"+grades+",\"badges\":"+badges+",\"history\":"+h+",\"globalStats\":"+globalStats+"}";
        } finally { gestor.logout(); }
    }
    private static String stringArray(List<String> values) { StringBuilder b=new StringBuilder("["); for(int i=0;i<values.size();i++){if(i>0)b.append(',');b.append(q(values.get(i)));} return b.append(']').toString(); }
    private String userJson(String id) { return "{\"id\":"+q(id)+",\"name\":"+q(nombres.get(id))+",\"email\":"+q(correos.get(id))+",\"role\":"+q(roles.get(id))+",\"career\":"+q(carreras.getOrDefault(id,""))+",\"subject\":"+q(materias.getOrDefault(id,""))+",\"documentType\":"+q(tiposDocumento.getOrDefault(id,""))+",\"age\":"+q(edades.getOrDefault(id,""))+",\"university\":"+q(universidades.getOrDefault(id,""))+",\"bio\":"+q(biografias.getOrDefault(id,""))+"}"; }
    private static String field(String json, String key) {
        Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"|([^,}\\s]+))").matcher(json);
        if(!m.find()) return ""; String value=m.group(1)!=null?m.group(1):m.group(2);
        return value.replace("\\\"","\"").replace("\\\\","\\").replace("\\n","\n");
    }
    private static String q(String s) { if(s==null)return "null"; return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""; }
    private static void send(HttpExchange ex,int status,String body)throws IOException { byte[] b=body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(status,b.length); try(OutputStream out=ex.getResponseBody()){out.write(b);} }
    private void staticFiles(HttpExchange ex)throws IOException {
        String path=ex.getRequestURI().getPath(); if(path.equals("/"))path="/index.html";
        Path root=Path.of("dist").toAbsolutePath().normalize(); Path file=root.resolve(path.substring(1)).normalize();
        if(!file.startsWith(root)||!Files.isRegularFile(file)){byte[] b="No encontrado".getBytes(StandardCharsets.UTF_8);ex.sendResponseHeaders(404,b.length);ex.getResponseBody().write(b);ex.close();return;}
        String lower=file.toString().toLowerCase(Locale.ROOT); String type=lower.endsWith(".css")?"text/css; charset=utf-8":lower.endsWith(".js")?"text/javascript; charset=utf-8":lower.endsWith(".svg")?"image/svg+xml":lower.endsWith(".png")?"image/png":lower.endsWith(".ico")?"image/x-icon":"text/html; charset=utf-8";
        ex.getResponseHeaders().set("Content-Type",type);byte[] b=Files.readAllBytes(file);ex.sendResponseHeaders(200,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);}
    }
}
