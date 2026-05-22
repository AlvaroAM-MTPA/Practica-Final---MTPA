import java.util.ArrayList;
import java.util.List;


public class Usuario{
    private String nombre;
    private String clave;         
    private String ultimoLogin;    
    private List<String> ultimosSalones;

    public Usuario(String nombre, String clave) {
        this.nombre = nombre;
        this.clave = clave;
        this.ultimoLogin = "";
        this.ultimosSalones = new ArrayList<>();
    }

    public String getNombre() {
        return nombre;
    }

    public String getClave() {
        return clave;
    }

    public String getUltimoLogin() {
        return ultimoLogin;
    }

    public void setUltimoLogin(String ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
    }

    public List<String> getUltimosSalones() {
        return ultimosSalones;
    }

    //Cuando se ha visitado un salón, se añade a la lista de últimos salones visitados
    public void añadirSalon(String nombreSalon) {
        ultimosSalones.remove(nombreSalon);       
        ultimosSalones.add(0, nombreSalon);
        if (ultimosSalones.size() > 5) {
            ultimosSalones.remove(5);
        }
    }

    //Convertir usuario a linea de texto para guardar en el fichero
    public String linea() {
        String salones = "";
        for (int i = 0; i < ultimosSalones.size(); i++) {
            salones += ultimosSalones.get(i);
            if (i < ultimosSalones.size() - 1) {
                salones += ",";
            }
        }
        return nombre + "|" + clave + "|" + ultimoLogin + "|" + salones;
    }

    //Lectura de fichero y reconstrucción de usuario a partir de una linea de texto
    public static Usuario desdeLinea(String linea) {
        String[] partes = linea.split("\\|", -1);
        if (partes.length < 4){
        return null;
        }
        Usuario user = new Usuario(partes[0], partes[1]);
        user.setUltimoLogin(partes[2]);
        if (!partes[3].isEmpty()) {
            String[] salones = partes[3].split(",");
            for (String salon : salones){
                user.getUltimosSalones().add(salon);
            }
        }
        return user;
    }

    @Override
    public String toString() {
        return "Usuario{nombre='" + nombre + "', ultimoLogin='" + ultimoLogin + "', ultimosSalones=" + ultimosSalones + "}";
    }
}
