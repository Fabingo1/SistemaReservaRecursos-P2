package servicios;

import modelo.Categoria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class  GestorXMLIT {

    @Test
    void guardarYCargarCategorias_devuelveLosMismosDatos(@TempDir Path tempDir) throws IOException {
        Categoria original = new Categoria();
        original.setId("CAT-000001");
        original.setDescripcion("Sala para 10 personas");

        List<Categoria> datos = new ArrayList<>();
        datos.add(original);

        String ruta = tempDir.resolve("categorias.xml").toString();
        GestorXML gestorXML = new GestorXML();

        gestorXML.guardarDatos(datos, ruta);
        List<Categoria> recuperado = gestorXML.cargarDatos(ruta);

        assertEquals(1, recuperado.size());
        assertEquals("CAT-000001", recuperado.get(0).getId());
        assertEquals("Sala para 10 personas", recuperado.get(0).getDescripcion());
    }
}
