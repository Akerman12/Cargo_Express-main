package com.example.guias.controller;

import com.example.guias.dto.RutaResumenDTO;
import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.GuiaService;
import com.example.guias.service.MongoSyncService;
import com.example.guias.service.RutaService;
import com.example.guias.service.VehiculoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reparto")
@PreAuthorize("hasAnyRole('ROLE_REPARTIDOR', 'ROLE_ADMIN')")
public class RepartidorController {

    private static final Set<String> ESTADOS_OPERATIVOS = EstadoGuia.OPERATIVOS_REPARTIDOR;

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RutaService rutaService;

    @Autowired
    private VehiculoService vehiculoService;

    @Autowired(required = false)
    private MongoSyncService mongoSyncService;

    @GetMapping("")
    public String index(Model model, Authentication auth) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("guias", List.of());
            model.addAttribute("rutaEntrega", List.of());
            model.addAttribute("rutaRecogida", List.of());
            model.addAttribute("regionesEntrega", Set.of());
            model.addAttribute("regionesRecogida", Set.of());
            return "reparto";
        }

        Usuario currentUser = usuarioOpt.get();
        if (currentUser.getRegionOperacion() != null) {
            model.addAttribute("regionRepartidor", currentUser.getRegionOperacion().getEtiqueta());
        }
        vehiculoService.buscarPorRepartidor(currentUser).ifPresent(v -> model.addAttribute("vehiculo", v));
        Optional<RutaResumenDTO> rutaActivaOpt = rutaService.obtenerRutaActivaPorRepartidor(currentUser);
        model.addAttribute("rutaActiva", rutaActivaOpt.orElse(null));

        List<Guia> todasAsignadas = guiaService.obtenerPorRepartidorAsignado(currentUser).stream()
                .filter(g -> !EstadoGuia.es(g.getEstado(), EstadoGuia.CANCELADO))
                .collect(Collectors.toList());

        // Fase de recogida: aún no están en sucursal
        List<Guia> rutaRecogida = todasAsignadas.stream()
                .filter(g -> g.getEstado() == null || EstadoGuia.esUnoDe(g.getEstado(),
                        EstadoGuia.PENDIENTE, EstadoGuia.RECOGIDO, EstadoGuia.NO_RECOGIDO))
                .sorted(Comparator.comparing(Guia::getOrdenRecogida, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Guia::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // Fase de entrega: ya ingresaron a la sucursal
        List<Guia> rutaEntrega = todasAsignadas.stream()
                .filter(g -> g.getEstado() != null && !EstadoGuia.esUnoDe(g.getEstado(),
                        EstadoGuia.PENDIENTE, EstadoGuia.RECOGIDO, EstadoGuia.NO_RECOGIDO))
                .sorted(Comparator.comparing(Guia::getOrdenEntrega, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Guia::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        Set<String> regionesEntrega = extraerRegiones(rutaEntrega, true);
        Set<String> regionesRecogida = extraerRegiones(rutaRecogida, false);

        model.addAttribute("guias", todasAsignadas);
        model.addAttribute("rutaEntrega", rutaEntrega);
        model.addAttribute("rutaRecogida", rutaRecogida);
        model.addAttribute("regionesEntrega", regionesEntrega);
        model.addAttribute("regionesRecogida", regionesRecogida);
        return "reparto";
    }

    @GetMapping("/ruta-activa")
    @ResponseBody
    public ResponseEntity<?> obtenerRutaActivaJson(Authentication auth) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("activa", false));
        }
        Optional<RutaResumenDTO> rutaOpt = rutaService.obtenerRutaActivaPorRepartidor(usuarioOpt.get());
        if (rutaOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("activa", false));
        }
        return ResponseEntity.ok(rutaOpt.get());
    }

    @GetMapping("/ruta-mapa")
    public String verMapaRutaCompleta(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/reparto";
        }
        Optional<RutaResumenDTO> rutaOpt = rutaService.obtenerRutaActivaPorRepartidor(usuarioOpt.get());
        if (rutaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No tienes una ruta multi-parada asignada todavía.");
            return "redirect:/reparto";
        }
        model.addAttribute("ruta", rutaOpt.get());
        return "reparto-ruta-mapa";
    }

    @PostMapping("/iniciar-ruta")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> iniciarRuta(Authentication auth) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Usuario no encontrado."));
        }
        Optional<RutaResumenDTO> rutaOpt = rutaService.iniciarRutaRepartidor(
                usuarioOpt.get(), auth.getName() + " (REPARTIDOR)");
        sincronizarMongoSeguro();

        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("sinRuta", rutaOpt.isEmpty());
        rutaOpt.ifPresent(r -> body.put("ruta", r));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/finalizar-ruta")
    public String finalizarRuta(@RequestParam(defaultValue = "false") boolean forzar,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/reparto";
        }
        try {
            rutaService.finalizarRutaRepartidor(usuarioOpt.get(), forzar);
            sincronizarMongoSeguro();
            redirectAttributes.addFlashAttribute("mensaje", "🏁 Ruta finalizada correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "⚠️ " + e.getMessage());
        }
        return "redirect:/reparto";
    }

    @GetMapping("/mapa/{id}")
    public String verMapa(@PathVariable Long id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
        Usuario currentUser = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (guiaOpt.isPresent() && tienePermisoSobreGuia(guiaOpt.get(), currentUser, auth)) {
            model.addAttribute("guia", guiaOpt.get());
            return "reparto-mapa";
        }
        redirectAttributes.addFlashAttribute("error", "Guía no encontrada o sin permiso de acceso.");
        return "redirect:/reparto";
    }

    @PostMapping("/actualizar-estado")
    @ResponseBody
    public ResponseEntity<String> actualizarEstado(@RequestParam Long id,
                                                 @RequestParam String estado,
                                                 Authentication auth) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
        Usuario currentUser = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (guiaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("NOT_FOUND");
        }
        Guia guia = guiaOpt.get();
        if (!tienePermisoSobreGuia(guia, currentUser, auth)) {
            return ResponseEntity.status(403).body("FORBIDDEN");
        }
        String nuevo = EstadoGuia.normalizar(estado);
        if (nuevo == null || !ESTADOS_OPERATIVOS.contains(nuevo)) {
            return ResponseEntity.badRequest().body("INVALID_STATUS");
        }
        if (!EstadoGuia.transicionValida(guia.getEstado(), nuevo)) {
            return ResponseEntity.badRequest().body("INVALID_TRANSITION");
        }
        try {
            guia.setEstado(nuevo);
            guia.setModificadoPor(auth.getName() + " (REPARTIDOR)");
            if (EstadoGuia.EN_SUCURSAL.equals(nuevo)) {
                // La guía ya está en la sucursal: sale de la lista del repartidor de la recogida
                guia.setRepartidorAsignado(null);
            }
            guiaService.guardarGuia(guia);
            rutaService.verificarCompletarRutaPorGuia(guia);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return ResponseEntity.internalServerError().body("ERROR: " + root.getMessage());
        }
        sincronizarMongoSeguro();
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/no-recogido")
    public String reportarNoRecogido(@RequestParam Long guiaId,
                                     @RequestParam String motivo,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(guiaId);
        Usuario currentUser = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (guiaOpt.isEmpty() || !tienePermisoSobreGuia(guiaOpt.get(), currentUser, auth)) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Guía no encontrada o sin permiso.");
            return "redirect:/reparto";
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Debes indicar el motivo por el que no se recogió.");
            return "redirect:/reparto";
        }
        Guia guia = guiaOpt.get();
        if (!EstadoGuia.transicionValida(guia.getEstado(), EstadoGuia.NO_RECOGIDO)) {
            redirectAttributes.addFlashAttribute("error", "⚠️ La guía #" + guiaId + " está en estado '"
                    + guia.getEstado() + "' y no puede marcarse como No Recogida.");
            return "redirect:/reparto";
        }
        guia.setEstado(EstadoGuia.NO_RECOGIDO);
        guia.setMotivoNovedad(motivo.trim());
        guia.setModificadoPor(auth.getName() + " (REPARTIDOR)");
        guiaService.guardarGuia(guia);
        rutaService.verificarCompletarRutaPorGuia(guia);
        sincronizarMongoSeguro();
        redirectAttributes.addFlashAttribute("mensaje",
                "📦 Guía SAI-" + guiaId + " marcada como No Recogida: " + motivo.trim());
        return "redirect:/reparto";
    }

    @PostMapping("/reportar-novedad")
    public String reportarNovedad(@RequestParam Long guiaId,
                                  @RequestParam String motivo,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(guiaId);
        Usuario currentUser = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (guiaOpt.isEmpty() || !tienePermisoSobreGuia(guiaOpt.get(), currentUser, auth)) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Guía no encontrada o sin permiso.");
            return "redirect:/reparto";
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Debes indicar el motivo de la novedad.");
            return "redirect:/reparto";
        }

        Guia guia = guiaOpt.get();
        if (!EstadoGuia.transicionValida(guia.getEstado(), EstadoGuia.NOVEDAD)) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Solo se reporta novedad en guías en tránsito (estado actual: "
                    + guia.getEstado() + "). Si no se pudo recoger, usa 'No recogido'.");
            return "redirect:/reparto";
        }
        guia.setEstado(EstadoGuia.NOVEDAD);
        guia.setMotivoNovedad(motivo.trim());
        guia.setModificadoPor(auth.getName() + " (REPARTIDOR)");
        guiaService.guardarGuia(guia);
        sincronizarMongoSeguro();

        redirectAttributes.addFlashAttribute("mensaje",
                "⚠️ Novedad reportada en guía SAI-" + guiaId + ": " + motivo.trim());
        return "redirect:/reparto";
    }

    @PostMapping("/finalizar-entrega")
    public String finalizarEntrega(@RequestParam Long guiaId,
                                   @RequestParam String firma,
                                   @RequestParam(required = false, defaultValue = "") String notas,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Optional<Guia> guiaOpt = guiaService.buscarPorId(guiaId);
            Usuario currentUser = auth != null ? usuarioRepository.findByUsername(auth.getName()).orElse(null) : null;
            if (guiaOpt.isPresent()) {
                Guia guia = guiaOpt.get();
                if (auth != null && !tienePermisoSobreGuia(guia, currentUser, auth)) {
                    redirectAttributes.addFlashAttribute("error", "❌ No tienes permiso sobre esta guía.");
                    return "redirect:/reparto";
                }

                if (firma == null || firma.trim().isEmpty() || firma.length() < 100) {
                    redirectAttributes.addFlashAttribute("error", "⚠️ La firma es obligatoria para finalizar la entrega.");
                    return "redirect:/reparto";
                }

                if (!EstadoGuia.esUnoDe(guia.getEstado(), EstadoGuia.EN_TRANSITO, EstadoGuia.EN_SUCURSAL, EstadoGuia.NOVEDAD)) {
                    redirectAttributes.addFlashAttribute("error", "⚠️ La guía #" + guiaId
                            + " aún no puede entregarse (estado actual: " + guia.getEstado() + ").");
                    return "redirect:/reparto";
                }

                guia.setEstado(EstadoGuia.ENTREGADO);
                guia.setFirmaEntrega(firma);
                guia.setNotasEntrega(notas);
                guia.setFechaEntrega(LocalDateTime.now());

                String modifierName = (auth != null) ? auth.getName() : "SISTEMA";
                guia.setModificadoPor(modifierName + " (REPARTIDOR)");

                guiaService.guardarGuia(guia);
                rutaService.verificarCompletarRutaPorGuia(guia);
                sincronizarMongoSeguro();
                redirectAttributes.addFlashAttribute("mensaje", "✅ Entrega de la guía #" + guiaId + " confirmada exitosamente.");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Error: La guía #" + guiaId + " no existe en el sistema.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error crítico al guardar la firma: " + e.getMessage());
        }

        return "redirect:/reparto";
    }

    // ------------------------------------------------------------------ helpers

    private boolean esAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private boolean tienePermisoSobreGuia(Guia guia, Usuario currentUser, Authentication auth) {
        if (esAdmin(auth)) return true;
        return currentUser != null
                && guia.getRepartidorAsignado() != null
                && guia.getRepartidorAsignado().getId().equals(currentUser.getId());
    }

    private Set<String> extraerRegiones(List<Guia> guias, boolean destino) {
        Set<String> regiones = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Guia g : guias) {
            String depto = destino ? g.getDepartamentoDestino() : g.getDepartamentoOrigen();
            String ciudad = destino ? g.getCiudadDestino() : g.getCiudadOrigen();
            String valor = (depto != null && !depto.isBlank()) ? depto.trim()
                    : ((ciudad != null && !ciudad.isBlank()) ? ciudad.trim() : null);
            if (valor != null) {
                regiones.add(valor);
            }
        }
        return regiones;
    }

    private void sincronizarMongoSeguro() {
        if (mongoSyncService != null) {
            try {
                mongoSyncService.sincronizarGuias();
            } catch (Exception e) {
                System.err.println("⚠️ Error no crítico al sincronizar guías con MongoDB: " + e.getMessage());
            }
        }
    }
}
