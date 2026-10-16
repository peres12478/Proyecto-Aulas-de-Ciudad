package co.edu.uco.orbita.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/** Profesor autorizado para calificar exclusivamente su propia materia. */
public final class Profesor extends Persona {
    private final String especialidad;
    private final String materia;
    /** Crea un docente con una única materia asignada. */
    public Profesor(String id, String nombre, String correo, String password, String especialidad, String materia) {
        super(id, nombre, correo, password);
        this.especialidad = texto(especialidad, "especialidad");
        this.materia = texto(materia, "materia");
    }
    /** Restaura un profesor desde un hash de contraseña persistido. */
    public Profesor(String id, String nombre, String correo, String credencial, String especialidad, String materia, boolean restaurar) {
        super(id, nombre, correo, credencial, true);
        this.especialidad = texto(especialidad, "especialidad");
        this.materia = texto(materia, "materia");
    }
    /** Área de especialidad del docente. */
    public String getEspecialidad() { return especialidad; }
    /** Materia que este profesor puede calificar. */
    public String getMateria() { return materia; }
    /** Único comando público del dominio que asigna una evaluación. */
    public void asignarNota(Estudiante estudiante, String actividad, double valor) {
        Objects.requireNonNull(estudiante, "estudiante").recibirNota(
            new Nota(materia, actividad, valor, getId(), LocalDateTime.now()));
    }
    @Override public String getRol() { return "Profesor"; }
}
