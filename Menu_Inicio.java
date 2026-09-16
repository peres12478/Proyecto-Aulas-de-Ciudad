import java.util.Scanner;
public class Menu_Inicio {
    
    static Scanner scanner = new Scanner(System.in); // inicializamos el scanner
    public static int Menu_inicio(){// creamos un metodo retornable para asi encontrar que quiere el usuario si iniciar sesion o crear usuario
        
        System.out.println("Bienvenido elije una opcion 1 : Iniciar sesion 2 : Crear Usuario");
        int x = scanner.nextInt();
        return x ;
    }
    // Este es el menu temporal para la terminal , esto para los profesores
    public static void Menu_Profesor(){
        while (true) {
            Listas lista = Sistema.getlista();// Obtenemos la variable lista
            Profesor profesor = lista.getProfesor_activo();// Obtenemos que profesor/ objeto de tipo profesor esta activo 
            System.out.println("Bienvenido "+ profesor.getnombre());//imprimimos
            System.out.println("1 agregar estudiante y Asignar la nota\n2 Modificar nota/Cambiar\n3 Salir del programa ");
            int x = scanner.nextInt();// Con esto pedimos que quiere hacer el usuario segun lo que necesite luego comparamos y hacemos la respctiva accion
            if( x == 1 ){
                lista.agregar_estudiante(new Estudiantes(Sistema.id(), Sistema.nombre(),Sistema.contraseña(),profesor.getAsignatura(),Sistema.asignarNota()));
                // agregamos al estudiante segun los datos ingresados por el profesor
                System.out.println("Cuidado la contraseña asignada debe ser obligatoriamente dada al estudiante lo mas pronto posible");
                
            }else if (x == 2){
                if(comprobarestudiante()){// llamamos al metodo comprobarestudiante el cual es un booleano retornable
                    System.out.println("Ingresa tu identificacion presiona ");//pedimos datos del usuario
                    int id = scanner.nextInt();
                    System.out.println("Ingresa la contraseña del estudiante");
                    int y = scanner.nextInt();
                    if(lista.verificar(id, y)){// verificamos si la informacion es correcta
                        Estudiantes estudiante = lista.getestudiante_activo();//obtenemos en que estudiante / objeto estan esos datos
                        System.out.println("Estudiante :"+ estudiante.getnombre());
                        double nota = scanner.nextDouble();
                        System.out.println("Asigne la nota/Cambiar");
                        estudiante.agregar_nota(profesor.getAsignatura(), nota);// llamamos ala funcion agregar nota para asi modificarla    
                        System.out.println("Nota asignada");
                    }else{
                        System.out.println("Datos no encontrados/ Ingresados Incorrectamente");
                    }
                }
            }else {
                break ;
            }
        }
        
    }
    //Menu de los estudiantes
    public static boolean comprobarestudiante(){
        Listas lista = Sistema.getlista(); // obtenemos la lista
        boolean x = false ;
        if(!lista.getListaEstui().isEmpty()){ // la ponemos ¡ para decir , si esta vacia es decir devuelve un True vuelva False y asi al contrario si es False = True
            x = true ;
            System.out.println("Estudiante Encontrado");
        }else{
            System.out.println("No hay estudiantes registrados");
        }
        return x ;
    }
    public static void MenuEstudiante(){// Menu temporal de estudiantes
        if(comprobarestudiante()){// comprobamos si hay objetos en estudiantes
            while(true){// bucle para se repita mas de una vez
                Listas lista = Sistema.getlista();
                System.out.println("Ingresa tu identificacion presiona 3 para salir");
                int x = scanner.nextInt();
                if (x == 3){
                    break ;// si presiona 3 sale de este apartado
                }
                System.out.println("Ingresa la contraseña proporcionada por tu profesor");
                int y = scanner.nextInt();
                if(lista.verificar(x, y)){// verificamos los datos
                    Estudiantes estudiante = lista.getestudiante_activo();//obtenemos en que estudiante / objeto estan esos datos
                    estudiante.imprimir_notas();// imprimimos las notas 
                }else{
                    System.out.println("Nombre / Contraseña ingresadas incorrectamente");
                }
            }
        }
        
    }
}
