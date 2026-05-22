
public class Protocolo {

    public static final int PUERTO_CENTRAL = 1000;
    public static final int PUERTO_SALON_IA = 1001;
    public static final int PUERTO_SALON_DEPORTES = 1002;
    public static final int PUERTO_SALON_THERIAN = 1003;
    public static final int PUERTO_SALON_MANGA = 1004;
    public static final int PUERTO_SALON_UEMC = 1005;
   
    //Clase auxiliar para representar los salones, con su nombre y puerto
    public static class Salon {
    public final String nombre;
    public final int puerto;

    public Salon(String nombre, int puerto) {
        this.nombre = nombre;
        this.puerto = puerto;
        }
    }
    
    //Arrays con los nombres y puertos de los salones
    public static final Salon[] SALONES = {
        new Salon("IA", PUERTO_SALON_IA),
        new Salon("Deportes", PUERTO_SALON_DEPORTES),
        new Salon("Therian", PUERTO_SALON_THERIAN),
        new Salon("Manga", PUERTO_SALON_MANGA),
        new Salon("UEMC", PUERTO_SALON_UEMC)
    };

    //COMANDOS
    //Registro y Autenticación
    public static final String REGISTRO = "REGISTRO";
    public static final String REGISTRO_OK = "REGISTRO_OK";
    public static final String REGISTRO_ERROR = "REGISTRO_ERROR";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_ERROR = "LOGIN_ERROR";
    public static final String LOGOUT = "LOGOUT";
    public static final String LOGOUT_OK = "LOGOUT_OK";

    //Gestión de salones
    public static final String LISTA_SALONES = "LISTA_SALONES";

    //Mensajes privados
    public static final String MENSAJEPRIV_CHECK = "MENSAJEPRIV_CHECK";
    public static final String MENSAJEPRIV_CHECK_OK = "MENSAJEPRIV_CHECK_OK";
    public static final String MENSAJEPRIV_CHECK_ERROR = "MENSAJEPRIV_CHECK_ERROR";
    public static final String MENSAJEPRIV = "MENSAJEPRIV";
    public static final String MENSAJEPRIV_ENVIO = "MENSAJEPRIV_ENVIO";
    public static final String MENSAJEPRIV_CIERRE = "MENSAJEPRIV_CIERRE";

    //Heartbeat
    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String HEARTBEAT_OK = "HEARTBEAT_OK";

    //Notificaciones
    public static final String NOTIFICACION_JOIN = "NOTIFICACION_JOIN";
    public static final String NOTIFICACION_LEAVE = "NOTIFICACION_LEAVE";
    public static final String NOTIFICACION_USUARIOS = "NOTIFICACION_USUARIOS";
    public static final String USUARIOS_ONLINE = "USUARIOS_ONLINE";

    //Entrada y salida de salones
    public static final String ENTRADA = "ENTRADA";
    public static final String ENTRADA_OK = "ENTRADA_OK";
    public static final String ENTRADA_ERROR = "ENTRADA_ERROR";
    public static final String SALIDA = "SALIDA";
    public static final String SALIDA_OK = "SALIDA_OK";

    //Mensajes de salones
    public static final String MENSAJE = "MENSAJE";
    public static final String MENSAJE_OK = "MENSAJE_OK";
    public static final String MENSAJE_ERROR = "MENSAJE_ERROR";
    public static final String MENSAJE_SERVIDOR = "MENSAJE_SERVIDOR";

    //Historial de mensajes
    public static final String HISTORIAL_HOY = "HISTORIAL_HOY";
    public static final String HISTORIAL_FECHA = "HISTORIAL_FECHA";
    public static final String HISTORIAL_COMENZAR = "HISTORIAL_COMENZAR";
    public static final String HISTORIAL_FIN = "HISTORIAL_FIN";

    //Control del servidor
    public static final String STOP_CLIENTES = "STOP_CLIENTES";
    public static final String CONTINUAR_CLIENTES = "CONTINUAR_CLIENTES";
    public static final String STOP_MENSAJES = "STOP_MENSAJES";
    public static final String CONTINUAR_MENSAJES = "CONTINUAR_MENSAJES";
    public static final String ESTADO_USUARIOS = "ESTADO_USUARIOS";
    public static final String ESTADO_SALON = "ESTADO_SALON";
    public static final String ESTADO_TODO = "ESTADO_TODO";

    // Códigos de error
    public static final String ERR_USUARIO_COGIDO = "101";
    public static final String ERR_USUARIO_INVALIDO = "102";
    public static final String ERR_USUARIO_NO_ENCONTRADO = "103";
    public static final String ERR_CLAVE_INVALIDA = "104";
    public static final String ERR_YA_CONECTADO = "105";
    public static final String ERR_SESION_EXPIRADA = "106";
    public static final String ERR_MSG_LARGO = "201";
    public static final String ERR_MSG_VACIO = "202";
    public static final String ERR_MSG_INVALIDO = "203";
    public static final String ERR_NO_EN_SALON = "204";
    public static final String ERR_SALON_NO_ENCONTRADO = "301";
    public static final String ERR_SALON_LLENO = "302";
    public static final String ERR_YA_EN_SALON = "303";
    public static final String ERR_NO_AUTORIZADO = "304";
    public static final String ERR_USUARIO_NO_CONECTADO = "401";
    public static final String ERR_USUARIO_PM_NO_EXISTE = "402";
    public static final String ERR_MANTENIMIENTO = "501";
    public static final String ERR_CONEXION_NO_PERMITIDA = "502";
    public static final String ERR_INTERNO = "503";

    //Límites
    public static final int MAX_CHARS_MENSAJE = 190;
    public static final int HEARTBEAT_INTERVALO = 30;
    public static final int HEARTBEAT_TIMEOUT = 90;
    public static final int MAX_CLIENTES_SALON = 50;
    
}
