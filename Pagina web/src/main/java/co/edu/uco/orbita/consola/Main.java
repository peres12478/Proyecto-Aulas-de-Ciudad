package co.edu.uco.orbita.consola;
import co.edu.uco.orbita.modelo.*;
import co.edu.uco.orbita.servicio.*;
import java.util.*;

/** Adaptador de consola. Ejecutar con Java 17 o superior. */
public final class Main {
    private final Scanner entrada = new Scanner(System.in);
    private final GestorAcademico gestor = new GestorAcademico(new BitacoraMemoria(), new InsigniasOrbita());
    private final boolean color = System.console() != null && System.getenv("NO_COLOR") == null;
    /** Punto de entrada de la demostración de consola. */
    public static void main(String[] args) { new Main().ejecutar(); }
    private String leer(String etiqueta) { System.out.print(etiqueta); return entrada.nextLine().trim(); }
    private void mensaje(String texto) { System.out.println((color ? "\u001b[36m" : "") + texto + (color ? "\u001b[0m" : "")); }
    private void ejecutar() {
        gestor.registrar(new Estudiante("E01", "Valentina Ríos", "valentina@uco.demo", "Orbita2026!", "Ingeniería de Sistemas"));
        gestor.registrar(new Estudiante("E02", "Samuel Gómez", "samuel@uco.demo", "Orbita2026!", "Ingeniería de Sistemas"));
        gestor.registrar(new Estudiante("E03", "Luciana Vélez", "luciana@uco.demo", "Orbita2026!", "Ingeniería de Sistemas"));
        gestor.registrar(new Profesor("P01", "Andrés Moreno", "andres@uco.demo", "Orbita2026!", "Ingeniería de software", "Programación Orientada a Objetos"));
        gestor.login("andres@uco.demo", "Orbita2026!");
        gestor.asignarNota("E01", "Laboratorio de clases", 4.7);
        gestor.asignarNota("E01", "Reto de herencia", 4.4);
        gestor.asignarNota("E01", "Proyecto SOLID", 4.8);
        gestor.logout();
        mensaje("ÓRBITA UCO · Demo académica | contraseña: Orbita2026!");
        while (entrada.hasNextLine()) {
            try {
                String opcion = leer("1. Ingresar  0. Salir > ");
                if (opcion.equals("0")) break;
                if (!opcion.equals("1")) { mensaje("Opción inválida"); continue; }
                String correo = leer("Correo (@uco.demo): ");
                String password = System.console() == null ? leer("Contraseña: ") : new String(System.console().readPassword("Contraseña: "));
                Persona persona = gestor.login(correo, password);
                mensaje("Hola, " + persona.getNombre() + " · " + persona.getRol());
                menu(persona);
            } catch (IllegalArgumentException | SecurityException ex) { mensaje(ex.getMessage()); }
        }
    }
    private void menu(Persona persona) {
        while (entrada.hasNextLine()) {
            String opcion = leer(persona instanceof Profesor
                ? "1. Estudiantes  2. Asignar nota  3. Historial  0. Cerrar sesión > "
                : "1. Mis notas e insignias  0. Cerrar sesión > ");
            if (opcion.equals("0")) { gestor.logout(); return; }
            try {
                if (persona instanceof Profesor) {
                    switch (opcion) {
                        case "1" -> gestor.listarEstudiantes().forEach(this::mostrar);
                        case "2" -> gestor.asignarNota(leer("ID estudiante: "), leer("Actividad: "), Double.parseDouble(leer("Nota 0–5: ").replace(',', '.')));
                        case "3" -> gestor.historial().forEach(this::mensaje);
                        default -> mensaje("Opción inválida");
                    }
                } else if (opcion.equals("1")) mostrar(gestor.consultarEstudiante(persona.getId()));
                else mensaje("Opción inválida");
            } catch (IllegalArgumentException | SecurityException ex) { mensaje(ex.getMessage()); }
        }
        gestor.logout();
    }
    private void mostrar(Estudiante estudiante) {
        mensaje(estudiante.getId() + " · " + estudiante.getNombre());
        estudiante.getNotas().forEach(n -> System.out.println(n.actividad() + " | " + n.materia() + " | " + n.valor()));
        mensaje("Promedio: " + (estudiante.calcularPromedio().isPresent() ? String.format(Locale.ROOT, "%.2f", estudiante.calcularPromedio().getAsDouble()) : "Sin notas"));
        mensaje("Insignias: " + gestor.insignias(estudiante.getId()));
    }
}
