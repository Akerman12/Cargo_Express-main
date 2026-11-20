package com.example.guias.controller;

import com.example.guias.model.Usuario;
import com.example.guias.service.EmailService;
import com.example.guias.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    private final UsuarioService usuarioService;
    private final EmailService emailService;

    public ForgotPasswordController(UsuarioService usuarioService, EmailService emailService) {
        this.usuarioService = usuarioService;
        this.emailService = emailService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                      HttpServletRequest request, 
                                      RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioService.findByUsername(email);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = usuarioService.createPasswordResetTokenForUser(usuario);
            
            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String resetUrl = appUrl + "/reset-password?token=" + token;
            
            String subject = "Restablecer contraseña - SAI CARGO";
            String content = "Hola " + usuario.getNombre() + ",\n\n"
                    + "Has solicitado restablecer tu contraseña en SAI CARGO.\n"
                    + "Haz clic en el siguiente enlace para continuar:\n"
                    + resetUrl + "\n\n"
                    + "Si no solicitaste esto, ignora este correo.\n"
                    + "El enlace expirará en 24 horas.";
            
            try {
                emailService.enviarCorreo(email, subject, content);
                redirectAttributes.addFlashAttribute("success", "Se ha enviado un enlace a tu correo.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error al enviar el correo. Inténtalo más tarde.");
            }
        } else {
            // Por seguridad, no informamos si el correo existe o no, pero aquí lo haremos para facilitar al usuario
            redirectAttributes.addFlashAttribute("error", "No encontramos una cuenta con ese correo.");
        }
        
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Optional<Usuario> usuarioOpt = usuarioService.getUserByPasswordResetToken(token);
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "El enlace es inválido o ha expirado.");
            return "forgot-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token, 
                                     @RequestParam("password") String password,
                                     RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioService.getUserByPasswordResetToken(token);
        
        if (usuarioOpt.isPresent()) {
            usuarioService.changeUserPassword(usuarioOpt.get(), password);
            redirectAttributes.addFlashAttribute("message", "Tu contraseña ha sido actualizada exitosamente.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Hubo un error al procesar tu solicitud.");
            return "redirect:/forgot-password";
        }
    }
}
