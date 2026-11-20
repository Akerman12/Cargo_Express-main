package com.example.guias.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guias.model.Region;
import com.example.guias.model.Rol;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
private com.example.guias.service.MongoSyncService mongoSyncService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("regiones", Region.values());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute @Valid Usuario usuario,
                                   BindingResult result,
                                   @RequestParam(value = "aceptaTerminos", defaultValue = "false") boolean aceptaTerminos,
                                   Model model) {
        model.addAttribute("regiones", Region.values());
        if (result.hasErrors()) {
            return "registro";
        }

        if (!aceptaTerminos) {
            model.addAttribute("error", "Debes aceptar los términos y la autorización de tratamiento de datos personales (Ley 1581 de 2012).");
            return "registro";
        }
        
        // Validar si existe
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            model.addAttribute("error", "El correo ya está registrado. Usa otro o inicia sesión.");
            return "registro";
        }
        
        // El repartidor debe indicar su región de operación; los demás roles no la usan
        if (usuario.getRol() == Rol.ROLE_REPARTIDOR) {
            if (usuario.getRegionOperacion() == null) {
                model.addAttribute("error", "Selecciona tu región de operación para solicitar la cuenta de repartidor.");
                return "registro";
            }
        } else {
            usuario.setRegionOperacion(null);
        }

        // Encriptar
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // Asignar rol y estado de aprobación
        if (usuario.getRol() == Rol.ROLE_REPARTIDOR) {
            usuario.setAprobado(false); // Requiere aprobación del admin
        } else {
            usuario.setAprobado(true); // Clientes se auto-aprueban
        }

        if (usuario.getRol() == null) {
            usuario.setRol(Rol.ROLE_CLIENTE);
        }

        usuarioRepository.save(usuario);
        mongoSyncService.sincronizarUsuarios();
        return "redirect:/login?registered" + (usuario.isAprobado() ? "" : "&pending");
        
    }

    @GetMapping("/login")
    public String mostrarLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "pending", required = false) String pending,
            Model model) {

        if (error != null) model.addAttribute("mensajeError", "Error: Credenciales inválidas o cuenta bloqueada por el administrador.");
        if (logout != null) model.addAttribute("mensaje", "Has cerrado sesión correctamente.");
        if (registered != null) {
            if (pending != null) {
                model.addAttribute("mensaje", "Registro exitoso. Tu cuenta de Repartidor está pendiente de aprobación por un administrador.");
            } else {
                model.addAttribute("mensaje", "Registro exitoso. Ya puedes iniciar sesión.");
            }
        }

        return "login";
    }

    @GetMapping("/conocenos")
    public String mostrarConocenos() {
        return "conocenos";
    }
}