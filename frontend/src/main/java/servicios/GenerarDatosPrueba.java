package servicios;

import modelo.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Main la llama al arrancar si administradores.xml aún no existe
// Usuarios creados: admin1/admin1 (administrador), func1/func1 y func2/func2 (funcionarios).
public class GenerarDatosPrueba {

    public static void main(String[] args) throws IOException {
        generar("data");
        System.out.println("Datos de prueba generados en data/");
    }

    public static boolean generarSiNoExisten(String carpeta) throws IOException {
        if (new File(carpeta, "administradores.xml").exists()) {
            return false;
        }
        generar(carpeta);
        return true;
    }

    /** Sobrescribe los XML de la carpeta con datos de prueba. */
    public static void generar(String carpeta) throws IOException {
        GestorXML gestor = new GestorXML();

        Administrador admin = new Administrador();
        admin.setId("admin1");
        admin.setClave("admin1");
        List<Administrador> admins = new ArrayList<>();
        admins.add(admin);
        gestor.guardarDatos(admins, ruta(carpeta, "administradores.xml"));

        Funcionario func1 = crearFuncionario("func1", "Juan Pérez", "8888-8888");
        Funcionario func2 = crearFuncionario("func2", "María Solís", "8777-7777");
        List<Funcionario> funcionarios = new ArrayList<>();
        funcionarios.add(func1);
        funcionarios.add(func2);
        gestor.guardarDatos(funcionarios, ruta(carpeta, "funcionarios.xml"));

        Categoria catSala = crearCategoria("CAT-001", "Sala para 10 personas");
        Categoria catLaptop = crearCategoria("CAT-002", "Laptop windows 11");
        Categoria catProyector = crearCategoria("CAT-003", "Proyector");
        List<Categoria> categorias = new ArrayList<>();
        categorias.add(catSala);
        categorias.add(catLaptop);
        categorias.add(catProyector);
        gestor.guardarDatos(categorias, ruta(carpeta, "categorias.xml"));

        Recurso sala1 = crearRecurso("SALA-1", catSala, "Sala 1 primer piso");
        Recurso sala2 = crearRecurso("SALA-2", catSala, "Sala 2 segundo piso");
        Recurso laptop1 = crearRecurso("238715", catLaptop, "Laptop #238715");
        Recurso laptop2 = crearRecurso("238716", catLaptop, "Laptop #238716");
        Recurso proyector1 = crearRecurso("PRY-01", catProyector, "Proyector Epson");
        List<Recurso> recursos = new ArrayList<>();
        recursos.add(sala1);
        recursos.add(sala2);
        recursos.add(laptop1);
        recursos.add(laptop2);
        recursos.add(proyector1);
        gestor.guardarDatos(recursos, ruta(carpeta, "recursos.xml"));

        // Fechas relativas a hoy para que las matrices y estadísticas muestren datos al abrir el programa.
        List<Reserva> reservas = new ArrayList<>();
        reservas.add(crearReserva("RES-000001", "Sesión de Junta Directiva", diasDesdeHoy(1), 9, 0, 11, 0,
                func1, new Categoria[]{catSala, catProyector}, new Recurso[]{sala1, proyector1}));
        reservas.add(crearReserva("RES-000002", "Capacitación Excel", diasDesdeHoy(1), 10, 30, 12, 0,
                func2, new Categoria[]{catSala, catLaptop}, new Recurso[]{sala2, laptop1}));
        reservas.add(crearReserva("RES-000003", "Reunión de planificación", diasDesdeHoy(3), 14, 0, 15, 30,
                func1, new Categoria[]{catSala}, new Recurso[]{sala1}));
        reservas.add(crearReserva("RES-000004", "Revisión de presupuesto", diasDesdeHoy(-7), 8, 0, 9, 0,
                func2, new Categoria[]{catLaptop}, new Recurso[]{laptop2}));
        gestor.guardarDatos(reservas, ruta(carpeta, "reservas.xml"));
    }

    // ------------------------------------------------------------------

    private static String ruta(String carpeta, String archivo) {
        return new File(carpeta, archivo).getPath();
    }

    private static Funcionario crearFuncionario(String id, String nombre, String telefono) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setClave(id);
        f.setNombre(nombre);
        f.setTelefono(telefono);
        return f;
    }

    private static Categoria crearCategoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    private static Recurso crearRecurso(String id, Categoria categoria, String descripcion) {
        Recurso r = new Recurso();
        r.setId(id);
        r.setCategoria(categoria);
        r.setDescripcion(descripcion);
        return r;
    }

    private static Date diasDesdeHoy(int dias) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, dias);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date hora(Date dia, int h, int m) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dia);
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        return cal.getTime();
    }

    private static Reserva crearReserva(String id, String actividad, Date dia, int hIni, int mIni,
                                        int hFin, int mFin, Funcionario funcionario,
                                        Categoria[] categorias, Recurso[] recursos) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setActividad(actividad);
        r.setFecha(dia);
        r.setHoraInicio(hora(dia, hIni, mIni));
        r.setHoraFin(hora(dia, hFin, mFin));

        // Copia del funcionario SIN clave (igual que hace ReservaController)
        Funcionario copia = new Funcionario();
        copia.setId(funcionario.getId());
        copia.setNombre(funcionario.getNombre());
        copia.setTelefono(funcionario.getTelefono());
        r.setFuncionario(copia);

        for (int i = 0; i < categorias.length; i++) {
            DetalleReserva d = new DetalleReserva();
            d.setCategoriaSolicitada(categorias[i]);
            d.setRecursoAsignado(recursos[i]);
            r.getDetalles().add(d);
        }
        return r;
    }
}
