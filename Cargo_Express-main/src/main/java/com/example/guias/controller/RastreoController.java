package com.example.guias.controller;

import com.example.guias.model.Guia;
import com.example.guias.service.GuiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class RastreoController {

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private com.example.guias.service.WekaService wekaService;


    @GetMapping("/rastreo")
    public String mostrarPaginaRastreo(@RequestParam(value = "id", required = false) Long id, Model model) {
        if (id != null) {
            Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
            if (guiaOpt.isPresent()) {
                Guia guia = guiaOpt.get();
                
                // Si la guía está eliminada, no la mostramos públicamente
                if ("Eliminada".equals(guia.getEstado())) {
                    model.addAttribute("error", "No se encontró información para la guía #" + id);
                } else {
                    model.addAttribute("guia", guia);
                    
                    // Calcular distancia en KM (priorizando ruta real por carretera si existe)
                    double km = com.example.guias.util.DistanciaUtil.obtenerDistanciaGuia(guia);
                    model.addAttribute("distanciaKm", Math.round(km * 10.0) / 10.0);
                    
                    // Predicción de CO2 con Regresión Lineal (AI)
                    double co2Kg = wekaService.predecirCO2(km, guia.getPesoKg());
                    model.addAttribute("co2Kg", co2Kg);
                }
            } else {
                model.addAttribute("error", "No se encontró ninguna guía con el número: " + id);
            }
        }
        return "rastreo";
    }

}
