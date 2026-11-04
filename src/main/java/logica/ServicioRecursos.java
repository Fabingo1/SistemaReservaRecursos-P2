package logica;

import datos.CategoriaDAO;
import datos.RecursoDAO;
import modelo.Categoria;
import modelo.Recurso;

import java.sql.SQLException;
import java.util.List;

// Reglas de negocio de Recursos en el Backend. A diferencia de Categoría, aquí el id (número de
// activo) NO se autogenera: lo escribe el administrador y debe ser único.
public class ServicioRecursos {

    private static final int LARGO_MAXIMO_ID = 20;          // VARCHAR2(20) en schema.sql
    private static final int LARGO_MAXIMO_DESCRIPCION = 100; // VARCHAR2(100) en schema.sql

    private static final int ORA_ID_DUPLICADO = 1;        // ORA-00001: unique constraint (la PK del recurso)
    private static final int ORA_PADRE_NO_EXISTE = 2291;  // ORA-02291: la categoria_id ya no existe (carrera)

    private final RecursoDAO recursoDAO;
    private final CategoriaDAO categoriaDAO;

    public ServicioRecursos() {
        this(new RecursoDAO(), new CategoriaDAO());
    }

    /** Para pruebas con DAOs falsos. */
    public ServicioRecursos(RecursoDAO recursoDAO, CategoriaDAO categoriaDAO) {
        this.recursoDAO = recursoDAO;
        this.categoriaDAO = categoriaDAO;
    }

    public List<Recurso> listar() throws SQLException {
        return recursoDAO.listar();
    }

    /** categoria o texto en null (o vacío) = sin ese filtro. */
    public List<Recurso> buscar(Categoria categoria, String texto) throws SQLException {
        String idCategoria = categoria != null ? categoria.getId() : null;
        String textoLimpio = (texto == null || texto.isBlank()) ? null : texto;
        return recursoDAO.buscar(idCategoria, textoLimpio);
    }

    public Recurso crear(String id, Categoria categoria, String descripcion) throws SQLException {
        validarId(id);
        validarDescripcion(descripcion);
        Categoria categoriaReal = validarCategoriaExiste(categoria);

        if (recursoDAO.buscarPorId(id.trim()) != null) {
            throw new IllegalArgumentException("Ya existe un recurso con id/activo " + id);
        }

        Recurso nuevo = new Recurso();
        nuevo.setId(id.trim());
        nuevo.setCategoria(categoriaReal);
        nuevo.setDescripcion(descripcion.trim());

        try {
            recursoDAO.insertar(nuevo);
        } catch (SQLException e) {
            throw traducirErrorDeGuardado(e, id);
        }
        return nuevo;
    }

    public void modificar(String id, Categoria nuevaCategoria, String nuevaDescripcion) throws SQLException {
        validarId(id);
        validarDescripcion(nuevaDescripcion);
        Categoria categoriaReal = validarCategoriaExiste(nuevaCategoria);

        boolean existe;
        try {
            existe = recursoDAO.modificar(id.trim(), categoriaReal.getId(), nuevaDescripcion.trim());
        } catch (SQLException e) {
            throw traducirErrorDeGuardado(e, id);
        }
        if (!existe) {
            throw new IllegalArgumentException("No existe un recurso con id " + id);
        }
    }

    public void borrar(String id) throws SQLException {
        validarId(id);
        String idLimpio = id.trim();

        if (recursoDAO.tieneReservasFuturas(idLimpio)) {
            throw new IllegalArgumentException("No se puede borrar el recurso " + id
                    + ": tiene reservas activas pendientes. Deben cancelarse primero.");
        }
        if (!recursoDAO.borrar(idLimpio)) {
            throw new IllegalArgumentException("No existe un recurso con id " + id);
        }
    }

    private void validarId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id o número de activo del recurso no puede estar vacío.");
        }
        if (id.trim().length() > LARGO_MAXIMO_ID) {
            throw new IllegalArgumentException("El id no puede tener más de " + LARGO_MAXIMO_ID + " caracteres.");
        }
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del recurso no puede estar vacía.");
        }
        if (descripcion.trim().length() > LARGO_MAXIMO_DESCRIPCION) {
            throw new IllegalArgumentException(
                    "La descripción no puede tener más de " + LARGO_MAXIMO_DESCRIPCION + " caracteres.");
        }
    }

    /**
     * Nunca confía en el objeto Categoria que mandó el Frontend (puede venir de un combo
     * desactualizado): siempre la vuelve a buscar en la base de datos por id.
     */
    private Categoria validarCategoriaExiste(Categoria categoria) throws SQLException {
        if (categoria == null || categoria.getId() == null || categoria.getId().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar una categoría para el recurso.");
        }
        Categoria real = categoriaDAO.buscarPorId(categoria.getId());
        if (real == null) {
            throw new IllegalArgumentException("La categoría seleccionada ya no existe.");
        }
        return real;
    }

    /** Traduce los códigos de error de Oracle a mensajes claros para el administrador. */
    private IllegalArgumentException traducirErrorDeGuardado(SQLException e, String id) {
        if (e.getErrorCode() == ORA_ID_DUPLICADO) {
            return new IllegalArgumentException("Ya existe un recurso con id/activo " + id);
        }
        if (e.getErrorCode() == ORA_PADRE_NO_EXISTE) {
            return new IllegalArgumentException("La categoría seleccionada ya no existe.");
        }
        return new IllegalArgumentException("Error de base de datos: " + e.getMessage());
    }
}
