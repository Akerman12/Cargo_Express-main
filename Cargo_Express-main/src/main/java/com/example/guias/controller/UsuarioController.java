package com.example.guias.controller;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.MongoSyncService; // IMPORTANTE

@Controller
@RequestMapping("/usuario")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // INYECCIÓN DEL SERVICIO DE MONGO
    @Autowired
    private MongoSyncService mongoSyncService;

    @GetMapping("/perfil")
    public String verPerfil(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            return "ver-perfil"; 
        } else {
            return "redirect:/menu?error=UsuarioNoEncontrado";
        }
    }

    @GetMapping("/editar-perfil")
    public String mostrarFormularioEdicion(Authentication authentication, Model model) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        model.addAttribute("usuario", usuario);
        return "editar-perfil";
    }

    @PostMapping("/actualizar-perfil")
    public String actualizarPerfil(@ModelAttribute Usuario usuarioForm, 
                                   @RequestParam(value = "newPassword", required = false) String newPassword,
                                   Authentication authentication, 
                                   RedirectAttributes redirectAttributes) {
        
        Usuario usuarioDb = usuarioRepository.findById(usuarioForm.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean usernameChanged = !usuarioDb.getUsername().equals(usuarioForm.getUsername());
        usuarioDb.setNombre(usuarioForm.getNombre());
        usuarioDb.setUsername(usuarioForm.getUsername());

        if (newPassword != null && !newPassword.isBlank()) {
            usuarioDb.setPassword(passwordEncoder.encode(newPassword));
        }

        usuarioRepository.save(usuarioDb);
        
        // ---> SINCRONIZAR CON MONGODB <---
        mongoSyncService.sincronizarUsuarios(); 
        
        if (usernameChanged) {
            redirectAttributes.addFlashAttribute("mensaje", "Has cambiado tu correo electrónico. Por seguridad, debes iniciar sesión de nuevo con tu nueva identidad.");
            return "redirect:/logout";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/usuario/perfil";
    }
}