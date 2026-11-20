package com.example.guias.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.GuiaService;

@Controller
public class MenuController {

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/menu")
    public String mostrarMenu(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        // Los clientes tienen su propio portal; no usan el panel operativo.
        if (!esAdminORepartidor(authentication)) {
            return "redirect:/cliente";
        }

        try {
            String username = authentication.getName();
            String rol = authentication.getAuthorities().toString();
            List<Guia> guias;

            if (esAdminORepartidor(authentication)) {
                guias = guiaService.obtenerTodas();
            } else {
                Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();
                    guias = guiaService.obtenerPorUsuario(usuario);
                } else {
                    model.addAttribute("error", "No se encontró información del usuario autenticado.");
                    guias = List.of();
                }
            }

            model.addAttribute("rol", rol);
            model.addAttribute("guias", guias);
            model.addAttribute("username", username);

            // Calcular Estadísticas del Usuario
            long totalEnvios = guias.size();
            long enTransito = guias.stream()
                    .filter(g -> EstadoGuia.es(g.getEstado(), EstadoGuia.EN_TRANSITO))
                    .count();
            long entregados = guias.stream()
                    .filter(g -> EstadoGuia.es(g.getEstado(), EstadoGuia.ENTREGADO))
                    .count();
            double totalInvertido = guias.stream().mapToDouble(Guia::getPrecio).sum();
            double totalPeso = guias.stream().mapToDouble(Guia::getPesoKg).sum();

            model.addAttribute("totalEnvios", totalEnvios);
            model.addAttribute("enTransito", enTransito);
            model.addAttribute("entregados", entregados);
            model.addAttribute("totalInvertido", totalInvertido);
            model.addAttribute("totalPeso", totalPeso);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error interno: " + e.getMessage());
        }
        return "menu";
    }

    
    @GetMapping("/dashboard")
    public String mostrarDashboard(Authentication authentication, Model model) {
        // Verificación de seguridad básica
        if (authentication == null) {
            return "redirect:/login";
        }

        if (!esAdminORepartidor(authentication)) {
            return "redirect:/cliente";
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("rol", authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_CLIENTE"));

        return "dashboard";
    }

    private boolean esAdminORepartidor(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_REPARTIDOR"));
    }
}