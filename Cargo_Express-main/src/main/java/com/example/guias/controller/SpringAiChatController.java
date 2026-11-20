
package com.example.guias.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.example.guias.ai.GuiaTools;
import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.service.ChatRagService;
import com.example.guias.service.GuiaService;
import com.example.guias.service.SesionChatService;
import com.example.guias.service.CodigoOtpService;
import com.example.guias.ai.CalculadoraTools;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.guias.repository.UsuarioRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@ConditionalOnProperty(name = "sai.rag.enabled", havingValue = "true", matchIfMissing = true)
@RestController
public class SpringAiChatController {

        private static final Logger log = LoggerFactory.getLogger(SpringAiChatController.class);

        private final ChatRagService chatRagService;
	private final CalculadoraTools calculadoraTools;
        private final GuiaTools guiaTools;
        private final CodigoOtpService codigoOtpService;
        private final SesionChatService sesionChatService;
        private final UsuarioRepository usuarioRepository;
        private final GuiaService guiaService;

    public SpringAiChatController(
            ChatRagService chatRagService,
            CalculadoraTools calculadoraTools,
	    GuiaTools guiaTools,
            CodigoOtpService codigoOtpService,
            SesionChatService sesionChatService,
            UsuarioRepository usuarioRepository,
            GuiaService guiaService) {

        this.chatRagService = chatRagService;
        this.calculadoraTools = calculadoraTools;
	this.guiaTools = guiaTools;
        this.codigoOtpService = codigoOtpService;
        this.sesionChatService = sesionChatService;
        this.usuarioRepository = usuarioRepository;
        this.guiaService = guiaService;
    }

    // Limite por IP (el endpoint es publico y consume un LLM de pago)
    private static final int LIMITE_POR_MINUTO = 25;
    private static final int MAX_PREGUNTA = 1000;
    private final Map<String, long[]> ventanas = new ConcurrentHashMap<>();

    private boolean excedeLimite(HttpServletRequest request) {
        long ahora = System.currentTimeMillis();
        if (ventanas.size() > 5000) {
            ventanas.entrySet().removeIf(e -> ahora - e.getValue()[0] > 60_000L);
        }
        long[] v = ventanas.compute(request.getRemoteAddr(), (ip, actual) -> {
            if (actual == null || ahora - actual[0] > 60_000L) {
                return new long[]{ahora, 1};
            }
            actual[1]++;
            return actual;
        });
        return v[1] > LIMITE_POR_MINUTO;
    }

    // 1. Consultas generales publicas mediante RAG.
    @GetMapping("/chat/ia/spring-ai")
    public ResponseEntity<Map<String, Object>> preguntarPublico(
            @RequestParam("q") String pregunta,
            HttpServletRequest request) {

        if (pregunta == null || pregunta.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("ok", false,
                            "respuesta", "Escribe una pregunta."));
        }

        if (pregunta.length() > MAX_PREGUNTA) {
            return ResponseEntity.badRequest().body(
                    Map.of("ok", false,
                            "respuesta", "La pregunta es demasiado larga."));
        }

        if (excedeLimite(request)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    Map.of("ok", false,
                            "respuesta", "Demasiadas consultas seguidas. Intenta en un minuto."));
        }

        try {
            String respuesta = chatRagService.preguntar(
                    pregunta,
                    calculadoraTools,
                    guiaTools);

            return ResponseEntity.ok(
                    Map.of("ok", true, "respuesta", respuesta));

        } catch (Exception e) {
            log.error("Error consultando SAI-Bot (Spring AI)", e);
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("ok", false,
                            "respuesta", "No fue posible consultar SAI-Bot."));
        }
    }

    // 2. Solicitar código OTP al correo del cliente.
    @PostMapping("/chat/ia/enviar-codigo")
    public ResponseEntity<Map<String, Object>> enviarCodigo(
            @RequestParam("email") String email,
            HttpSession session) {

        if (email == null || email.isBlank()
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(
                    Map.of("ok", false,
                            "respuesta", "Ingresa un correo electrónico válido."));
        }

        try {
            codigoOtpService.enviarCodigo(email.trim(), session);

            return ResponseEntity.ok(
                    Map.of("ok", true,
                            "respuesta",
                            "Si el correo corresponde a una cuenta habilitada, recibirás un código de verificación. Revisa tu bandeja de entrada."));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    Map.of("ok", false,
                            "respuesta", e.getMessage()));

        } catch (MessagingException e) {
            codigoOtpService.limpiarCodigo(session);

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("ok", false,
                            "respuesta",
                            "No fue posible enviar el código. Intenta nuevamente más tarde."));
        }
    }

    // 3. Verificar OTP y autorizar al cliente.
    @PostMapping("/chat/ia/verificar-codigo")
    public ResponseEntity<Map<String, Object>> verificarCodigo(
            @RequestParam("codigo") String codigo,
            HttpSession session) {

        Optional<Long> usuarioId =
                codigoOtpService.verificarCodigo(codigo, session);

        if (usuarioId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("ok", false,
                            "respuesta",
                            "El código no es válido, ha vencido o alcanzaste el límite de intentos."));
        }

        Optional<Usuario> usuario =
                usuarioRepository.findById(usuarioId.get());

        if (usuario.isEmpty() || !usuario.get().isAprobado()) {
            sesionChatService.limpiar(session);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("ok", false,
                            "respuesta",
                            "La cuenta no está habilitada para consultar guías."));
        }

        sesionChatService.autorizar(
                session, usuario.get().getId());

        return ResponseEntity.ok(
                Map.of("ok", true,
                        "respuesta",
                        "¡Correo verificado correctamente! Ahora puedes ingresar el número de tu guía.",
                        "nombre", usuario.get().getNombre()));
    }

    // 4. Consultar únicamente guías del cliente autorizado.
    @PostMapping("/chat/ia/consultar-guia")
    public ResponseEntity<Map<String, Object>> consultarGuia(
            @RequestParam("numeroGuia") Long numeroGuia,
            HttpSession session) {

        if (numeroGuia == null || numeroGuia <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("ok", false,
                            "respuesta", "Ingresa un número de guía válido."));
        }

        Long usuarioId =
                sesionChatService.obtenerClienteAutorizado(session);

        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("ok", false,
                            "respuesta",
                            "Primero debes verificar tu correo electrónico."));
        }

        Optional<Usuario> usuario =
                usuarioRepository.findById(usuarioId);

        if (usuario.isEmpty() || !usuario.get().isAprobado()) {
            sesionChatService.limpiar(session);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("ok", false,
                            "respuesta",
                            "La autorización ya no está disponible. Verifica tu correo nuevamente."));
        }

        Optional<Guia> guia =
                guiaService.buscarPorIdYUsuario(
                        numeroGuia, usuario.get());

        if (guia.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("ok", false,
                            "respuesta",
                            "No se encontró una guía asociada a tu cuenta."));
        }

        Guia resultado = guia.get();

        Map<String, Object> datosGuia = new LinkedHashMap<>();

        datosGuia.put("numero", resultado.getId());
        datosGuia.put("estado", resultado.getEstado());
        datosGuia.put("origen", resultado.getCiudadOrigen());
        datosGuia.put("destino", resultado.getCiudadDestino());

        datosGuia.put("fechaCreacion",
                resultado.getFechaCreacion() == null
                        ? null
                        : resultado.getFechaCreacion().toString());

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("ok", true);
        respuesta.put("respuesta",
                "Encontré tu guía. Estos son sus datos:");
        respuesta.put("guia", datosGuia);

        return ResponseEntity.ok(respuesta);
    }
}
