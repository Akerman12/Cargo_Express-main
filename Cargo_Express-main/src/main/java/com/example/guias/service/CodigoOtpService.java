
package com.example.guias.service;

import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class CodigoOtpService {

    private static final String USUARIO_ID = "SAI_OTP_USUARIO_ID";
    private static final String CODIGO_HASH = "SAI_OTP_HASH";
    private static final String EXPIRACION = "SAI_OTP_EXPIRA";
    private static final String INTENTOS = "SAI_OTP_INTENTOS";
    private static final String ENVIADO = "SAI_OTP_ENVIADO";

    private static final long DURACION_SEGUNDOS = 300;
    private static final long ESPERA_REENVIO = 60;
    private static final int MAX_INTENTOS = 5;

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public CodigoOtpService(
            UsuarioRepository usuarioRepository,
            EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    public void enviarCodigo(String email, HttpSession session)
            throws MessagingException {

        long ahora = Instant.now().getEpochSecond();

        Long ultimoEnvio = (Long) session.getAttribute(ENVIADO);

        if (ultimoEnvio != null
                && ahora - ultimoEnvio < ESPERA_REENVIO) {
            throw new IllegalStateException(
                    "Debes esperar antes de solicitar otro código.");
        }

        Optional<Usuario> usuario = usuarioRepository
                .findByEmailIgnoreCase(email.trim())
                .filter(Usuario::isAprobado);

        if (usuario.isEmpty()) {
            limpiarCodigo(session);
            return;
        }

        String codigo = String.format(
                "%06d", random.nextInt(1_000_000));

        String asunto = "Código de verificación - Carga Express";

        String mensaje = """
                Hola,

                Tu código de verificación para SAI-Bot de Carga Express es:

                %s

                Este código vence en cinco minutos.
                Si no solicitaste este código, puedes ignorar este mensaje.

                Equipo Carga Express
                """.formatted(codigo);

        emailService.enviarCorreo(
                email.trim(), asunto, mensaje);

        session.setAttribute(USUARIO_ID, usuario.get().getId());
        session.setAttribute(CODIGO_HASH, calcularHash(codigo));
        session.setAttribute(EXPIRACION, ahora + DURACION_SEGUNDOS);
        session.setAttribute(INTENTOS, 0);
        session.setAttribute(ENVIADO, ahora);
    }

    public Optional<Long> verificarCodigo(
            String codigo, HttpSession session) {

        if (codigo == null || !codigo.matches("\\d{6}")) {
            return Optional.empty();
        }

        Long usuarioId = (Long) session.getAttribute(USUARIO_ID);
        String hashGuardado =
                (String) session.getAttribute(CODIGO_HASH);
        Long expiracion =
                (Long) session.getAttribute(EXPIRACION);

        if (usuarioId == null || hashGuardado == null
                || expiracion == null) {
            limpiarCodigo(session);
            return Optional.empty();
        }

        if (Instant.now().getEpochSecond() >= expiracion) {
            limpiarCodigo(session);
            return Optional.empty();
        }

        Integer intentos = (Integer) session.getAttribute(INTENTOS);
        int cantidad = intentos == null ? 0 : intentos;

        if (cantidad >= MAX_INTENTOS) {
            limpiarCodigo(session);
            return Optional.empty();
        }

        session.setAttribute(INTENTOS, cantidad + 1);

        String hashIngresado = calcularHash(codigo);

        if (!MessageDigest.isEqual(
                hashGuardado.getBytes(StandardCharsets.UTF_8),
                hashIngresado.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        limpiarCodigo(session);
        return Optional.of(usuarioId);
    }

    public void limpiarCodigo(HttpSession session) {
        session.removeAttribute(USUARIO_ID);
        session.removeAttribute(CODIGO_HASH);
        session.removeAttribute(EXPIRACION);
        session.removeAttribute(INTENTOS);
        session.removeAttribute(ENVIADO);
    }

    private String calcularHash(String codigo) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] resultado = digest.digest(
                    codigo.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(resultado);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "No fue posible procesar el código.", e);
        }
    }
}
 