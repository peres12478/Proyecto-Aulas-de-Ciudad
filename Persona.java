public class Persona { // Clase padre ¿Por que? , porque tanto Profesor como Estudiantes comparten estas caracteristicas Id y nombre para asi no tener que repetir codigo inecesario
    private int id ;
    private String nombre ;
    // Constructor
    public Persona(int id , String nombre){
        // Aqui le decimos ala funcion , te van a entrar unos datos , de esos datos vas a ser unas asignaciones con las variables del programa
        this.id = id ;
        this.nombre = nombre ;
    }
    //Getters para obtener los valores de cada objeto cuando se requieran
    public int getid(){
        return this.id ;
    }
    public String getnombre(){
        return this.nombre ;
    }
}
