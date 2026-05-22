import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;


public class ServidorSalon extends Thread {

    private final String nombre;
    private final int puerto;
    private final ServidorCentral servidorCentral;
    private final Persistencia persistencia = new Persistencia();
    private final Logger logger;

    // Lista de clientes conectados en este salon ahora mismo
    private final List<String> nombresEnSalon = new ArrayList<>();
    private final List<ManejadorSalon> manejadoresEnSalon = new ArrayList<>();

    public ServidorSalon(String nombre, int puerto, ServidorCentral servidorCentral) {
        this.nombre = nombre;
        this.puerto = puerto;
        this.servidorCentral = servidorCentral;
        this.logger = new Logger("Salon-" + nombre + ":" + puerto, "salon_" + nombre + ".log");
        setName("Hilo Salon-" + nombre);
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(puerto);
            logger.info("Salon '" + nombre + "' escuchando en puerto " + puerto);

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                ManejadorSalon manejador = new ManejadorSalon(clienteSocket, this);
                manejador.start();
            }
        } catch (IOException e) {
            logger.error("Error en ServerSocket del salon " + nombre + ": " + e.getMessage());
        }
    }

    public synchronized void añadirCliente(String nombre, ManejadorSalon manejador) {
        nombresEnSalon.add(nombre);
        manejadoresEnSalon.add(manejador);
    }

    //Elimina un cliente del salon
    public synchronized void eliminarCliente(String nombre) {
        int indice = nombresEnSalon.indexOf(nombre);
        if (indice != -1) {
            nombresEnSalon.remove(indice);
            manejadoresEnSalon.remove(indice);
        }
    }
    
    //Comprobaciones del salon
    public boolean estaEnSalon(String nombre) {
        return nombresEnSalon.contains(nombre);
    }
    public boolean estaLleno() {
        return nombresEnSalon.size() >= Protocolo.MAX_CLIENTES_SALON;
    }
    public List<String> getUsuariosConectados() {
        return new ArrayList<>(nombresEnSalon);
    }
    public void mensajeATodos(String mensaje) {
        for (ManejadorSalon m : manejadoresEnSalon) {
            m.enviar(mensaje);
        }
    }
    //Mensaje a todos excepto a uno (Notificacion de entrada/salida en el salon)
    public void notificacion(String excluido, String mensaje) {
        for (int i = 0; i < nombresEnSalon.size(); i++) {
            if (!nombresEnSalon.get(i).equals(excluido)) {
                manejadoresEnSalon.get(i).enviar(mensaje);
            }
        }
    }

    public String getNombre(){
        return nombre; 
    }
    public int getPuerto(){
        return puerto; 
    }
    public ServidorCentral getServidorCentral(){ 
        return servidorCentral; 
    }
    public Persistencia getPersistencia(){ 
        return persistencia; 
    }
        
}