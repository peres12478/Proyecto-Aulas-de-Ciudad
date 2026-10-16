package co.edu.uco.orbita.servicio;
import co.edu.uco.orbita.modelo.*;
import java.util.*;
/** Insignias vigentes, recalculadas con evidencia académica actual. */
public final class InsigniasOrbita implements PoliticaInsignias {
    @Override public List<String> evaluar(Estudiante estudiante) {
        List<String> resultado = new ArrayList<>();
        if (estudiante.getNotas().isEmpty()) return List.of();
        double promedio = estudiante.calcularPromedio().orElseThrow();
        resultado.add("Primer despegue");
        if (promedio >= 4.0) resultado.add("Órbita de excelencia");
        if (promedio >= 4.5) resultado.add("Cerebrito UCO");
        var codigo = estudiante.getNotas().stream().filter(n -> n.materia().equals("Programación Orientada a Objetos")).toList();
        if (codigo.size() >= 3 && codigo.stream().mapToDouble(Nota::valor).average().orElse(0) >= 4.5)
            resultado.add("Master Code");
        return List.copyOf(resultado);
    }
}
