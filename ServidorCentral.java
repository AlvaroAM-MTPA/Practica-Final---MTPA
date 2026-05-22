import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class ServidorCentral {

    private final Logger logger = new Logger("Servidor-Central", "servidor_central.log");
    private final Persistencia persistencia = new Persistencia();

    private final List<Usuario> usuarios = new ArrayList<>(); // Usuarios registrados
    private final List<ManejadorCentral> conectados = new ArrayList<>(); // Manejadores centrales conectados

    // Flags de control del servidor
    private volatile boolean aceptandoClientes = true;
    private volatile boolean mensajesActivos = true;

    //Listas de los salones
    private final List<String> nombresSalones = new ArrayList<>();
    private final List<Integer> mensajesSalones = new ArrayList<>();

    public static void main(String[] args) {
        new ServidorCentral().arrancar();
    }

    public void arrancar() {
        //Usuarios del fichero
        usuarios.addAll(persistencia.cargarUsuarios());
        logger.info("Sistema iniciado. Usuarios en base de datos: " + usuarios.size());

        //Contadores de mensajes por salón
        for (Protocolo.Salon salon : Protocolo.SALONES) {
            nombresSalones.add(salon.nombre);
            mensajesSalones.add(0);
        }
        
        // Arrancamos los 5 servidores de salón en hilos independientes
        for (Protocolo.Salon salon : Protocolo.SALONES) {
            ServidorSalon servidorSalon = new ServidorSalon(salon.nombre, salon.puerto, this);
            servidorSalon.start();
        }

        //Heartbeat
        Thread hiloHeartbeat = new Thread(new Runnable() {
            public void run() {
                hiloHeartbeats();
            }
        });
        hiloHeartbeat.setName("Hilo Heartbeat");
        hiloHeartbeat.start();

        //Hilo consola de administración
        Thread hiloAdmin = new Thread(new Runnable() {
            public void run() {
                hiloAdmin();
            }
        });
        hiloAdmin.setName("Hilo Admin");
        hiloAdmin.start();

        try  {
            ServerSocket serverSocket = new ServerSocket(Protocolo.PUERTO_CENTRAL);
            logger.info("Servidor Central escuchando en puerto " + Protocolo.PUERTO_CENTRAL);
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                if (!aceptandoClientes) {
                    PrintWriter pw = new PrintWriter(clienteSocket.getOutputStream(), true);
                    pw.println(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_CONEXION_NO_PERMITIDA);
                    clienteSocket.close();
                    logger.aviso("Conexión rechazada");
                    continue;
                }
                ManejadorCentral manejador = new ManejadorCentral(clienteSocket, this);
                manejador.start();
            }
        } catch (IOException e) {
            logger.error("Error en ServerSocket: " + e.getMessage());
        }
    }

    
    private void hiloHeartbeats() {
    while (true) {
        try {
            Thread.sleep(Protocolo.HEARTBEAT_INTERVALO * 1000);
        } catch (InterruptedException e) {
            break;
        }
        long ahora = System.currentTimeMillis();
        long limite = Protocolo.HEARTBEAT_TIMEOUT * 1000;
        List<ManejadorCentral> copia = new ArrayList<>(conectados);
        for (ManejadorCentral manejador : copia) {
            if (ahora - manejador.getUltimoHeartbeat() > limite) {
                logger.aviso("Heartbeat perdido de " + manejador.getNombreUsuario() + ", desconectando.");
                manejador.enviar(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_SESION_EXPIRADA);
                desconectarUsuario(manejador.getNombreUsuario());
            }
        }
    }
}   

    //
    private void hiloAdmin() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Consola de administración del Servidor Central");
        System.out.println("Comandos: STOP_CLIENTES, CONTINUAR_CLIENTES, STOP_MENSAJES, CONTINUAR_MENSAJES, ESTADO_USUARIOS, ESTADO_SALON, ESTADO_TODO");

        while (scanner.hasNextLine()) {
            String comando = scanner.nextLine().trim().toUpperCase();
            if (comando.equals (Protocolo.STOP_CLIENTES)) {
                aceptandoClientes = false;
                System.out.println("[ADMIN] Servidor Central ha dejado de aceptar nuevos clientes.");
            } else if (comando.equals(Protocolo.CONTINUAR_CLIENTES)) {
                aceptandoClientes = true;
                System.out.println("[ADMIN] Servidor Central ha reanudado la aceptación de nuevos clientes.");
            } else if (comando.equals(Protocolo.STOP_MENSAJES)) {
                mensajesActivos = false;
                System.out.println("[ADMIN] Servidor Central ha desactivado el envío de mensajes.");
            } else if (comando.equals(Protocolo.CONTINUAR_MENSAJES)) {
                mensajesActivos = true;
                System.out.println("[ADMIN] Servidor Central ha reactivado el envío de mensajes.");
            } else if (comando.equals(Protocolo.ESTADO_USUARIOS)) {
                System.out.println("[ADMIN] Usuarios conectados: " + conectados.size());
                for (ManejadorCentral manejador : conectados) {
                    System.out.println("[ADMIN] - " + manejador.getNombreUsuario());
                }
            } else if (comando.equals(Protocolo.ESTADO_SALON)) {
                System.out.println("[ADMIN] Mensajes por salón:");
                for (int i = 0; i < nombresSalones.size(); i++) {
                    System.out.println("[ADMIN] Salón " + nombresSalones.get(i) + ": " + mensajesSalones.get(i) + " mensajes");
                }
            } else if (comando.equals(Protocolo.ESTADO_TODO)) {
                System.out.println("[ADMIN] Estado completo del servidor:");
                System.out.println("[ADMIN] - Usuarios conectados: " + conectados.size());
                for (ManejadorCentral manejador : conectados) {
                    System.out.println("[ADMIN]   - " + manejador.getNombreUsuario());
                }
                System.out.println("[ADMIN] - Mensajes por salón:");
                for (int i = 0; i < nombresSalones.size(); i++) {
                    System.out.println("[ADMIN] - Salón " + nombresSalones.get(i) + ": " + mensajesSalones.get(i) + " mensajes");
                }
            } else {
                System.out.println("Comando desconocido.");
            }
        }
    }

    //Buscar usuario en una lista
    private Usuario buscarUsuario(String nombre) {
        for (Usuario user : usuarios) {
            if (user.getNombre().equals(nombre)) {
                return user;
            }
        }
        return null;
    }

    //Comprobaciones de usuario
    public boolean existeUsuario(String nombre) {
        return buscarUsuario(nombre) != null;
    }
    public boolean claveCorrecta(String nombre, String clave) {
        Usuario user = buscarUsuario(nombre);
        return user != null && user.getClave().equals(clave);
    }
    public boolean estaConectado(String nombre) {
        for (ManejadorCentral manejador : conectados) {
            if (manejador.getNombreUsuario().equals(nombre)) {
                return true;
            }
        }
        return false;
    }
    public boolean aceptandoClientes() {
        return aceptandoClientes;
    }
    public boolean mensajesActivos() {
        return mensajesActivos;
    }

    //Gestión de usuarios
    public String registrarUsuario(String nombre) {
        String clave = persistencia.generarClave(usuarios);
        Usuario user = new Usuario(nombre, clave);
        usuarios.add(user);
        persistencia.añadirUsuario(user);
        return clave;
    }
    public synchronized void conectarUsuario(String nombre, ManejadorCentral manejador) {
        conectados.add(manejador);
    }
    
    public synchronized void desconectarUsuario(String nombre) {
        ManejadorCentral aEliminar = null;
        for (ManejadorCentral manejador : conectados) {
            if (manejador.getNombreUsuario().equals(nombre)) {
                aEliminar = manejador;
                break;
            }
        }
        if (aEliminar != null) {
            conectados.remove(aEliminar);
        }
    }

    public void actualizarUltimoLogin(String nombre, String tiempo) {
        Usuario user = buscarUsuario(nombre);
        if (user != null) {
            user.setUltimoLogin(tiempo);
            persistencia.guardarUsuarios(usuarios);
        }
    }

    public List<String> getUsuariosConectados() {
        List<String> lista = new ArrayList<>();
        for (ManejadorCentral manejador : conectados) {
            lista.add(manejador.getNombreUsuario());
        }
        return lista;
    }

    public void enviarAUsuario(String nombre, String mensaje) {
        for (ManejadorCentral manejador : conectados) {
            if (nombre.equals(manejador.getNombreUsuario())) {
                manejador.enviar(mensaje);
                break;
            }
        }
    }

    public void difundirATodos(String mensaje) {
        for (ManejadorCentral manejador : conectados) {
            manejador.enviar(mensaje);
        }
    }

    public void incrementarMensajesSalon (String nombreSalon){
        for (int i = 0; i < nombresSalones.size(); i++) {
            if (nombresSalones.get(i).equals(nombreSalon)) {
                mensajesSalones.set(i, mensajesSalones.get(i) + 1);
                break;
            }
        }
    }
}
