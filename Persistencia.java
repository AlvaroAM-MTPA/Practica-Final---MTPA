import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Persistencia {

    private static final String FICHERO_USUARIOS = "usuarios.txt";
    private static final String MENSAJES = "mensajes_";
    private static final String EXTENSION = ".txt";
    
    private final Logger logger = new Logger("Persistencia");
    private final Random random = new Random(); //Generar claves únicas
   
    //APARTADO 1: Lista de usuarios
    //Cargar usuarios desde un fichero
    public List<Usuario> cargarUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        File f = new File(FICHERO_USUARIOS);
        if (!f.exists()) {
            logger.info("No existe el fichero de usuarios, se creará al guardar el primer usuario.");
            return lista;
        }
        try (BufferedReader lector = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.trim().isEmpty()) continue; // Ignorar líneas vacías
                Usuario user = Usuario.desdeLinea(linea);
                if (user != null) {
                    lista.add(user);
                } else {
                    logger.aviso("Línea de usuario mal formada: " + linea);
                }
            }
            logger.info("Usuarios cargados desde fichero: " + lista.size());
        } catch (IOException e) {
            logger.error("Error cargando usuarios: " + e.getMessage());
        }
        return lista;
    }

    //Guardar usuarios en un fichero
    public void guardarUsuarios(List<Usuario> usuarios) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHERO_USUARIOS, false))) {
            for (Usuario user : usuarios) {
                pw.println(user.linea());
            }
            logger.info("Usuarios guardados: " + usuarios.size() + " usuarios.");
        } catch (IOException e) {
            logger.error("Error guardando usuarios: " + e.getMessage());
        }
    }

    //Añadir un usuario a un fichero
    public void añadirUsuario(Usuario user) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHERO_USUARIOS, true))) {
            pw.println(user.linea());
            logger.info("Usuario añadido: " + user.getNombre());
        } catch (IOException e) {
            logger.error("Error añadiendo usuario: " + e.getMessage());
        }
    }
    //APARTADO 2: Info de salones
    //Guardar un mensaje en el historial de un salón
    public void guardarMensaje(String nombreSalon, String msgId, String usuario, String tiempo, String texto) {
        String ruta = MENSAJES + nombreSalon + EXTENSION;
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta, true))) {
            pw.println(msgId + "|" + usuario + "|" + tiempo + "|" + texto);
        } catch (IOException e) {
            logger.error("Error guardando mensaje en " + nombreSalon + ": " + e.getMessage());
        }
    }

    //Cargar mensajes de un salón
    public List<String[]> cargarMensajesPorFecha(String nombreSalon, String fecha) {
        List<String[]> mensajes = new ArrayList<>();
        if (fecha == null) {
            LocalDate hoy = LocalDate.now();
            String dia = String.format("%02d", hoy.getDayOfMonth());
            String mes = String.format("%02d", hoy.getMonthValue());
            String ano = String.valueOf(hoy.getYear());
            fecha = dia + "-" + mes + "-" + ano;
        }
        String ruta = MENSAJES + nombreSalon + EXTENSION;
        File f = new File(ruta);
        if (!f.exists()) {
            return mensajes;
        }
        try (BufferedReader lector = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.trim().isEmpty()) continue; // Ignorar líneas vacías
                String[] partes = linea.split("\\|", 4);
                if (partes.length == 4) {
                    String fechaMensaje = partes[2].split(" ")[0];
                    if (fechaMensaje.equals(fecha)) {
                        mensajes.add(partes);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error cargando mensajes de " + nombreSalon + ": " + e.getMessage());
        }
        return mensajes;
    }

    //APARTADO 3: Generación de claves
    public String generarClave (List<Usuario> usuariosExistentes) {
        List<String> clavesUsadas = new ArrayList<>();
        for (Usuario user : usuariosExistentes) {
            clavesUsadas.add(user.getClave());
        }
        String clave;
        do {
            //clave de 6 digitos
            clave = String.valueOf(100000 + random.nextInt(900000));
        } while (clavesUsadas.contains(clave));
        return clave;
    }
}
