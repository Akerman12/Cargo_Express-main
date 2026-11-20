package com.example.guias.controller;

import com.example.guias.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/soporte")
public class SoporteController {

    @Autowired
    private EmailService emailService;

    @GetMapping("")
    public String mostrarSoporte() {
        return "soporte";
    }

    @PostMapping("/enviar-queja")
    public String enviarQueja(@RequestParam("nombre") String nombre,
                              @RequestParam("email") String email,
                              @RequestParam("guiaId") String guiaId,
                              @RequestParam("mensaje") String mensaje,
                              RedirectAttributes redirectAttributes) {
        
        String destinatario = "saicargoexpress123@gmail.com";
        String asunto = "⚠️ NUEVA RECLAMACIÓN/PQRS - SAI CARGO #" + (guiaId.isEmpty() ? "Gral" : guiaId);
        
        String htmlCuerpo = """
            <div style="background-color: #f4f7fa; padding: 40px 0; font-family: 'Inter', 'Segoe UI', Arial, sans-serif;">
                <table align="center" border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;">
                    <!-- Cabecera Premium -->
                    <tr>
                        <td align="center" style="background: linear-gradient(135deg, #002e7d 0%%, #001f5c 100%%); padding: 40px 20px;">
                            <img src="cid:logo" alt="SAI Cargo" width="100" style="display: block; margin-bottom: 15px; border-radius: 8px;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px; text-transform: uppercase;">Centro de PQRS</h1>
                            <div style="height: 3px; width: 50px; background-color: #ffcc00; margin: 15px auto;"></div>
                            <p style="color: #ffcc00; margin: 0; font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px;">Gestión de Incidencias de Carga</p>
                        </td>
                    </tr>
                    
                    <!-- Contenido -->
                    <tr>
                        <td style="padding: 40px 50px;">
                            <h2 style="color: #1e293b; font-size: 20px; margin-top: 0; font-weight: 800; border-left: 4px solid #ef4444; padding-left: 15px;">Notificación de Nueva Queja</h2>
                            <p style="color: #64748b; font-size: 15px; line-height: 1.6;">Se ha recibido una nueva solicitud de soporte/reclamación a través del portal oficial. A continuación los detalles para su gestión inmediata:</p>
                            
                            <div style="background-color: #f8fafc; border-radius: 12px; padding: 25px; margin: 30px 0; border: 1px solid #e2e8f0;">
                                <table width="100%%" cellpadding="0" cellspacing="0">
                                    <tr>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #edf2f7;">
                                            <span style="color: #94a3b8; font-size: 11px; font-weight: 700; text-transform: uppercase;">Remitente</span>
                                            <div style="color: #1e293b; font-size: 15px; font-weight: 700;">%s</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #edf2f7;">
                                            <span style="color: #94a3b8; font-size: 11px; font-weight: 700; text-transform: uppercase;">Email de Contacto</span>
                                            <div style="color: #002e7d; font-size: 14px; font-weight: 600;">%s</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0; border-bottom: 1px solid #edf2f7;">
                                            <span style="color: #94a3b8; font-size: 11px; font-weight: 700; text-transform: uppercase;">ID de Guía Relacionada</span>
                                            <div style="color: #1e293b; font-size: 14px; font-weight: 700;">%s</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 15px 0 0 0;">
                                            <span style="color: #94a3b8; font-size: 11px; font-weight: 700; text-transform: uppercase;">Situación Descrita</span>
                                            <div style="color: #475569; font-size: 14px; line-height: 1.6; margin-top: 5px; padding: 15px; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0;">
                                                %s
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            
                            <p style="color: #94a3b8; font-size: 13px; text-align: center; font-style: italic;">Este reporte ha sido generado automáticamente por el sistema de auditoría SAI Cargo.</p>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #f8fafc; padding: 30px; text-align: center; border-top: 1px solid #e2e8f0;">
                            <p style="color: #64748b; font-size: 12px; margin: 0;">&copy; 2026 SAI CARGO EXPRESS | Operaciones Logísticas Globales</p>
                            <div style="margin-top: 10px;">
                                <span style="display: inline-block; width: 8px; height: 8px; background: #10b981; border-radius: 50%%; margin-right: 5px;"></span>
                                <span style="color: #10b981; font-size: 11px; font-weight: 700; text-transform: uppercase;">Sistema Activo</span>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>
            """.formatted(nombre, email, (guiaId.isEmpty() ? "No proporcionada" : guiaId), mensaje);

        try {
            emailService.enviarCorreoHtml(destinatario, asunto, htmlCuerpo, "static/imagenes/LOGO Cargo Expres.jpg");
            redirectAttributes.addFlashAttribute("mensajeExito", "Tu queja ha sido enviada con éxito. Nos pondremos en contacto contigo pronto.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo enviar el correo en este momento. Por favor, intenta más tarde o escribe directamente a " + destinatario);
        }

        return "redirect:/soporte";
    }
}
