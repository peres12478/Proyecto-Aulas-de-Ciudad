package co.edu.uco.orbita.modelo;

import java.util.*;

/** Estudiante que protege sus notas y calcula su promedio aritmético. */
public final class Estudiante extends Persona {
    private final String carrera;
    private final List<Nota> notas = new ArrayList<>();
    /** Crea un estudiante sin evaluaciones iniciales. */
    public Estudiante(String id, String nombre, String correo, String password, String carrera) {
        super(id, nombre, correo, password);
        this.carrera = texto(carrera, "carrera");
    }
    /** Restaura un estudiante desde un hash de contraseña persistido. */
    public Estudiante(String id, String nombre, String correo, String credencial, String carrera, boolean restaurar) {
        super(id, nombre, correo, credencial, true);
        this.carrera = texto(carrera, "carrera");
    }
    /** Carrera cursada por el estudiante. */
    public String getCarrera() { return carrera; }
    /** Copia inmutable del expediente. */
    public List<Nota> getNotas() { return List.copyOf(notas); }
    /** Ausencia de notas no equivale a una calificación de cero. */
    public OptionalDouble calcularPromedio() { return notas.stream().mapToDouble(Nota::valor).average(); }
    // Mutación interna al dominio: el caso de uso público pertenece a Profesor.
    void recibirNota(Nota nota) {
        boolean existe = notas.stream().anyMatch(n -> n.materia().equalsIgnoreCase(nota.materia())
            && n.actividad().equalsIgnoreCase(nota.actividad()));
        if (existe) throw new IllegalArgumentException("La actividad ya tiene nota para este estudiante");
        notas.add(Objects.requireNonNull(nota));
    }
    @Override public String getRol() { return "Estudiante"; }
}
