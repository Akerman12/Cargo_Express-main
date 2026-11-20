package com.example.guias.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Decide a donde entrar despues del login (formulario o Google):
 *  - Clientes -> /cliente (portal de clientes)
 *  - Admin y repartidores -> /menu (panel operativo)
 */
@Component
public class RolSuccessHandler implements AuthenticationSuccessHandler {

    private static final Set<String> ROLES_INTERNOS = Set.of("ROLE_ADMIN", "ROLE_REPARTIDOR");

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        boolean personalInterno = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_INTERNOS::contains);

        String destino = personalInterno ? "/menu" : "/cliente";
        response.sendRedirect(request.getContextPath() + destino);
    }
}
