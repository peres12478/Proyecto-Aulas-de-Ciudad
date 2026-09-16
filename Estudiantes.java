import java.util.HashMap;// Importamos libreria para diccionarios para asi guardar cada nota con su respctiva materia
import java.util.Map;
public class Estudiantes extends Persona { // le damos los atributos de Persona
    private int contraseña;
    private Map<String, Double> notas ;// Creamos el diccionario
    //Constructor
    public Estudiantes(int id , String nombre , int contraseña, String materia , Double nota){
        notas = new HashMap<>();// inicializamos el diccionario
        super(id, nombre);// metodo super pára cuando se inice envie los datos a la clase Persona sin esto el codigo dara error
        this.contraseña = contraseña ;
        notas.put(materia,nota);// Agregamos lo valores al diccionario
    }
    public int getcontraseña(){// Getter para obtener la contraseña
        return this.contraseña;
    }
    public void imprimir_notas(){// Metodo para imprimir todas las notas
        for (Map.Entry<String, Double> notas : notas.entrySet()) {
            System.out.println("Materia: " + notas.getKey() + " | Nota: " + notas.getValue());
        }
    }
    //metodo para agregar/cambiar la nota
    public void agregar_nota(String materia , Double nota){
        notas.put(materia,nota);
    }
    
}
