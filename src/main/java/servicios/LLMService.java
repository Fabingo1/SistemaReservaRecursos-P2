package servicios;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLMService {

    private static final String MODELO = "gemini-3.5-flash";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODELO + ":generateContent";

    private static final String PROMPT_SISTEMA_BASE =
            "Extraés datos de reservas de recursos a partir de una frase en lenguaje natural. " +
                    "Respondé UNICAMENTE con un objeto JSON, sin texto adicional antes ni después, " +
                    "sin explicaciones, sin bloques de código markdown, sin ```. " +
                    "El JSON debe tener exactamente estas claves: " +
                    "\"actividad\" (String, breve), " +
                    "\"fecha\" (String, formato yyyy-MM-dd), " +
                    "\"horaInicio\" (String, formato HH:mm), " +
                    "\"horaFin\" (String, formato HH:mm), " +
                    "\"categorias\" (String, nombres de categorías separados por coma). " +
                    "Si algún dato no se menciona en la frase, dejá esa clave con un string vacío.";

    // Límites de espera para que la llamada no se quede colgada
    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(10);
    private static final Duration TIEMPO_RESPUESTA = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String apiKey;

    public LLMService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIEMPO_CONEXION).build();
        this.apiKey = System.getenv("GEMINI_API_KEY");
    }

    /** categoriasDisponibles: descripciones reales de la BD; el LLM solo puede elegir de ahí. */
    public Map<String, String> extraerDatos(String frase, List<String> categoriasDisponibles)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "No se encontró la variable de entorno GEMINI_API_KEY. Configuralo antes de usar esta funcionalidad.");
        }

        JSONObject cuerpo = construirCuerpoRequest(frase, categoriasDisponibles);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(TIEMPO_RESPUESTA)
                .header("x-goog-api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error de la API (código " + response.statusCode() + "): " + response.body());
        }

        String textoRespuesta = extraerTextoDeRespuesta(response.body());
        JSONObject datosExtraidos = new JSONObject(textoRespuesta);

        Map<String, String> resultado = new HashMap<>();
        resultado.put("actividad", datosExtraidos.optString("actividad", ""));
        resultado.put("fecha", datosExtraidos.optString("fecha", ""));
        resultado.put("horaInicio", datosExtraidos.optString("horaInicio", ""));
        resultado.put("horaFin", datosExtraidos.optString("horaFin", ""));
        resultado.put("categorias", datosExtraidos.optString("categorias", ""));

        return resultado;
    }

    private JSONObject construirCuerpoRequest(String frase, List<String> categoriasDisponibles) {
        JSONObject textoUsuario = new JSONObject().put("text", frase);
        JSONObject contenidoUsuario = new JSONObject().put("parts", new JSONArray().put(textoUsuario));

        JSONObject textoSistema = new JSONObject().put("text", construirPromptSistema(categoriasDisponibles));
        JSONObject instruccionSistema = new JSONObject().put("parts", new JSONArray().put(textoSistema));

        JSONObject cuerpo = new JSONObject();
        cuerpo.put("contents", new JSONArray().put(contenidoUsuario));
        cuerpo.put("systemInstruction", instruccionSistema);
        return cuerpo;
    }

    private String construirPromptSistema(List<String> categoriasDisponibles) {
        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "La fecha de hoy es " + fechaHoy + ". Usá esa fecha como referencia para resolver "
                + "expresiones relativas como \"mañana\", \"el próximo [día]\" o \"la semana que viene\". "
                + PROMPT_SISTEMA_BASE
                + " Las categorías disponibles son: " + String.join(" | ", categoriasDisponibles) + ". "
                + "En \"categorias\" escribí SOLO nombres copiados exactamente de esa lista, "
                + "eligiendo la que mejor encaje con lo pedido (una por cada recurso mencionado). "
                + "Si ninguna encaja, dejala vacía.";
    }

    private String extraerTextoDeRespuesta(String cuerpoRespuesta) throws IOException {
        JSONObject json = new JSONObject(cuerpoRespuesta);
        if (!json.has("candidates") || json.getJSONArray("candidates").isEmpty()) {
            throw new IOException("La IA no devolvió ninguna respuesta. Intente con otra frase.");
        }
        String texto = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();

        if (texto.startsWith("```")) {
            texto = texto.replaceFirst("^```(json)?", "").trim();
            if (texto.endsWith("```")) {
                texto = texto.substring(0, texto.length() - 3).trim();
            }
        }
        return texto;
    }

    //para pruebas
    public static void main(String[] args) throws IOException, InterruptedException {
        LLMService service = new LLMService();

        String frase = "necesito hacer una reunión de trabajo de 10 personas el próximo 14 de agosto de 8am a 10am en una sala y usando una laptop";

        System.out.println("Frase: " + frase);
        System.out.println("Consultando la API...");

        List<String> categorias = List.of("Sala de Juntas", "Sala para 10 personas", "Laptop", "Proyector");
        Map<String, String> datos = service.extraerDatos(frase, categorias);

        System.out.println("Resultado:");
        for (Map.Entry<String, String> entry : datos.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
    }
}