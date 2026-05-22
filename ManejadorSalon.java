import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;


public class ManejadorSalon extends Thread {

    private final Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;          
    private final ServidorSalon salon;
    private final Logger logger;

    public ManejadorSalon(Socket socket, ServidorSalon salon) {
        this.socket = socket;
        this.salon  = salon;
        this.logger = new Logger("Salon-" + salon.getNombre() + ":" + salon.getPuerto());

        try {
            this.entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            logger.error("Error abriendo streams: " + e.getMessage());
        }
    }

    public void run() {
        try {
            logger.info("Nueva conexion al salon " + salon.getNombre());
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    procesarComando(linea.trim());
                }
            }
        } catch (IOException e) {
            String info = "Conexion cerrada inesperadamente en salon " + salon.getNombre();
            if (nombreUsuario != null) {
                info = info + " (usuario: " + nombreUsuario + ")";
            }
            logger.aviso(info + ": " + e.getMessage());
        } finally {
            cerrarSesion();
        }
    }

    private void procesarComando(String linea) {
        String[] partes = linea.split(" ", 2);
        String comando = partes[0].toUpperCase();
        String resto;
        if (partes.length > 1) {
            resto = partes[1];
        } else {
            resto = "";
        }

        if (comando.equals(Protocolo.ENTRADA)) {
            procesarEntrada(resto);
        } else if (comando.equals(Protocolo.SALIDA)) {
            procesarSalida();
        } else if (comando.equals(Protocolo.MENSAJE)) {
            procesarMensaje(resto);
        } else if (comando.equals(Protocolo.HISTORIAL_HOY)) {
            procesarHistorialHoy();
        } else if (comando.equals(Protocolo.HISTORIAL_FECHA)) {
            procesarHistorialFecha(resto);
        } else {
            logger.aviso("Comando desconocido en salon: " + comando);
        }
    }

    //Entrada de usuario al salon
    private void procesarEntrada(String parametros) {
        String nombre = parametros.trim();
        if (salon.estaLleno()) {
            enviar(Protocolo.ENTRADA_ERROR + " " + Protocolo.ERR_SALON_LLENO);
            return;
        }
        if (salon.estaEnSalon(nombre)) {
            enviar(Protocolo.ENTRADA_ERROR + " " + Protocolo.ERR_YA_EN_SALON);
            return;
        }
        this.nombreUsuario = nombre;
        salon.añadirCliente(nombre, this);
        logger.info("Usuario entro al salon: " + nombre);

        // Carga de mensajes para enviarlos al cliente
        List<String[]> mensajesHoy = salon.getPersistencia().cargarMensajesPorFecha(salon.getNombre(), null);
        enviar(Protocolo.ENTRADA_OK + " " + salon.getNombre() + " " + mensajesHoy.size());
        enviarHistorial(mensajesHoy);
        String notificacion = Protocolo.NOTIFICACION_JOIN + " " + salon.getNombre() + " " + nombre;
        salon.notificacion(nombre, notificacion);
        enviarListaUsuarios();
    }

    //Salida del usuario del salon
    private void procesarSalida() {
        enviar(Protocolo.SALIDA_OK);
        cerrarSesion();
    }

    private void procesarMensaje(String parametros) {
        if (!salon.getServidorCentral().mensajesActivos()) {
            enviar(Protocolo.MENSAJE_ERROR + " " + Protocolo.ERR_MANTENIMIENTO);
            return;
        }

        String texto = parametros.trim();
        if (texto.startsWith("\"") && texto.endsWith("\"")) {
            texto = texto.substring(1, texto.length() - 1);
        } 
        if (texto.isEmpty()) {
            enviar(Protocolo.MENSAJE_ERROR + " " + Protocolo.ERR_MSG_VACIO);
            return;
        }
        if (texto.length() > Protocolo.MAX_CHARS_MENSAJE) {
            enviar(Protocolo.MENSAJE_ERROR + " " + Protocolo.ERR_MSG_LARGO);
            return;
        }

        String msgId  = generarMsgId();
        String tiempo = obtenerFecha();
        enviar(Protocolo.MENSAJE_OK + " " + msgId);
        String difusion = Protocolo.MENSAJE_SERVIDOR + " " + msgId + " " +
            nombreUsuario + " " + tiempo + " \"" + texto + "\"";
        salon.mensajeATodos(difusion);
        salon.getPersistencia().guardarMensaje (salon.getNombre(), msgId, nombreUsuario, tiempo, texto);
        salon.getServidorCentral().incrementarMensajesSalon(salon.getNombre());
        logger.info("Mensaje de " + nombreUsuario + " en salon " + salon.getNombre());
    }

    //Historial de mensajes
    private void procesarHistorialHoy() {
        List<String[]> mensajes = salon.getPersistencia().cargarMensajesPorFecha(salon.getNombre(), null);
        enviarHistorial(mensajes);
    }

    private void procesarHistorialFecha(String parametros) {
        String fecha = parametros.trim();
        List<String[]> mensajes = salon.getPersistencia().cargarMensajesPorFecha(salon.getNombre(), fecha);
        enviarHistorial(mensajes);
    }

    private void enviarHistorial(List<String[]> mensajes) {
        enviar(Protocolo.HISTORIAL_COMENZAR + " " + mensajes.size());
        for (String[] m : mensajes) {
            enviar(Protocolo.MENSAJE_SERVIDOR + " " + m[0] + " " + m[1] + " " + m[2] + " \"" + m[3] + "\"");
        }
        enviar(Protocolo.HISTORIAL_FIN);
    }

    //Notificaciones de usuarios en el salon
    private void enviarListaUsuarios() {
        List<String> usuarios = salon.getUsuariosConectados();
        String mensaje = Protocolo.NOTIFICACION_USUARIOS + " " +
            salon.getNombre() + " " + usuarios.size();
        for (String user : usuarios) {
            mensaje = mensaje + " " + user;
        }
        salon.mensajeATodos(mensaje);
    }

    private String generarMsgId() {
        String prefijo = salon.getNombre().substring(0, 2).toUpperCase();
        return prefijo + System.currentTimeMillis();
    }

    private String obtenerFecha() {
        LocalDateTime ahora   = LocalDateTime.now();
        String dia = String.format("%02d", ahora.getDayOfMonth());
        String mes = String.format("%02d", ahora.getMonthValue());
        String ano = String.valueOf(ahora.getYear());
        String hora = String.format("%02d", ahora.getHour());
        String minutos = String.format("%02d", ahora.getMinute());
        String segundos = String.format("%02d", ahora.getSecond());
        return dia + "-" + mes + "-" + ano + " " + hora + ":" + minutos + ":" + segundos;
    }

    public synchronized void enviar(String mensaje) {
        if (salida != null && !socket.isClosed()) {
            salida.println(mensaje);
        }
    }

    //Cerrar sesion del cliente
    private void cerrarSesion() {
        if (nombreUsuario != null) {
            salon.eliminarCliente(nombreUsuario);
            String notificacion = Protocolo.NOTIFICACION_LEAVE + " " + salon.getNombre() + " " + nombreUsuario;
            salon.mensajeATodos(notificacion);
            logger.info("Usuario salio del salon: " + nombreUsuario);
            nombreUsuario = null;
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error cerrando socket del salon: " + e.getMessage());
        }
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }
}