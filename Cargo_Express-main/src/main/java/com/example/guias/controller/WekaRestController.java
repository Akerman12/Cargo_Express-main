package com.example.guias.controller;

import com.example.guias.service.WekaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/weka")
public class WekaRestController {

    @Autowired
    private WekaService wekaService;

    @PostMapping("/tiempo")
    public Map<String, Object> predecirTiempo(@RequestBody Map<String, String> payload) {
        String origen = payload.get("origen");
        String destino = payload.get("destino");
        String tipoMedio = payload.get("tipoMedio"); // e.g., Terrestre
        String clima = payload.get("clima");
        String tipoServicio = payload.get("tipoServicio");

        double basePrediction = wekaService.predecirTiempoConHeuristica(origen, destino, tipoMedio, clima, tipoServicio);
        return Map.of("resultado", basePrediction + " días");
    }

    @PostMapping("/empaque")
    public Map<String, Object> predecirEmpaque(@RequestBody Map<String, String> payload) {
        double peso = Double.parseDouble(payload.get("peso"));
        double volumen = Double.parseDouble(payload.get("volumen")); 
        double fragilidad = Double.parseDouble(payload.get("fragilidad")); 

        String empaque = wekaService.predecirEmpaque(peso, volumen, fragilidad);
        return Map.of("resultado", empaque);
    }

    @PostMapping("/co2")
    public Map<String, Object> predecirCO2(@RequestBody Map<String, String> payload) {
        double distancia = Double.parseDouble(payload.get("distancia"));
        double peso = Double.parseDouble(payload.get("peso"));
        String modalidad = payload.get("modalidad");
        String combustible = payload.get("combustible");

        double co2 = wekaService.predecirCO2(distancia, peso); // base
        // Apply heuristics for modalidad/combustible
        if ("Avión de carga".equals(modalidad)) co2 *= 2.5;
        if ("Camión pesado".equals(modalidad)) co2 *= 1.2;
        if ("Eléctrico".equals(combustible)) co2 *= 0.1;
        if ("Híbrido".equals(combustible)) co2 *= 0.6;
        
        co2 = Math.round(co2 * 100.0) / 100.0;
        
        return Map.of("resultado", co2 + " kg");
    }
}
