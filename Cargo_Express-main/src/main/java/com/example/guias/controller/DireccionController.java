package com.example.guias.controller;

import com.example.guias.model.Direccion;
import com.example.guias.model.Usuario;
import com.example.guias.repository.DireccionRepository;
import com.example.guias.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/direcciones")
public class DireccionController {

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private com.example.guias.service.MongoSyncService mongoSyncService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String listarDirecciones(Model model, Authentication auth) {
        String username = auth.getName();
        Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
        
        java.util.List<Direccion> direcciones = new java.util.ArrayList<>();
        if (userOpt.isPresent()) {
            direcciones = direccionRepository.findByUsuario(userOpt.get());
        }
        model.addAttribute("direcciones", direcciones);
        return "mis-direcciones";
    }

    @PostMapping("/guardar")
    @org.springframework.transaction.annotation.Transactional
    public String guardarDireccion(
            @RequestParam String alias,
            @RequestParam String nombreContacto,
            @RequestParam String direccion,
            @RequestParam String ciudad,
            @RequestParam String departamento,
            @RequestParam String telefono,
            Authentication auth, 
            RedirectAttributes redirectAttributes) {
        try {
            String identifier = auth.getName();
            // Buscar por username o por email (caso OAuth2)
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(identifier);
            if (userOpt.isEmpty()) {
                userOpt = usuarioRepository.findAll().stream()
                        .filter(u -> identifier.equals(u.getEmail()))
                        .findFirst();
            }
            
            if (userOpt.isPresent()) {
                Direccion novaDir = new Direccion();
                novaDir.setAlias(alias);
                novaDir.setNombreContacto(nombreContacto);
                novaDir.setDireccion(direccion);
                novaDir.setCiudad(ciudad);
                novaDir.setDepartamento(departamento);
                novaDir.setTelefono(telefono);
                novaDir.setUsuario(userOpt.get());
                
            direccionRepository.save(novaDir);
                mongoSyncService.sincronizarDirecciones(); // <-- AGREGA ESTO
                redirectAttributes.addFlashAttribute("mensaje", "📍 Ubicación '" + alias + "' guardada en tu direccionario."); 
            } else {
                redirectAttributes.addFlashAttribute("error", "Error: No se pudo verificar tu sesión. Intenta re-ingresar.");
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR AL GUARDAR DIRECCIÓN: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error técnico al guardar: " + e.getMessage());
        }
        return "redirect:/direcciones";
    }

    

    @PostMapping("/eliminar/{id}")
    public String eliminarDireccion(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Direccion> dirOpt = direccionRepository.findById(id);
        if (dirOpt.isPresent()) {
            Direccion dir = dirOpt.get();
            if (dir.getUsuario().getUsername().equals(auth.getName())) {
                direccionRepository.delete(dir);
                mongoSyncService.sincronizarDirecciones(); // <-- AGREGA ESTO
                redirectAttributes.addFlashAttribute("mensaje", "Dirección eliminada.");
            
            }
        }
        return "redirect:/direcciones";
    }
}
