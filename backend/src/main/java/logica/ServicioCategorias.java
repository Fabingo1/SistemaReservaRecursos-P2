package logica;

import datos.CategoriaDAO;
import modelo.Categoria;

import java.sql.SQLException;
import java.util.List;

// Reglas de negocio de Categorías en el Backend. El id es autogenerado (CAT-001, CAT-002, ...).
public class ServicioCategorias {

    private static final int LARGO_MAXIMO = 100; // VARCHAR2(100) en schema.sql
    private static final int ORA_HIJOS_EXISTEN = 2292; // ORA-02292: hay filas hijas (FK)

    // Compartido por todos los clientes: dos administradores creando a la vez no reciben el mismo id
    private static final Object CANDADO = new Object();

    private final CategoriaDAO categoriaDAO;

    public ServicioCategorias() {
        this(new CategoriaDAO());
    }

    /** Para pruebas con un DAO falso. */
    public ServicioCategorias(CategoriaDAO categoriaDAO) {
        this.categoriaDAO = categoriaDAO;
    }

    public List<Categoria> listar() throws SQLException {
        return categoriaDAO.listar();
    }

    public List<Categoria> buscar(String texto) throws SQLException {
        if (texto == null || texto.isBlank()) {
            return listar();
        }
        return categoriaDAO.buscar(texto);
    }

    public Categoria crear(String descripcion) throws SQLException {
        validarDescripcion(descripcion);
        synchronized (CANDADO) {
            List<Categoria> existentes = categoriaDAO.listar();
            validarDescripcionUnica(existentes, descripcion, null);

            Categoria nueva = new Categoria();
            nueva.setId(generarId(existentes));
            nueva.setDescripcion(descripcion.trim());
            categoriaDAO.insertar(nueva);
            return nueva;
        }
    }

    public void modificar(String id, String descripcion) throws SQLException {
        validarId(id);
        validarDescripcion(descripcion);
        synchronized (CANDADO) {
            validarDescripcionUnica(categoriaDAO.listar(), descripcion, id.trim());
            if (!categoriaDAO.modificar(id.trim(), descripcion.trim())) {
                throw new IllegalArgumentException("No existe una categoría con id " + id);
            }
        }
    }

    public void borrar(String id) throws SQLException {
        validarId(id);
        String idLimpio = id.trim();

        int recursos = categoriaDAO.contarRecursos(idLimpio);
        if (recursos > 0) {
            throw new IllegalArgumentException(mensajeConRecursos(recursos));
        }
        try {
            if (!categoriaDAO.borrar(idLimpio)) {
                throw new IllegalArgumentException("No existe una categoría con id " + id);
            }
        } catch (SQLException e) {
            // Por si otro administrador le agregó un recurso justo entre la revisión y el borrado
            if (e.getErrorCode() == ORA_HIJOS_EXISTEN) {
                throw new IllegalArgumentException(mensajeConRecursos(categoriaDAO.contarRecursos(idLimpio)));
            }
            throw e;
        }
    }

    void validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción de la categoría no puede estar vacía.");
        }
        if (descripcion.trim().length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException("La descripción no puede tener más de " + LARGO_MAXIMO + " caracteres.");
        }
    }

    private void validarId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Seleccione una categoría.");
        }
    }

    private void validarDescripcionUnica(List<Categoria> categorias, String descripcion, String idExcluido) {
        String buscada = descripcion.trim();
        for (Categoria c : categorias) {
            if (c.getId().equals(idExcluido)) {
                continue;
            }
            if (c.getDescripcion() != null && c.getDescripcion().trim().equalsIgnoreCase(buscada)) {
                throw new IllegalArgumentException("Ya existe una categoría con la descripción \"" + buscada + "\".");
            }
        }
    }

    /** Siguiente número después del mayor existente: CAT-004 si la mayor es CAT-003. */
    String generarId(List<Categoria> existentes) {
        int max = 0;
        for (Categoria c : existentes) {
            String soloNumero = c.getId() == null ? "" : c.getId().replaceAll("[^0-9]", "");
            if (!soloNumero.isEmpty()) {
                try {
                    max = Math.max(max, Integer.parseInt(soloNumero));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("CAT-%03d", max + 1);
    }

    private String mensajeConRecursos(int cantidad) {
        return "No se puede borrar la categoría: tiene " + cantidad
                + " recurso(s) asociado(s). Borre o reasigne esos recursos primero.";
    }
}
