package co.edu.uco.orbita.servicio;
import co.edu.uco.orbita.modelo.*;
import java.util.*;

/** Casos de uso y control de sesión. Sus dependencias se inyectan por constructor. */
public final class GestorAcademico {
    private final List<Persona> personas = new ArrayList<>();
    private final Bitacora bitacora;
    private final PoliticaInsignias politica;
    private Persona sesion;
    /** Construye el gestor con los puertos de bitácora e insignias. */
    public GestorAcademico(Bitacora bitacora, PoliticaInsignias politica) {
        this.bitacora = Objects.requireNonNull(bitacora);
        this.politica = Objects.requireNonNull(politica);
    }
    /** Registro de arranque; la consola no expone administración de identidades. */
    public void registrar(Persona persona) {
        Objects.requireNonNull(persona);
        if (personas.stream().anyMatch(p -> p.getId().equals(persona.getId()) || p.getCorreo().equals(persona.getCorreo())))
            throw new IllegalArgumentException("ID o correo duplicado");
        personas.add(persona);
    }
    /** Un intento de acceso fallido invalida cualquier sesión anterior. */
    public Persona login(String correo, String password) {
        sesion = null;
        Persona persona = personas.stream().filter(p -> p.getCorreo().equalsIgnoreCase(correo == null ? "" : correo.trim()))
            .filter(p -> p.autenticar(password)).findFirst().orElseThrow(() -> new SecurityException("Credenciales incorrectas"));
        sesion = persona;
        bitacora.registrar("Acceso de " + persona.getNombre());
        return persona;
    }
    /** Finaliza la sesión actual y registra la salida. */
    public void logout() { if (sesion != null) bitacora.registrar("Salida de " + sesion.getNombre()); sesion = null; }
    private Profesor exigirProfesor() {
        if (!(sesion instanceof Profesor profesor)) throw new SecurityException("Se requiere sesión de profesor");
        return profesor;
    }
    /** Devuelve el grupo únicamente al profesor autenticado. */
    public List<Estudiante> listarEstudiantes() {
        exigirProfesor();
        return personas.stream().filter(Estudiante.class::isInstance).map(Estudiante.class::cast).toList();
    }
    /** Impide consultar expedientes ajenos desde una sesión de estudiante. */
    public Estudiante consultarEstudiante(String id) {
        if (sesion == null || (sesion instanceof Estudiante && !sesion.getId().equals(id)))
            throw new SecurityException("Acceso no autorizado");
        return personas.stream().filter(p -> p instanceof Estudiante && p.getId().equals(id))
            .map(Estudiante.class::cast).findFirst().orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
    }
    /** Asigna una evaluación usando la sesión de profesor y registra el resultado. */
    public void asignarNota(String estudianteId, String actividad, double valor) {
        Profesor profesor = exigirProfesor();
        Estudiante estudiante = consultarEstudiante(estudianteId);
        profesor.asignarNota(estudiante, actividad, valor);
        bitacora.registrar(profesor.getNombre() + " calificó " + actividad + " para " + estudiante.getNombre() + ": " + valor);
    }
    /** Calcula las insignias del expediente autorizado. */
    public List<String> insignias(String id) { return politica.evaluar(consultarEstudiante(id)); }
    /** Devuelve el historial únicamente al profesor autenticado. */
    public List<String> historial() { exigirProfesor(); return bitacora.consultar(); }
}
