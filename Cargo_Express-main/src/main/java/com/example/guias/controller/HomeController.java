

package com.example.guias.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean esAdminORepartidor = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol ->
                        rol.equals("ROLE_ADMIN") ||
                        rol.equals("ROLE_REPARTIDOR"));

        if (esAdminORepartidor) {
            return "redirect:/menu";
        }

        return "redirect:/cliente";
    }
}
