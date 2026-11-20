
package com.example.guias.controller;

import com.example.guias.model.Guia;
import com.example.guias.service.PriceCalculatorService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cotizacion")
public class CotizacionController {

    private final PriceCalculatorService priceCalculatorService;

    public CotizacionController(
            PriceCalculatorService priceCalculatorService) {
        this.priceCalculatorService = priceCalculatorService;
    }

    @PostMapping("/calcular")
    public ResponseEntity<?> calcular(
            @RequestBody CotizacionRequest datos) {

        if (datos.origen() == null || datos.origen().isBlank()
                || datos.destino() == null || datos.destino().isBlank()
                || !Double.isFinite(datos.altura())
                || !Double.isFinite(datos.ancho())
                || !Double.isFinite(datos.largo())
                || !Double.isFinite(datos.peso())
                || !Double.isFinite(datos.valorDeclarado())
                || datos.altura() <= 0
                || datos.ancho() <= 0
                || datos.largo() <= 0
                || datos.peso() <= 0
                || datos.valorDeclarado() < 0) {

            return ResponseEntity.badRequest().body(
                Map.of("error", "Verifica los datos de tu paquete.")
            );
        }

        Guia guia = new Guia();

        guia.setAlturaCm(datos.altura());
        guia.setAnchoCm(datos.ancho());
        guia.setLargoCm(datos.largo());
        guia.setPesoKg(datos.peso());
        guia.setValorDeclarado(datos.valorDeclarado());

        guia.setCiudadOrigen(datos.origen().trim());
        guia.setCiudadDestino(datos.destino().trim());

        double pesoVolumetrico =
                (datos.altura() * datos.ancho() * datos.largo()) / 4000.0;

        double pesoCobrable =
                Math.max(datos.peso(), pesoVolumetrico);

        double precio =
                priceCalculatorService.calcularPrecio(guia);

        return ResponseEntity.ok(Map.of(
            "origen", datos.origen().trim(),
            "destino", datos.destino().trim(),
            "pesoFisico", datos.peso(),
            "pesoVolumetrico", pesoVolumetrico,
            "pesoCobrable", pesoCobrable,
            "valorDeclarado", datos.valorDeclarado(),
            "precio", precio
        ));
    }

    public record CotizacionRequest(
        String origen,
        String destino,
        double altura,
        double ancho,
        double largo,
        double peso,
        double valorDeclarado
    ) {}
}
