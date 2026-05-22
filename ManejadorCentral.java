import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;

public class ManejadorCentral extends Thread {

    private final Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;     
    private final ServidorCentral servidor;
    private final Logger logger;
    private long ultimoHeartbeat; //Para los heartbeats (guardado de timestamp)

    public ManejadorCentral(Socket socket, ServidorCentral servidor) {
        this.socket       = socket;
        this.servidor     = servidor;
        this.logger       = new Logger("Servidor-Central");
        this.ultimoHeartbeat = System.currentTimeMillis();

        try {
            this.entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")); // UTF-8 para soportar caracteres especiales
            this.salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            logger.error("Error abriendo streams del socket: " + e.getMessage());
        }
    }

    public void run() {
        //Gestión de conexiones
        try { //Gestión de conexiones
            logger.info("Nueva conexion desde " + socket.getInetAddress());
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    procesarComando(linea.trim());
                }
            }
        } catch (IOException e) {
            String info = "Conexion cerrada inesperadamente";
            if (nombreUsuario != null) {
                info = info + " (usuario: " + nombreUsuario + ")";
            }
            logger.aviso(info + ": " + e.getMessage());
        } finally {
            cerrarSesion();
        }
    }

    //Comandos
    private void procesarComando(String linea) {
        String[] partes = linea.split(" ", 2);
        String comando = partes[0].toUpperCase();
        String resto;
        if (partes.length > 1) {
            resto = partes[1];
        } else {
            resto = "";
        }
        String usuarioLog;
        if (nombreUsuario != null) {
            usuarioLog = nombreUsuario;
        } else {
            //Si el usuario no ha hecho login, mostramos que es desconocido
            usuarioLog = "desconocido";
        }
        logger.info("Recibido de [" + usuarioLog + "]: " + linea);

        if (comando.equals(Protocolo.REGISTRO)) {
            procesarRegistro(resto);
        } else if (comando.equals(Protocolo.LOGIN)) {
            procesarLogin(resto);
        } else if (comando.equals(Protocolo.LOGOUT)) {
            procesarLogout();
        } else if (comando.equals(Protocolo.HEARTBEAT)) {
            procesarHeartbeat();
        } else if (comando.equals(Protocolo.MENSAJEPRIV_CHECK)) {
            procesarMensajePrivCheck(resto);
        } else if (comando.equals(Protocolo.MENSAJEPRIV)) {
            procesarMensajePrivado(resto);
        } else if (comando.equals(Protocolo.MENSAJEPRIV_CIERRE)) {
            procesarMensajePrivCierre(resto);
        } else {
            logger.aviso("Comando desconocido recibido: " + comando);
        }
    }

    //Registro de usuario
    private void procesarRegistro(String parametros) {
        String nombre = parametros.trim();
        if (nombre.isEmpty() || nombre.contains("|") || nombre.contains(" ")) {
            enviar(Protocolo.REGISTRO_ERROR + " " + Protocolo.ERR_USUARIO_INVALIDO);
            return;
        }
        if (servidor.existeUsuario(nombre)) {
            enviar(Protocolo.REGISTRO_ERROR + " " + Protocolo.ERR_USUARIO_COGIDO);
            return;
        }
        // El servidor genera una clave unica
        String clave = servidor.registrarUsuario(nombre);
        logger.info("Usuario registrado: " + nombre + " con clave " + clave);
        enviar(Protocolo.REGISTRO_OK + " " + clave);
    }

    //Login de usuario
    private void procesarLogin(String parametros) {
        String[] partes = parametros.split(" ", 2);
        if (partes.length < 2) {
            enviar(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_USUARIO_INVALIDO);
            return;
        }
        String nombre = partes[0];
        String clave  = partes[1];
        if (!servidor.existeUsuario(nombre)) {
            enviar(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_USUARIO_NO_ENCONTRADO);
            return;
        }
        if (!servidor.claveCorrecta(nombre, clave)) {
            enviar(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_CLAVE_INVALIDA);
            return;
        }
        if (servidor.estaConectado(nombre)) {
            enviar(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_YA_CONECTADO);
            return;
        }
        if (!servidor.aceptandoClientes()) {
            enviar(Protocolo.LOGIN_ERROR + " " + Protocolo.ERR_CONEXION_NO_PERMITIDA);
            return;
        }
        //Login exitoso, registramos al usuario como conectado
        this.nombreUsuario = nombre;
        servidor.conectarUsuario(nombre, this);
        servidor.actualizarUltimoLogin(nombre, obtenerFecha());
        logger.info("Login exitoso: " + nombre);
        enviar(Protocolo.LOGIN_OK + " " + nombre);

        //Lista de salones disponibles
        String listaSalones = Protocolo.LISTA_SALONES + " " + Protocolo.SALONES.length;
        for (Protocolo.Salon salon : Protocolo.SALONES) {
            listaSalones = listaSalones + " " + salon.nombre + ":" + salon.puerto;
        }
        enviar(listaSalones);
        enviarUsuariosOnline();
    }

    //Logout de usuario
    private void procesarLogout() {
        logger.info("Logout de: " + nombreUsuario);
        enviar(Protocolo.LOGOUT_OK);
        cerrarSesion();
    }

    //Heartbeat
    private void procesarHeartbeat() {
        ultimoHeartbeat = System.currentTimeMillis();
        enviar(Protocolo.HEARTBEAT_OK);
    }

    public long getUltimoHeartbeat() {
        return ultimoHeartbeat;
    }

    //Mensajes privados
    private void procesarMensajePrivCheck(String destino) {
        destino = destino.trim();
        if (!servidor.existeUsuario(destino)) {
            enviar(Protocolo.MENSAJEPRIV_CHECK_ERROR + " " + Protocolo.ERR_USUARIO_PM_NO_EXISTE);
        } else if (!servidor.estaConectado(destino)) {
            enviar(Protocolo.MENSAJEPRIV_CHECK_ERROR + " " + Protocolo.ERR_USUARIO_NO_CONECTADO);
        } else {
            enviar(Protocolo.MENSAJEPRIV_CHECK_OK + " " + destino);
        }
    }

 
    //Procesamiento de mensaje privado
    private void procesarMensajePrivado(String parametros) {
        // Separamos destino del texto por el primer espacio
        String[] partes = parametros.split(" ", 2);
        if (partes.length < 2) {
            logger.aviso("Mensaje mal formado de: " + nombreUsuario);
            return;
        }
        String destino = partes[0];
        String texto   = partes[1].trim();
        // Quitamos las comillas del texto si las tiene
        if (texto.startsWith("\"") && texto.endsWith("\"")) {
            texto = texto.substring(1, texto.length() - 1);
        }
        if (!servidor.estaConectado(destino)) {
            enviar(Protocolo.MENSAJEPRIV_CHECK_ERROR + " " + Protocolo.ERR_USUARIO_NO_CONECTADO);
            return;
        }
        String msgParaDestino = Protocolo.MENSAJEPRIV_ENVIO + " " + nombreUsuario +
            " " + obtenerFecha() + " \"" + texto + "\"";
        servidor.enviarAUsuario(destino, msgParaDestino);
    }

    //Procesamiento de cierre de conversación privada
    private void procesarMensajePrivCierre(String destino) {
        logger.info(nombreUsuario + " cerro conversacion privada con " + destino.trim());
    }
    //Lista de usuarios online
    private void enviarUsuariosOnline() {
        List<String> online = servidor.getUsuariosConectados();
        String mensaje = Protocolo.USUARIOS_ONLINE + " " + online.size();
        for (String usuario : online) {
            mensaje = mensaje + " " + usuario;
        }
        enviar(mensaje);
    }
    //Fecha y hora
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

    //Método sincronizado para enviar mensajes al cliente, evitando problemas de concurrencia
    public synchronized void enviar(String mensaje) {
        if (salida != null && !socket.isClosed()) {
            salida.println(mensaje);
            String destinatario;
            if (nombreUsuario != null) {
                destinatario = nombreUsuario;
            } else {
                destinatario = "?";
            }
            logger.info("Enviado a [" + destinatario + "]: " + mensaje);
        }
    }

    // Cierre de sesión
    private void cerrarSesion() {
        if (nombreUsuario != null) {
            servidor.desconectarUsuario(nombreUsuario);
            logger.info("Sesion cerrada: " + nombreUsuario);
            nombreUsuario = null;
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error cerrando socket: " + e.getMessage());
        }
    }
    public String getNombreUsuario() {
        return nombreUsuario;
    }
}