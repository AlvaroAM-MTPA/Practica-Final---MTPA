import java.io.*;
import java.time.LocalDateTime;

public class Logger {

    //Niveles de log
    public static final String INFO  = "INFO";
    public static final String AVISO = "AVISO";
    public static final String ERROR = "ERROR";

    private final String usuario;          
    private PrintWriter fichero; 
    
    //Constructor por consola
    public Logger(String usuario) {
        this.usuario = usuario;
        this.fichero = null;
    }
    //Constructor por fichero
    public Logger(String usuario, String rutaFichero) {
        this.usuario = usuario;
        try {
            this.fichero = new PrintWriter(new FileWriter(rutaFichero, true), true);
        } catch (IOException e) {
            System.out.println("Error al abrir el fichero de log: " + e.getMessage());
        }
    }

    //Metodo de registro prinicipal
    public synchronized void registrar(String nivel, String mensaje) {
        String linea = "[" + obtenerFecha() + "] [" + nivel + "] [" + usuario + "] " + mensaje;
        System.out.println(linea);
        if (fichero != null) {
            fichero.println(linea);
        }
    }
    private String obtenerFecha() {
    LocalDateTime tiempo = LocalDateTime.now();
    String dia = String.format("%02d", tiempo.getDayOfMonth());
    String mes = String.format("%02d", tiempo.getMonthValue());
    String ano = String.valueOf(tiempo.getYear());
    String hora = String.format("%02d", tiempo.getHour());
    String minuto = String.format("%02d", tiempo.getMinute());
    String segundo = String.format("%02d", tiempo.getSecond());
    return dia + "/" + mes + "/" + ano + " " + hora + ":" + minuto + ":" + segundo;
}

    public void info(String mensaje)  { 
        registrar(INFO,  mensaje); }
    public void aviso(String mensaje) { 
        registrar(AVISO, mensaje); }
    public void error(String mensaje) { 
        registrar(ERROR, mensaje); }

    
    public void cerrar() {
        if (fichero != null) {
            fichero.close();
        }
    }
}
