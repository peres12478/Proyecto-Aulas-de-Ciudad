package co.edu.uco.orbita;
import co.edu.uco.orbita.modelo.*;
import co.edu.uco.orbita.servicio.*;

/** Pruebas ejecutables sin dependencias, incluyendo permisos y valores límite. */
public final class Pruebas {
    private static int total;
    private static void comprobar(boolean valor, String mensaje) {
        total++;
        if (!valor) throw new AssertionError(mensaje);
    }
    private static void rechaza(Class<? extends Throwable> tipo, Runnable accion) {
        total++;
        try { accion.run(); }
        catch (Throwable ex) { if (tipo.isInstance(ex)) return; throw new AssertionError("Excepción inesperada", ex); }
        throw new AssertionError("Debía fallar con " + tipo.getSimpleName());
    }
    public static void main(String[] args) {
        var estudiante = new Estudiante("E1", "Ana", "ana@uco.demo", "Orbita2026!", "Sistemas");
        var otro = new Estudiante("E2", "Leo", "leo@uco.demo", "Orbita2026!", "Sistemas");
        var profesor = new Profesor("P1", "Docente", "docente@uco.demo", "Orbita2026!", "Software", "Programación Orientada a Objetos");
        var gestor = new GestorAcademico(new BitacoraMemoria(), new InsigniasOrbita());
        gestor.registrar(estudiante);gestor.registrar(otro);gestor.registrar(profesor);
        comprobar(estudiante.calcularPromedio().isEmpty(), "Sin notas debe ser vacío");
        comprobar(new InsigniasOrbita().evaluar(estudiante).isEmpty(), "Sin insignias iniciales");
        comprobar(estudiante.autenticar("Orbita2026!"), "Password correcto");
        comprobar(!estudiante.autenticar("incorrecta"), "Password incorrecto");
        rechaza(IllegalArgumentException.class, () -> gestor.registrar(estudiante));
        rechaza(SecurityException.class, () -> gestor.asignarNota("E1", "A", 4));
        gestor.login("ana@uco.demo", "Orbita2026!");
        rechaza(SecurityException.class, () -> gestor.asignarNota("E1", "A", 4));
        rechaza(SecurityException.class, () -> gestor.consultarEstudiante("E2"));
        rechaza(SecurityException.class, gestor::historial);
        gestor.login("docente@uco.demo", "Orbita2026!");
        for (double valor : new double[]{-1, 5.1, Double.NaN, Double.POSITIVE_INFINITY})
            rechaza(IllegalArgumentException.class, () -> gestor.asignarNota("E1", "Inválida", valor));
        gestor.asignarNota("E1", "A", 4.5);gestor.asignarNota("E1", "B", 4.5);gestor.asignarNota("E1", "C", 4.5);
        comprobar(estudiante.calcularPromedio().orElseThrow()==4.5, "Promedio exacto");
        comprobar(gestor.insignias("E1").size()==4, "Umbral 4.5 y tres notas");
        rechaza(IllegalArgumentException.class, () -> gestor.asignarNota("E1", " a ", 3));
        rechaza(UnsupportedOperationException.class, () -> estudiante.getNotas().clear());
        gestor.asignarNota("E2", "Cero", 0);gestor.asignarNota("E2", "Cinco", 5);
        comprobar(otro.calcularPromedio().orElseThrow()==2.5, "Extremos incluidos");
        gestor.asignarNota("E1", "D", 0);
        comprobar(gestor.insignias("E1").size()==1, "Insignias se recalculan al bajar promedio");
        comprobar(gestor.historial().stream().noneMatch(s->s.contains("Orbita2026!")), "No filtrar secretos");
        rechaza(SecurityException.class, () -> gestor.login("docente@uco.demo", "fallo"));
        rechaza(SecurityException.class, gestor::listarEstudiantes);
        gestor.login("docente@uco.demo", "Orbita2026!");gestor.logout();
        rechaza(SecurityException.class, gestor::listarEstudiantes);
        System.out.println("OK · " + total + " verificaciones de dominio y autorización");
    }
}
