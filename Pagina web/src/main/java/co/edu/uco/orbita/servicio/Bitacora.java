package co.edu.uco.orbita.servicio;
import java.util.List;
/** Puerto pequeño para desacoplar el registro de acciones. */
public interface Bitacora {
    void registrar(String mensaje);
    List<String> consultar();
}
