package servicios;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PDFService {

    private static final float MARGEN = 40f;
    private static final float ALTO_FILA = 18f;
    private static final float TAMANO_TITULO = 16f;
    private static final float TAMANO_TEXTO = 10f;

    public void generarReporte(String titulo, String[] columnas, List<Object[]> filas, String rutaSalida)
            throws IOException {

        File archivoSalida = new File(rutaSalida);
        File carpeta = archivoSalida.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }

        PDFont fuenteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont fuenteNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDDocument documento = new PDDocument()) {
            // Con muchas columnas (matrices semanales/de recursos) se usa hoja horizontal
            PDRectangle tamano = columnas.length > 4
                    ? new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth())
                    : PDRectangle.LETTER;
            PDPage pagina = new PDPage(tamano);
            documento.addPage(pagina);

            float altoPagina = pagina.getMediaBox().getHeight();
            float anchoUtil = pagina.getMediaBox().getWidth() - 2 * MARGEN;
            float anchoColumna = columnas.length > 0 ? anchoUtil / columnas.length : anchoUtil;

            PDPageContentStream contenido = new PDPageContentStream(documento, pagina);
            float y = altoPagina - MARGEN;

            y = escribirEncabezado(contenido, titulo, fuenteNegrita, fuenteNormal, y);
            y = escribirFilaEncabezadoTabla(contenido, columnas, fuenteNegrita, anchoColumna, y);

            for (Object[] fila : filas) {
                if (y < MARGEN + ALTO_FILA) {
                    contenido.close();
                    pagina = new PDPage(tamano);
                    documento.addPage(pagina);
                    contenido = new PDPageContentStream(documento, pagina);
                    y = altoPagina - MARGEN;
                    y = escribirFilaEncabezadoTabla(contenido, columnas, fuenteNegrita, anchoColumna, y);
                }
                y = escribirFilaDatos(contenido, fila, fuenteNormal, anchoColumna, y);
            }

            contenido.close();
            documento.save(archivoSalida);
        }
    }

    private float escribirEncabezado(PDPageContentStream contenido, String titulo, PDFont fuenteNegrita,
                                      PDFont fuenteNormal, float y) throws IOException {
        contenido.beginText();
        contenido.setFont(fuenteNegrita, TAMANO_TITULO);
        contenido.newLineAtOffset(MARGEN, y);
        contenido.showText(limpiar(titulo));
        contenido.endText();

        float y2 = y - TAMANO_TITULO - 4;
        String fechaGeneracion = "Generado: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        contenido.beginText();
        contenido.setFont(fuenteNormal, TAMANO_TEXTO - 1);
        contenido.newLineAtOffset(MARGEN, y2);
        contenido.showText(limpiar(fechaGeneracion));
        contenido.endText();

        return y2 - ALTO_FILA;
    }

    private float escribirFilaEncabezadoTabla(PDPageContentStream contenido, String[] columnas,
                                               PDFont fuenteNegrita, float anchoColumna, float y) throws IOException {
        float x = MARGEN;
        contenido.setFont(fuenteNegrita, TAMANO_TEXTO);
        for (String columna : columnas) {
            contenido.beginText();
            contenido.newLineAtOffset(x, y);
            contenido.showText(ajustarAncho(limpiar(columna), fuenteNegrita, TAMANO_TEXTO, anchoColumna - 4));
            contenido.endText();
            x += anchoColumna;
        }

        float lineaY = y - 4;
        contenido.setLineWidth(0.5f);
        contenido.moveTo(MARGEN, lineaY);
        contenido.lineTo(MARGEN + anchoColumna * columnas.length, lineaY);
        contenido.stroke();

        return lineaY - ALTO_FILA;
    }

    private float escribirFilaDatos(PDPageContentStream contenido, Object[] fila, PDFont fuenteNormal,
                                     float anchoColumna, float y) throws IOException {
        float x = MARGEN;
        contenido.setFont(fuenteNormal, TAMANO_TEXTO);
        for (Object valor : fila) {
            String texto = ajustarAncho(limpiar(valor == null ? "" : valor.toString()),
                    fuenteNormal, TAMANO_TEXTO, anchoColumna - 4);
            contenido.beginText();
            contenido.newLineAtOffset(x, y);
            contenido.showText(texto);
            contenido.endText();
            x += anchoColumna;
        }
        return y - ALTO_FILA;
    }

    private String limpiar(String texto) {
        if (texto == null) return "";
        String limpio = texto
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace("ñ", "n").replace("Ñ", "N").replace("ü", "u").replace("Ü", "U")
                .replace("\n", " ").replace("\r", " ").replace("\t", " ");
        // Cualquier otro carácter fuera de ASCII imprimible se reemplaza por '?'
        StringBuilder sb = new StringBuilder();
        for (char c : limpio.toCharArray()) {
            sb.append(c >= 32 && c < 127 ? c : '?');
        }
        return sb.toString();
    }

    private String ajustarAncho(String texto, PDFont fuente, float tamano, float anchoMaximo) throws IOException {
        if (anchoTexto(texto, fuente, tamano) <= anchoMaximo) return texto;
        String puntos = "...";
        int fin = texto.length();
        while (fin > 0 && anchoTexto(texto.substring(0, fin) + puntos, fuente, tamano) > anchoMaximo) {
            fin--;
        }
        return fin == 0 ? "" : texto.substring(0, fin) + puntos;
    }

    private float anchoTexto(String texto, PDFont fuente, float tamano) throws IOException {
        return fuente.getStringWidth(texto) / 1000f * tamano;
    }

    public static void main(String[] args) throws IOException {
        // Prueba manual rápida
        PDFService service = new PDFService();
        List<Object[]> filas = List.of(
                new Object[]{"Sala de Juntas", 5},
                new Object[]{"Laptop Windows 11", 3}
        );
        service.generarReporte("Reporte de prueba", new String[]{"Categoria", "Cantidad"}, filas,
                "data/reporte_prueba.pdf");
        System.out.println("Reporte de prueba generado en data/reporte_prueba.pdf");
    }
}
