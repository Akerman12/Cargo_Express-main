package com.example.guias.controller;

import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.MongoSyncService; // IMPORTANTE
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.example.guias.service.GuiaService guiaService;

    @Autowired
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    // INYECCIÓN DEL SERVICIO DE MONGO
    @Autowired
    private MongoSyncService mongoSyncService;

    @Autowired
    private com.example.guias.service.VehiculoService vehiculoService;

    private static final String SUPREMO = "admin@saicargo.com";

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);
        return "admin-usuarios";
    }

    @PostMapping("/usuarios/aprobar/{id}")
    public String aprobarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioRepository.findById(id).ifPresent(u -> {
            if (u.getRol() == com.example.guias.model.Rol.ROLE_REPARTIDOR && u.getRegionOperacion() == null) {
                redirectAttributes.addFlashAttribute("error", "Antes de aprobar a " + u.getUsername()
                        + " asígnale una región de operación (botón Editar).");
                return;
            }
            u.setAprobado(true);
            usuarioRepository.save(u);
            
            // ---> SINCRONIZAR CON MONGODB <---
            mongoSyncService.sincronizarUsuarios();
            
            redirectAttributes.addFlashAttribute("mensaje", "Usuario " + u.getUsername() + " aprobado correctamente.");
        });
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/bloquear/{id}")
    public String bloquearUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setAprobado(false);
            usuarioRepository.save(u);
            
            // ---> SINCRONIZAR CON MONGODB <---
            mongoSyncService.sincronizarUsuarios();
            
            redirectAttributes.addFlashAttribute("error", "Usuario " + u.getUsername() + " bloqueado.");
        });
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        usuarioRepository.findById(id).ifPresentOrElse(
            u -> model.addAttribute("usuario", u),
            () -> redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.")
        );
        model.addAttribute("regiones", com.example.guias.model.Region.values());
        return "admin-usuarios-editar";
    }

    @PostMapping("/usuarios/editar")
    public String procesarEdicion(@ModelAttribute Usuario usuario, 
                                 @RequestParam(value = "newPassword", required = false) String newPassword,
                                 org.springframework.security.core.Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        
        String currentUser = auth.getName();

        usuarioRepository.findById(usuario.getId()).ifPresent(u -> {
            if (u.getRol() == com.example.guias.model.Rol.ROLE_ADMIN && !currentUser.equals(SUPREMO) && usuario.getRol() != com.example.guias.model.Rol.ROLE_ADMIN) {
                redirectAttributes.addFlashAttribute("error", "Solo el Admin Supremo puede degradar a otros administradores.");
                return;
            }

            u.setNombre(usuario.getNombre());
            u.setUsername(usuario.getUsername());

            // Si deja de ser repartidor, su furgoneta debe quedar libre (y no puede tener paquetes activos)
            if (u.getRol() == com.example.guias.model.Rol.ROLE_REPARTIDOR
                    && usuario.getRol() != com.example.guias.model.Rol.ROLE_REPARTIDOR) {
                java.util.Optional<com.example.guias.model.Vehiculo> vOpt = vehiculoService.buscarPorRepartidor(u);
                if (vOpt.isPresent()) {
                    String bloqueo = vehiculoService.motivoNoLiberable(vOpt.get());
                    if (bloqueo != null) {
                        redirectAttributes.addFlashAttribute("error", "No se puede cambiar el rol. " + bloqueo);
                        return;
                    }
                    vehiculoService.liberarVehiculoDe(u);
                }
            }

            // Región de operación: solo aplica a repartidores y no puede cambiar con paquetes asignados
            com.example.guias.model.Region nuevaRegion = usuario.getRol() == com.example.guias.model.Rol.ROLE_REPARTIDOR
                    ? usuario.getRegionOperacion() : null;
            if (usuario.getRol() == com.example.guias.model.Rol.ROLE_REPARTIDOR && nuevaRegion == null) {
                redirectAttributes.addFlashAttribute("error", "El repartidor debe tener una región de operación.");
                return;
            }
            if (u.getRegionOperacion() != null && u.getRegionOperacion() != nuevaRegion
                    && !guiaService.obtenerPorRepartidorAsignado(u).isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se puede cambiar la región de " + u.getUsername()
                        + " mientras tenga guías asignadas.");
                return;
            }
            u.setRegionOperacion(nuevaRegion);

            u.setRol(usuario.getRol());
            u.setEmail(usuario.getEmail());
            
            if (newPassword != null && !newPassword.isBlank()) {
                u.setPassword(passwordEncoder.encode(newPassword));
            }
            
            usuarioRepository.save(u);
            
            // ---> SINCRONIZAR CON MONGODB <---
            mongoSyncService.sincronizarUsuarios();
            
            redirectAttributes.addFlashAttribute("mensaje", "Usuario " + u.getUsername() + " actualizado correctamente.");
        });
        
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, 
                                 org.springframework.security.core.Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        
        String currentUser = auth.getName();

        usuarioRepository.findById(id).ifPresentOrElse(u -> {
            if (u.getRol() == com.example.guias.model.Rol.ROLE_ADMIN && !currentUser.equals(SUPREMO)) {
                redirectAttributes.addFlashAttribute("error", "Solo el Admin Supremo puede eliminar a otros administradores.");
            } else if (u.getUsername().equals(currentUser)) {
                redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propia cuenta.");
            } else {
                // Si es repartidor con furgoneta: bloquear si tiene paquetes activos; si no, liberar la furgoneta
                java.util.Optional<com.example.guias.model.Vehiculo> vOpt = vehiculoService.buscarPorRepartidor(u);
                if (vOpt.isPresent()) {
                    String bloqueo = vehiculoService.motivoNoLiberable(vOpt.get());
                    if (bloqueo != null) {
                        redirectAttributes.addFlashAttribute("error", "No se puede eliminar al usuario. " + bloqueo);
                        return;
                    }
                    vehiculoService.liberarVehiculoDe(u);
                }
                usuarioRepository.delete(u);
                
                // ---> SINCRONIZAR CON MONGODB <---
                // En caso de eliminar usuarios, ejecutamos la sincronización para actualizar la lista
                mongoSyncService.sincronizarUsuarios();
                
                redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado permanentemente.");
            }
        }, () -> redirectAttributes.addFlashAttribute("error", "Usuario no encontrado."));
        
        return "redirect:/admin/usuarios";
    }

    @Autowired
    private com.example.guias.service.WekaService wekaService;

    @GetMapping("/warroom")
    public String mostrarWarRoom(Model model, org.springframework.security.core.Authentication auth) {
        boolean isSupremo = auth.getName().equals(SUPREMO);
        model.addAttribute("isSupremo", isSupremo);
        List<com.example.guias.model.Guia> guias = guiaService.obtenerTodas();
        double distanciaTotal = guiaService.calcularDistanciaTotal();
        
        double co2TotalKg = guias.stream()
            .mapToDouble(g -> {
                double km = com.example.guias.util.DistanciaUtil.obtenerDistanciaGuia(g);
                return wekaService.predecirCO2(km, g.getPesoKg());
            })
            .sum();

        model.addAttribute("totalGuias", guias.size());
        model.addAttribute("distanciaTotal", Math.round(distanciaTotal));
        model.addAttribute("guiasRecientes", guias.stream().limit(5).toList());
        model.addAttribute("co2Ahorrado", Math.round(co2TotalKg * 10.0) / 10.0);
        
        return "admin-warroom";
    }

    @GetMapping("/weka-models")
    public String mostrarWekaModels(Model model) {
        java.util.List<String> ciudades = com.example.guias.util.DistanciaUtil.getCiudades().stream()
                .filter(c -> !c.matches(".*[ÁÉÍÓÚ].*"))
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("ciudades", ciudades);
        return "admin-weka-models";
    }
}