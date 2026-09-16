class Profesor extends Persona{ // Esta es una clase hija la cual heredara los atributos de Persona 
    private int contraseña ; 
    private String asignatura;// Variable contraseña
    public Profesor(int id , String nombre, int contraseña, String asignatura){// Metodo constructor el cual esperara los datos y asignara esos datos alas variables privadas de la clase 
        super(id,nombre);// metodo super pára cuando se inice envie los datos a la clase Persona sin esto el codigo dara error
        this.contraseña = contraseña ;// Asignamos los valores de las varibales privadas con el de las entrantes
        this.asignatura = asignatura ;
        
    }
    //Getters para obtener los valores de cada objeto cuando se requieran
    public int getContraseña(){
        return this.contraseña;
    }
    public String getAsignatura(){
        return this.asignatura;
    }
}

