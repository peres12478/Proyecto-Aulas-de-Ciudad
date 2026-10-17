package co.edu.uco.orbita.servicio;
import co.edu.uco.orbita.modelo.Estudiante;
import java.util.List;
/** Estrategia intercambiable de reconocimientos académicos. */
public interface PoliticaInsignias { List<String> evaluar(Estudiante estudiante); }
