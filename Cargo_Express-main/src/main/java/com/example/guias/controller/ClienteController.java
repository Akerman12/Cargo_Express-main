package com.example.guias.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

/**
 * Portal exclusivo para clientes. Es una experiencia distinta del panel
 * administrativo (/menu): resumen personal, nivel de cliente, envios
 * recientes, pagos pendientes y actividad.
 */
@Controller
public class ClienteController {

    private static final int RECIENTES = 6;

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/cliente")
    public String portalCliente(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        // El personal administrativo sigue usando el panel operativo.
        if (esAdminORepartidor(authentication)) {
            return "redirect:/menu";
        }

        List<Guia> guias = new ArrayList<>();
        String nombre = authentication.getName();
        String email = authentication.getName();

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(authentication.getName());
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            guias = new ArrayList<>(guiaService.obtenerPorUsuario(usuario));
            if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) {
                nombre = usuario.getNombre();
            }
            if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
                email = usuario.getEmail();
            }
        }

        guias.sort(Comparator.comparing(
                Guia::getFechaCreacion,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long totalEnvios = guias.size();
        long enTransito = guias.stream()
                .filter(g -> EstadoGuia.es(g.getEstado(), EstadoGuia.EN_TRANSITO))
                .count();
        long entregados = guias.stream()
                .filter(g -> EstadoGuia.es(g.getEstado(), EstadoGuia.ENTREGADO))
                .count();
        long pendientesPago = guias.stream()
                .filter(g -> !"PAGADO".equals(g.getEstadoPago()))
                .count();
        double totalInvertido = guias.stream().mapToDouble(Guia::getPrecio).sum();
        double montoPendiente = guias.stream()
                .filter(g -> !"PAGADO".equals(g.getEstadoPago()))
                .mapToDouble(Guia::getPrecio)
                .sum();

        int cantidad = (int) totalEnvios;
        int[] base = {0, 10, 30, 60};
        int[] techo = {10, 30, 60, 60};
        String[] niveles = {"Bronce", "Plata", "Oro", "Platino"};
        int indice = cantidad >= 60 ? 3 : cantidad >= 30 ? 2 : cantidad >= 10 ? 1 : 0;
        int progreso = indice == 3 ? 100
                : (int) Math.round(((cantidad - base[indice]) * 100.0) / (techo[indice] - base[indice]));
        progreso = Math.max(0, Math.min(100, progreso));
        int faltantes = indice == 3 ? 0 : Math.max(0, techo[indice] - cantidad);

        List<Guia> recientes = guias.size() > RECIENTES ? guias.subList(0, RECIENTES) : guias;
        List<Guia> porPagar = guias.stream()
                .filter(g -> !"PAGADO".equals(g.getEstadoPago()))
                .limit(3)
                .toList();

        model.addAttribute("nombre", nombre);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("email", email);
        model.addAttribute("iniciales", iniciales(nombre));
        model.addAttribute("totalEnvios", totalEnvios);
        model.addAttribute("enTransito", enTransito);
        model.addAttribute("entregados", entregados);
        model.addAttribute("pendientesPago", pendientesPago);
        model.addAttribute("totalInvertido", totalInvertido);
        model.addAttribute("montoPendiente", montoPendiente);
        model.addAttribute("nivel", niveles[indice]);
        model.addAttribute("progreso", progreso);
        model.addAttribute("faltantes", faltantes);
        model.addAttribute("siguienteNivel", indice == 3 ? null : niveles[indice + 1]);
        model.addAttribute("recientes", recientes);
        model.addAttribute("porPagar", porPagar);

        return "portal-cliente";
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "C";
        }
        String limpio = nombre.trim();
        String[] partes = limpio.split("[\\s._-]+");
        if (partes.length >= 2) {
            return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
        return limpio.substring(0, Math.min(2, limpio.length())).toUpperCase(Locale.ROOT);
    }

    private boolean esAdminORepartidor(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol -> rol.equals("ROLE_ADMIN") || rol.equals("ROLE_REPARTIDOR"));
    }
}
