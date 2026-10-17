package co.edu.uco.orbita.modelo;

import java.time.LocalDateTime;

/** Evaluación inmutable, con trazabilidad y valor finito entre 0 y 5. */
public record Nota(String materia, String actividad, double valor, String profesorId, LocalDateTime fecha) {
    public Nota {
        materia = Persona.texto(materia, "materia");
        actividad = Persona.texto(actividad, "actividad");
        profesorId = Persona.texto(profesorId, "profesor");
        if (!Double.isFinite(valor) || valor < 0 || valor > 5)
            throw new IllegalArgumentException("La nota debe estar entre 0.0 y 5.0");
        if (fecha == null) throw new IllegalArgumentException("Fecha obligatoria");
    }
}
