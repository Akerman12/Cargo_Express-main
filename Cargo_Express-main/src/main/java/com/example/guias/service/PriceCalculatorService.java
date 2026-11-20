package com.example.guias.service;

import com.example.guias.model.Guia;
import org.springframework.stereotype.Service;

@Service
public class PriceCalculatorService {

    
    private static final double TARIFA_BASE_FIJA = 12500.0; 
    private static final double PRECIO_POR_KG = 3500.0; 
    private static final double SEGURO_PORCENTAJE = 0.02; 
    private static final double RECARGO_ZONA_DIFÍCIL = 15000.0; 
    public static final double TARIFA_POR_KM_VIAL = 25.0;

    public double calcularPrecio(Guia guia) {
        double precio = TARIFA_BASE_FIJA;

        // Peso Volumétrico vs Físico
        // Compatibilidad: sin largo registrado se asumen 20 cm (comportamiento historico)
        double largo = guia.getLargoCm() != null && guia.getLargoCm() > 0 ? guia.getLargoCm() : 20.0;
        double pesoVolumetrico = (guia.getAnchoCm() * guia.getAlturaCm() * largo) / 4000.0;
        double pesoCobrar = Math.max(guia.getPesoKg(), pesoVolumetrico);
        
        precio += pesoCobrar * PRECIO_POR_KG;
        precio += guia.getValorDeclarado() * SEGURO_PORCENTAJE;

        if (guia.getDistanciaKm() != null && Double.isFinite(guia.getDistanciaKm()) && guia.getDistanciaKm() > 0) {
            precio += guia.getDistanciaKm() * TARIFA_POR_KM_VIAL;
        }

        if (esRutaEspecial(guia.getCiudadOrigen(), guia.getCiudadDestino())) {
            precio += RECARGO_ZONA_DIFÍCIL;
        }

        return Math.round(precio);
    }

    private boolean esRutaEspecial(String origen, String destino) {
        // Ejemplo de rutas que podrían tener recargo (ciudades apartadas)
        String[] apartadas = {"Leticia", "Quibdó", "Puerto Carreño", "Mitú", "Inírida"};
        for (String ciudad : apartadas) {
            if (ciudad.equalsIgnoreCase(origen) || ciudad.equalsIgnoreCase(destino)) {
                return true;
            }
        }
        return false;
    }
}