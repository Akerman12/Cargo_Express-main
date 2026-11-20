package com.example.guias.controller;

import com.example.guias.repository.GuiaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puente entre el SAI-Bot y OpenRouter.
 *
 * Se expone como GET /chat/ia?q=...&h=... para no necesitar token CSRF y poder
 * usarse tambien desde el login y la pagina publica de rastreo.
 *
 * Al prompt se le inyecta contexto real del sistema (numero de envios y
 * ciudades con mas movimiento) para que pueda responder preguntas de negocio.
 */
@RestController
public class ChatIaController {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final int MAX_PREGUNTA = 1000;
    private static final int LIMITE_POR_MINUTO = 25;
    private static final int MAX_MENSAJES_HISTORIAL = 4;
    private static final int MAX_LARGO_HISTORIAL = 300;

    private static final String INSTRUCCIONES =
            "Eres SAI, el asistente virtual de Cargo Express, una empresa colombiana de logistica y envios. "
          + "Responde SIEMPRE en espanol neutro, de forma breve (maximo 90 palabras), clara, amable y practica. "
          + "Hablas de envios, paquetes, guias, rastreo, precios, tiempos de entrega, cobertura, logistica y "
          + "servicios de Cargo Express; si te preguntan algo totalmente ajeno, redirige el tema con amabilidad. "
          + "Si el usuario pregunta por ciudades, estadisticas, cobertura o volumen de envios, responde usando el "
          + "CONTEXTO DEL SISTEMA que aparece abajo. Si alli dice que no hay envios registrados o no hay datos, "
          + "dilo con naturalidad y menciona las ciudades de cobertura; nunca inventes cifras. "
          + "Solo pide el numero de guia cuando la pregunta sea sobre el estado de un envio concreto. "
          + "Para preguntas generales responde directamente, sin pedir datos personales. "
          + "Usa el historial de la conversacion para entender respuestas cortas o seguimientos "
          + "(por ejemplo: 'si', 'esas ciudades', 'la segunda', 'y cuanto cuesta').";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final GuiaRepository guiaRepository;

    private final Map<String, Deque<Long>> peticionesPorIp = new ConcurrentHashMap<>();

    private volatile String contextoCache;
    private volatile long contextoCacheTs;

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model:google/gemini-2.5-flash}")
    private String modeloPrincipal;

    @Value("${openrouter.fallback.models:inclusionai/ling-3.2-flash:free,baidu/ernie-5.5-flash-preview:free}")
    private String modelosRespaldo;

    @Value("${openrouter.max.tokens:500}")
    private int maxTokens;

    @Value("${openrouter.cobertura.ciudades:Bogota, Medellin, Cali, Barranquilla, Cartagena, Bucaramanga, Pereira, Santa Marta, Cucuta, Ibague, Villavicencio, Manizales}")
    private String ciudadesCobertura;

    public ChatIaController(GuiaRepository guiaRepository) {
        this.guiaRepository = guiaRepository;
    }

    @GetMapping(value = "/chat/ia", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> preguntar(
            @RequestParam(name = "q", required = false) String pregunta,
            @RequestParam(name = "h", required = false) String historial,
            HttpServletRequest request) {

        String ip = clientIp(request);
        if (!permitirPeticion(ip)) {
            return respuesta(429, "He recibido demasiadas preguntas seguidas. Espera un minuto y vuelve a intentarlo.",
                    null, false);
        }

        if (apiKey == null || apiKey.isBlank()) {
            return respuesta(200, "El servicio de inteligencia artificial no esta configurado en este servidor.",
                    null, false);
        }

        String texto = pregunta == null ? "" : pregunta.trim();
        if (texto.isEmpty()) {
            return respuesta(200, "Escribeme tu pregunta sobre envios, guias o rastreo y te ayudo.",
                    null, false);
        }
        if (texto.length() > MAX_PREGUNTA) {
            texto = texto.substring(0, MAX_PREGUNTA);
        }

        List<Map<String, String>> mensajesPrevios = parsearHistorial(historial);
        List<String> errores = new ArrayList<>();
        for (String modelo : modelosAConsultar()) {
            try {
                String respuesta = consultarModelo(modelo, texto, mensajesPrevios);
                if (respuesta != null && !respuesta.isBlank()) {
                    return respuesta(200, respuesta, modelo, true);
                }
                errores.add(modelo + ": respuesta vacia");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errores.add(modelo + ": interrumpido");
                break;
            } catch (Exception e) {
                errores.add(modelo + ": " + e.getMessage());
            }
        }

        System.err.println("[SAI-Bot] Sin respuesta de la IA -> " + String.join(" | ", errores));
        return respuesta(200,
                "Ahora mismo no puedo conectar con mi motor de IA. Puedes intentarlo de nuevo en un momento "
              + "o preguntarme por rastreo, guias, precios y tiempos de entrega con las opciones del chat.",
                null, false);
    }

    private ResponseEntity<Map<String, Object>> respuesta(int status, String texto, String modelo, boolean ok) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("respuesta", texto);
        body.put("ok", ok);
        if (modelo != null) {
            body.put("modelo", modelo);
        }
        return ResponseEntity.status(status)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    private Set<String> modelosAConsultar() {
        Set<String> modelos = new LinkedHashSet<>();
        if (modeloPrincipal != null && !modeloPrincipal.isBlank()) {
            modelos.add(modeloPrincipal.trim());
        }
        if (modelosRespaldo != null) {
            for (String m : modelosRespaldo.split(",")) {
                if (!m.isBlank()) {
                    modelos.add(m.trim());
                }
            }
        }
        return modelos;
    }

    private String consultarModelo(String modelo, String pregunta, List<Map<String, String>> historial)
            throws Exception {

        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", modelo);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", 0.4);

        ArrayNode mensajes = payload.putArray("messages");
        ObjectNode sistema = mensajes.addObject();
        sistema.put("role", "system");
        sistema.put("content", INSTRUCCIONES + "\n\nCONTEXTO DEL SISTEMA:\n" + contextoNegocio());

        for (Map<String, String> previo : historial) {
            ObjectNode m = mensajes.addObject();
            m.put("role", previo.get("role"));
            m.put("content", previo.get("content"));
        }

        ObjectNode usuario = mensajes.addObject();
        usuario.put("role", "user");
        usuario.put("content", pregunta);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("HTTP-Referer", "https://cargoexpress.local")
                .header("X-Title", "Cargo Express SAI-Bot")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload),
                        java.nio.charset.StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req,
                HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " " + recortar(resp.body(), 180));
        }

        JsonNode raiz = mapper.readTree(resp.body());
        JsonNode mensaje = raiz.path("choices").path(0).path("message");
        String contenido = mensaje.path("content").asText("");
        if (contenido.isBlank()) {
            // Algunos modelos gratuitos gastan los tokens en "reasoning" y dejan content vacio.
            contenido = mensaje.path("reasoning").asText("");
        }
        return limpiar(contenido);
    }

    private List<Map<String, String>> parsearHistorial(String historial) {
        List<Map<String, String>> lista = new ArrayList<>();
        if (historial == null || historial.isBlank()) {
            return lista;
        }
        try {
            JsonNode raiz = mapper.readTree(historial);
            if (!raiz.isArray()) {
                return lista;
            }
            int inicio = Math.max(0, raiz.size() - MAX_MENSAJES_HISTORIAL);
            for (int i = inicio; i < raiz.size(); i++) {
                JsonNode nodo = raiz.get(i);
                String rol = nodo.path("role").asText("");
                String contenido = nodo.path("content").asText("");
                if (!"user".equals(rol) && !"assistant".equals(rol)) {
                    continue;
                }
                if (contenido.isBlank()) {
                    continue;
                }
                if (contenido.length() > MAX_LARGO_HISTORIAL) {
                    contenido = contenido.substring(0, MAX_LARGO_HISTORIAL);
                }
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", rol);
                m.put("content", contenido);
                lista.add(m);
            }
        } catch (Exception ignored) {
            // Historial mal formado: se ignora y se responde solo con la pregunta actual.
        }
        return lista;
    }

    /** Contexto real de la base de datos, cacheado 2 minutos para no golpear la BD en cada mensaje. */
    private String contextoNegocio() {
        long ahora = System.currentTimeMillis();
        if (contextoCache != null && ahora - contextoCacheTs < 120_000L) {
            return contextoCache;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("- Ciudades de cobertura habituales: ").append(ciudadesCobertura).append("\n");
        try {
            long total = guiaRepository.count();
            sb.append("- Envios registrados actualmente en el sistema: ").append(total).append("\n");
            if (total > 0) {
                List<Object[]> origenes = guiaRepository.ciudadesOrigenFrecuentes(PageRequest.of(0, 5));
                List<Object[]> destinos = guiaRepository.ciudadesDestinoFrecuentes(PageRequest.of(0, 5));
                if (!origenes.isEmpty()) {
                    sb.append("- Ciudades de origen con mas envios: ").append(formatearCiudades(origenes)).append("\n");
                }
                if (!destinos.isEmpty()) {
                    sb.append("- Ciudades de destino con mas envios: ").append(formatearCiudades(destinos)).append("\n");
                }
            } else {
                sb.append("- No hay envios registrados todavia, por lo que no existen estadisticas reales de ciudades.\n");
            }
        } catch (Exception e) {
            sb.append("- No se pudieron leer las estadisticas de envios en este momento.\n");
        }

        String contexto = sb.toString();
        contextoCache = contexto;
        contextoCacheTs = ahora;
        return contexto;
    }

    private static String formatearCiudades(List<Object[]> filas) {
        StringBuilder sb = new StringBuilder();
        for (Object[] fila : filas) {
            String ciudad = fila[0] == null ? "Sin ciudad" : String.valueOf(fila[0]);
            String cantidad = fila[1] == null ? "0" : String.valueOf(fila[1]);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ciudad).append(" (").append(cantidad).append(")");
        }
        return sb.toString();
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\r\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max) + "...";
    }

    private boolean permitirPeticion(String ip) {
        long ahora = System.currentTimeMillis();
        Deque<Long> marcas = peticionesPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > 60_000L) {
                marcas.pollFirst();
            }
            if (marcas.size() >= LIMITE_POR_MINUTO) {
                return false;
            }
            marcas.addLast(ahora);
            return true;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
