package com.example.guias.controller;

import com.example.guias.model.DriverLocationMessage;
import com.example.guias.model.Guia;
import com.example.guias.model.UbicacionMensaje;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.GuiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class TrackingWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // El emisor (repartidor) enviará a: /app/track/driver
    // El servidor retransmitirá a múltiples canales: /topic/route/{guiaId}
    @MessageMapping("/track/driver")
    public void updateDriverLocation(Principal principal, @Payload DriverLocationMessage driverLocation) {
        // 1. Identificar al repartidor
        // Si usamos Spring Security STOMP, 'principal' tendrá el usuario actual.
        // Si no, confiamos en el username del payload.
        String username = (principal != null) ? principal.getName() : driverLocation.getUsername();
        
        if (username == null || username.isEmpty()) {
            System.err.println("Error: No se pudo identificar al repartidor en WebSocket.");
            return;
        }

        Optional<Usuario> repartidorOpt = usuarioRepository.findByUsername(username);
        
        if (repartidorOpt.isPresent()) {
            Usuario repartidor = repartidorOpt.get();
            
            // 2. Obtener guías activas del repartidor (que no estén entregadas ni eliminadas)
            List<Guia> guiasActivas = guiaService.obtenerPorRepartidorAsignado(repartidor);
            
            System.out.println("Repartidor " + username + " emitiendo GPS a " + guiasActivas.size() + " clientes activos.");

            // 3. Multiplexar: Enviar la ubicación a cada guía activa
            for (Guia guia : guiasActivas) {
                // Solo transmitimos si está 'En tránsito' (o dependiendo de la lógica de tu negocio)
                // obtenerPorRepartidorAsignado ya excluye 'Entregado' y 'Eliminada'.
                
                UbicacionMensaje ubicacionCliente = new UbicacionMensaje(
                        guia.getId(), 
                        driverLocation.getLatitud(), 
                        driverLocation.getLongitud()
                );
                
                messagingTemplate.convertAndSend("/topic/route/" + guia.getId(), ubicacionCliente);
            }
        }
    }
}
