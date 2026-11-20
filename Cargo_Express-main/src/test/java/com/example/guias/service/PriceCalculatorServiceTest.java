package com.example.guias.service;

import com.example.guias.model.Guia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorServiceTest {

    private PriceCalculatorService priceCalculatorService;

    @BeforeEach
    void setUp() {
        priceCalculatorService = new PriceCalculatorService();
    }

    @Test
    void testCalcularPrecioRutaNormal() {
        // Preparar
        Guia guia = new Guia();
        guia.setPesoKg(10.0);
        guia.setAnchoCm(10.0);
        guia.setAlturaCm(10.0); // Peso vol: (10*10*20)/4000 = 0.5. Se cobra 10.
        guia.setValorDeclarado(50000.0);
        guia.setCiudadOrigen("Bogotá");
        guia.setCiudadDestino("Medellín");

        // Actuar
        double precio = priceCalculatorService.calcularPrecio(guia);

        // Verificar:
        // Base: 12500
        // Peso: 10 * 3500 = 35000
        // Seguro: 50000 * 0.02 = 1000
        // Total esperado: 12500 + 35000 + 1000 = 48500
        assertEquals(48500.0, precio);
    }

    @Test
    void testCalcularPrecioRutaEspecial() {
        // Preparar
        Guia guia = new Guia();
        guia.setPesoKg(2.0); // Peso cobrado: 2
        guia.setAnchoCm(10.0);
        guia.setAlturaCm(10.0);
        guia.setValorDeclarado(10000.0);
        guia.setCiudadOrigen("Leticia"); // Ruta especial
        guia.setCiudadDestino("Bogotá");

        // Actuar
        double precio = priceCalculatorService.calcularPrecio(guia);

        // Verificar:
        // Base: 12500
        // Peso: 2 * 3500 = 7000
        // Seguro: 10000 * 0.02 = 200
        // Recargo: 15000
        // Total esperado: 12500 + 7000 + 200 + 15000 = 34700
        assertEquals(34700.0, precio);
    }

    @Test
    void testCalcularPrecioUsaLargoCuandoExiste() {
        Guia guia = new Guia();
        guia.setPesoKg(2.0);
        guia.setAnchoCm(10.0);
        guia.setAlturaCm(10.0);
        guia.setLargoCm(400.0); // volumetrico: 10*10*400/4000 = 10 kg
        guia.setValorDeclarado(0.0);
        guia.setCiudadOrigen("Bogotá");
        guia.setCiudadDestino("Medellín");
        assertEquals(12500.0 + 10 * 3500.0, priceCalculatorService.calcularPrecio(guia));
    }

    @Test
    void testCalcularPrecioConDistanciaVialOSRM() {
        Guia guiaBase = new Guia();
        guiaBase.setPesoKg(10.0);
        guiaBase.setAnchoCm(10.0);
        guiaBase.setAlturaCm(10.0);
        guiaBase.setValorDeclarado(50000.0);
        guiaBase.setCiudadOrigen("Bogotá");
        guiaBase.setCiudadDestino("Medellín");

        // Ruta más rápida: 400 km (+400 * 25 = 10000)
        guiaBase.setDistanciaKm(400.0);
        assertEquals(58500.0, priceCalculatorService.calcularPrecio(guiaBase));

        // Ruta alternativa más larga: 460 km (+460 * 25 = 11500)
        guiaBase.setDistanciaKm(460.0);
        assertEquals(60000.0, priceCalculatorService.calcularPrecio(guiaBase));
    }
}
