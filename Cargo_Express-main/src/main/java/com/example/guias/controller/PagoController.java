package com.example.guias.controller;

import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.GuiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/pago")
public class PagoController {

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private com.example.guias.service.MongoSyncService mongoSyncService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.example.guias.repository.PagoRepository pagoRepository;

    @Autowired
    private com.example.guias.service.PdfService pdfService;

    @GetMapping("/{id}")
    public String mostrarPasarela(@PathVariable Long id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
        
        if (guiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La guía que intentas pagar no existe.");
            return "redirect:/guias";
        }

        Guia guia = guiaOpt.get();
        
        // Verificar que el usuario sea el dueño o sea Admin
        String username = auth.getName();
        Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (!guia.getUsuario().getId().equals(user.getId()) && !isAdmin) {
                redirectAttributes.addFlashAttribute("error", "No tienes autorización para pagar esta guía.");
                return "redirect:/guias";
            }
        }

        if ("PAGADO".equals(guia.getEstadoPago())) {
            redirectAttributes.addFlashAttribute("mensaje", "Esta guía ya se encuentra pagada.");
            return "redirect:/guias";
        }

        model.addAttribute("guia", guia);
        return "pagar";
    }

    @PostMapping("/procesar")
    @org.springframework.transaction.annotation.Transactional
    public String procesarPago(@RequestParam("guiaId") Long guiaId, 
                               @RequestParam("metodo") String metodo,
                               @RequestParam(value="titularCard", required=false) String titularCard,
                               @RequestParam(value="titularNequi", required=false) String titularNequi,
                               @RequestParam(value="numeroCuentaBancolombia", required=false) String cuentaBan,
                               @RequestParam(value="docPse", required=false) String docPse,
                               Authentication auth, RedirectAttributes redirectAttributes) {
        
        try {
            Optional<Guia> guiaOpt = guiaService.buscarPorId(guiaId);
            
            if (guiaOpt.isPresent()) {
                Guia guia = guiaOpt.get();
                
                // 1. Simulación de validación y actualización de guía
                guia.setEstadoPago("PAGADO");
                guiaService.guardarGuia(guia);

                

                // 2. Prevenir colisión OneToOne: Borrar pago anterior si existe y FLUSH
                pagoRepository.findByGuiaId(guiaId).ifPresent(p -> {
                    pagoRepository.delete(p);
                    pagoRepository.flush(); // Forzar la eliminación en DB antes de insertar el nuevo
                });

                // 3. Determinar el titular real de forma segura
                String infoTitular = "";
                if ("NEQUI".equalsIgnoreCase(metodo)) infoTitular = titularNequi;
                else if ("BANCOLOMBIA".equalsIgnoreCase(metodo)) infoTitular = cuentaBan;
                else if ("PSE".equalsIgnoreCase(metodo)) infoTitular = docPse;
                else infoTitular = titularCard;

                if (infoTitular == null || infoTitular.isBlank()) {
                    infoTitular = (auth != null) ? auth.getName() : "Usuario Final";
                }

            // 4. Guardar registro de pago para auditoría admin
                com.example.guias.model.Pago pago = new com.example.guias.model.Pago(guia.getPrecio(), metodo, infoTitular, guia);
                pagoRepository.save(pago);
                
                // SINCRONIZAR A MONGO (Actualiza la guía porque cambió su estado a PAGADO, y guarda el nuevo pago)
                mongoSyncService.sincronizarGuias();
                mongoSyncService.sincronizarPagos();
                
                redirectAttributes.addFlashAttribute("mensaje", "💳 ¡Pago procesado con éxito! Tu guía #" + guiaId + " ahora está confirmada."); 
            } else {
                redirectAttributes.addFlashAttribute("error", "Error: Guía no encontrada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error crítico al procesar el pago. Por favor intente más tarde.");
        }
        
        return "redirect:/guias/mis-guias";
    }

    @GetMapping("/admin/ver")
    public String verPagosAdmin(Model model, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return "redirect:/menu";

        java.util.List<com.example.guias.model.Pago> pagos = pagoRepository.findAll();
        double totalAcumulado = pagos.stream().mapToDouble(com.example.guias.model.Pago::getMonto).sum();

        boolean isSupremo = auth.getName().equals("admin@saicargo.com");
        model.addAttribute("isSupremo", isSupremo);
        model.addAttribute("pagos", pagos);
        model.addAttribute("totalAcumulado", totalAcumulado);
        return "admin-pagos";
    }

    @GetMapping("/recibo/{guiaId}")
    public org.springframework.http.ResponseEntity<byte[]> descargarRecibo(@PathVariable Long guiaId, Authentication auth) {
        // Buscamos la guía primero
        Optional<Guia> guiaOpt = guiaService.buscarPorId(guiaId);
        if (guiaOpt.isEmpty()) return org.springframework.http.ResponseEntity.notFound().build();
        
        // Buscamos el pago asociado a esa guía usando el nuevo método del repositorio
        Optional<com.example.guias.model.Pago> pagoOpt = pagoRepository.findByGuiaId(guiaId);

        if (pagoOpt.isEmpty()) return org.springframework.http.ResponseEntity.notFound().build();

        com.example.guias.model.Pago pago = pagoOpt.get();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!pago.getGuia().getUsuario().getUsername().equals(auth.getName()) && !isAdmin) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }

        try {
            byte[] pdfContent = pdfService.generarReciboPago(pago);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Recibo_SAI_" + guiaId + ".pdf");
            return org.springframework.http.ResponseEntity.ok().headers(headers).body(pdfContent);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }
}
