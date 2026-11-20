package com.example.guias.util;

import com.example.guias.model.Guia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistanciaUtilTest {

    @Test
    void obtenerDistanciaGuia_prefiereDistanciaVialOSRMCuandoEsValida() {
        Guia guia = new Guia();
        guia.setCiudadOrigen("Bogotá D.C.");
        guia.setCiudadDestino("Medellín");
        guia.setDistanciaKm(415.6);

        double resultado = DistanciaUtil.obtenerDistanciaGuia(guia);
        assertEquals(415.6, resultado, 0.001);
    }

    @Test
    void obtenerDistanciaGuia_usaHaversineSiDistanciaKmEsNulaOInvalida() {
        Guia guia = new Guia();
        guia.setCiudadOrigen("Bogotá D.C.");
        guia.setCiudadDestino("Cartagena de Indias");
        guia.setDistanciaKm(null);

        double distCartagena = DistanciaUtil.obtenerDistanciaGuia(guia);
        assertTrue(distCartagena > 600 && distCartagena < 700,
                "Distancia Bogotá D.C. -> Cartagena de Indias debería rondar ~650 km, fue: " + distCartagena);

        guia.setCiudadDestino("Medellín");
        guia.setDistanciaKm(-10.0);
        double distMedellin = DistanciaUtil.obtenerDistanciaGuia(guia);
        assertTrue(distMedellin > 200 && distMedellin < 280,
                "Con distancia negativa debe caer a Haversine (~240 km), fue: " + distMedellin);
    }

    @Test
    void obtenerDistanciaGuia_manejaGuiaNulaSinErrores() {
        assertEquals(0.0, DistanciaUtil.obtenerDistanciaGuia(null));
    }
}

