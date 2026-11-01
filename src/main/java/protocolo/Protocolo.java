package protocolo;

// Constantes que comparten Frontend (ServiceProxy) y Backend (Worker): así ambos lados
// usan exactamente los mismos nombres y un error de dedo no rompe la comunicación.
public final class Protocolo {

    private Protocolo() {
    }

    // Primer mensaje de cada socket: indica para qué es esa conexión.
    // PETICIONES = petición/respuesta normal. ALERTAS = solo notificaciones del Backend al Frontend.
    public static final String CANAL_PETICIONES = "PETICIONES";
    public static final String CANAL_ALERTAS = "ALERTAS";

    // Login
    public static final String LOGIN = "login";
    public static final String CAMBIAR_CLAVE = "cambiarClave";
    public static final String LOGOUT = "logout";

    // Funcionarios
    public static final String LISTAR_FUNCIONARIOS = "listarFuncionarios";
    public static final String BUSCAR_FUNCIONARIOS = "buscarFuncionarios";
    public static final String CREAR_FUNCIONARIO = "crearFuncionario";
    public static final String MODIFICAR_FUNCIONARIO = "modificarFuncionario";
    public static final String BORRAR_FUNCIONARIO = "borrarFuncionario";

    // Categorías
    public static final String LISTAR_CATEGORIAS = "listarCategorias";
    public static final String BUSCAR_CATEGORIAS = "buscarCategorias";
    public static final String CREAR_CATEGORIA = "crearCategoria";
    public static final String MODIFICAR_CATEGORIA = "modificarCategoria";
    public static final String BORRAR_CATEGORIA = "borrarCategoria";

    // Recursos
    public static final String BUSCAR_RECURSOS = "buscarRecursos";
    public static final String LISTAR_RECURSOS = "listarRecursos";
    public static final String CREAR_RECURSO = "crearRecurso";
    public static final String MODIFICAR_RECURSO = "modificarRecurso";
    public static final String BORRAR_RECURSO = "borrarRecurso";

    // Reservas
    public static final String CREAR_RESERVA = "crearReserva";
    public static final String CANCELAR_RESERVA = "cancelarReserva";
    public static final String OBTENER_RESERVAS_DE = "obtenerReservasDe";
    public static final String EXTRAER_DATOS_RESERVA = "extraerDatosReserva";

    // Calendarización, Actividades y Estadísticas
    public static final String OBTENER_RESERVAS_ENTRE = "obtenerReservasEntre";
    public static final String ESTADISTICAS_RECURSOS = "estadisticasRecursos";
    public static final String ESTADISTICAS_ACTIVIDADES = "estadisticasActividades";
}
