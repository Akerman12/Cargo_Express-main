package com.example.guias.controller;

import com.example.guias.service.WekaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/guias")
public class GuiaRestController {

    @Autowired
    private WekaService wekaService;

    @GetMapping("/predecir-empaque")
    public Map<String, String> predecirEmpaque(
            @RequestParam double peso,
            @RequestParam double ancho,
            @RequestParam double alto) {
        
        String empaqueSugerido = wekaService.predecirEmpaque(peso, ancho, alto);
        return Map.of("sugerencia", empaqueSugerido);
    }
}
