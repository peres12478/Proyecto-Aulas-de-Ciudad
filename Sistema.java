import java.util.Scanner; // llamamos ala libreria scanner para poder acceder ala informacion por medio de terminal es decir leer entradas
public class Sistema{
    // Inicializamos un scanner para poder leer las entradas desde la terminal la pongo static por que si no es static no me deja ser accesible desde otras partes
    static Scanner scanner = new Scanner(System.in);
    //Ponemos la lista como una variable static para cuando el programa inicie se ejecute y ademas privada para que sea accesible en todo el codigo
    private static Listas lista = new Listas();
    public static void main(String[] args){
        // Con esta creamos un objeto en este codigo llamado lista donde ser por asi decirlo una llave para acceder a los metodos de la clase listas
        
        // Un bucle para asi que el codigo se repita siempre que pueda hasta que el usuario decida salirse
        while(true){
            //Llamamos al metodo Menu_inicio donde esta la entrada del usuario para asi saber que quiere hacer durante el codigo
            if (Menu_Inicio.Menu_inicio() == 1){
                System.out.println("¿Quien va a iniciar sesion? , 1 : Profesor , 2 : Estudiante");
                //Pedimos la entradapara saber quien quiere iniciar sesion
                int x = scanner.nextInt();
                //comprobamos esa entrada
                if (x == 1){
                    // llamamos al metodo comprobar para asi verificar si la lista de profesores esta vacia para mas informacion dirijirse a la clase Listas.java
                    if(lista.comprobar()){
                        // Aqui vamos a pedir los datos del profesor para iniciar sesion
                        System.out.println("Ingresa numero de documento");
                        int id = scanner.nextInt();
                        System.out.println("Ingresa tu contraseña");
                        int contra = scanner.nextInt();
                        // llamamos al metodo comprobar_datos mandandole las entradas de id(id) y contraseña(contra) para que el metodo las verifique y devuelva un valor verdadero / Falso 
                        if(lista.comprobar_datos(id,contra)){
                            System.out.println("Registro exitoso");
                            // Aqui llamamos al metodo Menu_profesor donde se ejecutara toda la logica de las acciones a realizar por parte del profesor
                            Menu_Inicio.Menu_Profesor();
                        }else{
                            System.out.println("Fallo al iniciar sesion porfavor intentalo mas tarde");
                        }
                    }else {
                        System.out.println("No hay ningun registro de profesores porfavor crear uno");
                    }
                }else{
                    //Estudiantes
                    //Aqui llamamos al metodo MenuEstudiante donde se ejecutara toda la logica de las acciones a realizar por parte del Estudiante
                    Menu_Inicio.MenuEstudiante();
                } 
            }else{
                // Llamamos ala variable retornable  Crea_Usuario y el valor que aroja la guardamos en x
                int x = Crear_Usuario() ;
        
                // Comparamos el valor de x para asi ejecutar la funcion / accion que el usuario queri hacer
                if( x == 1 ){
                    // Como decidio crear profesor ya que tal como se le aviso en Crea_Usuario() 2 es para profesor aqui pedimos los datos respectivos lalmando a la respectiva funcion y los enviamos a la clase lista "Abajo continua"
                    // donde estara guardando el valor que le enviemos como tipo Porfesor , ¿Por que Profesor? por que profesor es una clase hija de la clase persona ya con eso añadimos los atributos tanto de persona y profesor 
                    lista.agregar_profe(new Profesor(id(), nombre(),contraseña(),asignatura()));
                    lista.mostrar(); // Llamamos al metodo mostrar de la clase lista "En la clase lista se explica el funcionamiento"
                }else { // Como el sistema no detecto ni 1 o 2 va a asumir que escribio 3 por lo tanto con ese break ronpemos el bucle por lo tanto se acaba el codigo
                 break ;
                }
        
            }
        }
    }
    //Creamos este metodo para asi poder acceder ala varibale lista ya que no podemos repetir new lista ya que solo existe 1 lista en la memoria
    public static Listas getlista(){
        return lista ;
    }
    private static int Crear_Usuario(){
        // Inicializamos pidiendole que quiere realizar
        System.out.println("Bienvenido al sistema de creacion de usuarios que desea crear hoy \n 1 Crear Profesor\n 2 Salir del programa \n dd la creacion de estudiantes solo esta disponible para los docentes siempre y cuando hayan iniciado sesion" );
        // Llamamos al scanner para detectar la entrada del usuario y lo guardamos en una varibale x donde debe ser entero ya que el scanner va a detectar un int y la funcion retornable espera un int
        int x = scanner.nextInt(); 
        return x ;// Retornamos la entrada del usuario
    }
    public static int id(){ // Con esta funcion le pedimos el número de identificacion retornando asi la variable x
        //pdd : veras que en la mayoria se va a retornar x eso es para tener mayor control y ademas no llenar esto con muchos nombres de varibales volviendolo confuso ademas como son locales de la funcion no afecta
        System.out.println("Ingresa tu numero de identificacion");
        int x = scanner.nextInt();
        scanner.nextLine();
        return x ;
        
    }    
    public static String nombre(){// Con esta funcion le pedimos el nombre retornando asi la variable x
        System.out.println("Ingresa tu nombre de usuario");
        String x = scanner.nextLine();
        return x ;
    }
    public static int contraseña(){// Con esta funcion le pedimos la contraseña retornando asi la variable x
        System.out.println("Ingresa tu contraseña (numerica)");
        int x = scanner.nextInt();
        scanner.nextLine();
        return x ;
    }
    private static String asignatura(){// Con esta funcion le pedimos la Asignatura retornando asi la variable x
        System.out.println("Ingresa la asignatura ala que asistes");
        String x = scanner.nextLine();
        return x ;
    }
    // Asignar nota estudiante
    public static double asignarNota(){
        System.out.println("Dime la nota que vas a asignar");
        double x = scanner.nextDouble();
        return x ;
    }
}