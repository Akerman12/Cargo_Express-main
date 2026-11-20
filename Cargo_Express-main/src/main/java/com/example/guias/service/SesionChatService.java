
package com.example.guias.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SesionChatService {

    private static final String CLIENTE_ID = "SAI_CLIENTE_ID";
    private static final String EXPIRA = "SAI_CLIENTE_EXPIRA";
    private static final String INTENTOS = "SAI_INTENTOS";

    private static final long DURACION_SEGUNDOS = 600;
    private static final int MAX_INTENTOS = 5;

    public boolean puedeIntentar(HttpSession session) {
        Integer intentos = (Integer) session.getAttribute(INTENTOS);
        return intentos == null || intentos < MAX_INTENTOS;
    }

    public void registrarIntento(HttpSession session) {
        Integer intentos = (Integer) session.getAttribute(INTENTOS);
        session.setAttribute(INTENTOS, intentos == null ? 1 : intentos + 1);
    }

    public void autorizar(HttpSession session, Long usuarioId) {
        session.setAttribute(CLIENTE_ID, usuarioId);
        session.setAttribute(
            EXPIRA,
            Instant.now().getEpochSecond() + DURACION_SEGUNDOS
        );
        session.removeAttribute(INTENTOS);
    }

    public Long obtenerClienteAutorizado(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute(CLIENTE_ID);
        Long expiracion = (Long) session.getAttribute(EXPIRA);

        if (usuarioId == null || expiracion == null
                || Instant.now().getEpochSecond() >= expiracion) {
            limpiar(session);
            return null;
        }

        return usuarioId;
    }

    public void limpiar(HttpSession session) {
        session.removeAttribute(CLIENTE_ID);
        session.removeAttribute(EXPIRA);
        session.removeAttribute(INTENTOS);
    }
}
