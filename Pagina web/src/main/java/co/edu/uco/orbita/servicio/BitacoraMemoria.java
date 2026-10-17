package co.edu.uco.orbita.servicio;
import java.time.LocalDateTime;
import java.util.*;
/** Historial inmutable para el consumidor; nunca registra contraseñas. */
public final class BitacoraMemoria implements Bitacora {
    private final List<String> eventos = new ArrayList<>();
    @Override public void registrar(String mensaje) { eventos.add(LocalDateTime.now() + " · " + mensaje); }
    @Override public List<String> consultar() { return List.copyOf(eventos); }
}
