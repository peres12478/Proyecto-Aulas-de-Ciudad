package co.edu.uco.orbita.modelo;

import java.util.Objects;

/** Horario semanal publicado por un docente en el calendario de Órbita UCO. */
public final class HorarioClase {
    private final String id;
    private final String profesorId;
    private final int diaSemana; // ISO-8601: lunes=1 ... domingo=7
    private final String hora;
    private final String salon;
    private final String materia;

    public HorarioClase(String id, String profesorId, int diaSemana, String hora, String salon, String materia) {
        this.id = Objects.requireNonNull(id);
        this.profesorId = Objects.requireNonNull(profesorId);
        if (diaSemana < 1 || diaSemana > 7) throw new IllegalArgumentException("El día debe estar entre lunes y domingo.");
        this.diaSemana = diaSemana;
        this.hora = Objects.requireNonNull(hora);
        this.salon = Objects.requireNonNull(salon);
        this.materia = Objects.requireNonNull(materia);
    }

    public String getId() { return id; }
    public String getProfesorId() { return profesorId; }
    public int getDiaSemana() { return diaSemana; }
    public String getHora() { return hora; }
    public String getSalon() { return salon; }
    public String getMateria() { return materia; }
}
