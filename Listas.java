import java.util.ArrayList;
// Aqui estamos creando la lista de donde se va a almacenar cada objeto Tipo Profesor
class Listas{
    private Profesor profesor_activo ;
    private Estudiantes estudiante_activo ;
    private ArrayList<Estudiantes> listaEstudiantes;// Array para los estudiantes
    // Se crea el array donde se guardaran el objeto tipo profesor
    private ArrayList<Profesor> listaProfesores;// Array para los estudiantes
    //Se crea el constructor para asi cuando lo llamemos en el codigo cree el array
    public Listas() {
        this.listaProfesores = new ArrayList<>() ;
        this.listaEstudiantes = new ArrayList<>();
    }
    // Con esta funcion se va guardar el objeto tipo Profesor , ¿Por que profesor y no persona?, por que el profesor comparte las mismas caracteristicas que el estudiante y para simplificar el codigo , la vamos a guardar con tipo profesor 
    public void agregar_profe(Profesor persona){
        this.listaProfesores.add(persona);
    }
    public void mostrar(){// Aqui imprimos la verificacion de los datos es decir imprimir los profesores
        for (Profesor profe : listaProfesores) {
            System.out.println("ID: "  + profe.getid());
            System.out.println("Nombre: " + profe.getnombre());
            System.out.println("Contraseña: " + profe.getContraseña());
            System.out.println("Asignatura: " + profe.getAsignatura());
        }
    }
    public boolean comprobar(){// Aqui comprobamos si la lista de profesores esta vacia o no
        boolean x ;
        if (listaProfesores.isEmpty()){
            x = false ;
        }else{
            x = true ;
        }
        return x ;
    }
    public  boolean comprobar_datos(int id , int contraseña){// Aqui comprobamos los datos para asi verificar si fueran ingresados correctamente
        boolean x = false , y  = false ;
        boolean comprobador = false;
        for (Profesor profesor : listaProfesores) {//Un foreach para asi hacer que pase por cada tipo de la listaprofesores
            if (profesor.getid() == id ){
                this.profesor_activo = profesor ;
                x = true ;
            }
            if (profesor.getContraseña() == contraseña){
                y = true ;
            }
        }
        if (x == true && y == true){// Si el Id y la contraseña fueron ingresados correctamente devuelve True
            comprobador = true;
        }
        return comprobador ;// returna el booleano
    }
    //Getter para obtener el objeto en que esta parado profesor es decir en que objeto se encontraron esos ID y COntraseña
    public Profesor getProfesor_activo(){
        return this.profesor_activo ;
    }
    //hasta aqui fue todo de profesores apartir de aqui sera para estudiantes
    public void agregar_estudiante(Estudiantes estudinate){
        this.listaEstudiantes.add(estudinate);//Agregamos el objeto ala lista
    }
    public void imprimir(){// Aqui esta el metodo para imprimir todos los atributos del estudiante 
        for (Estudiantes estudiantes : listaEstudiantes) {
            System.out.println(estudiantes.getnombre());
            System.out.println(estudiantes.getid());
            System.out.println(estudiantes.getcontraseña());
        }
    }
    public ArrayList<Estudiantes> getListaEstui(){ // Este metodo esta siendo utilizado para obtener los valores de esa lista en otra clase
        return this.listaEstudiantes;
    }
    public  boolean verificar(int id , int contra){ // Verificamos los datos
        boolean x = false ;
        for (Estudiantes estudiante : listaEstudiantes) {
            if(estudiante.getid() == id && estudiante.getcontraseña() == contra ){
                x = true ;
                this.estudiante_activo = estudiante ;
            }
        }
        return x ;
    }
    public Estudiantes getestudiante_activo(){//Encontramos el objeto donde esta la informacion
        return this.estudiante_activo;
    }
}
