package com.example.guias.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoGuiaYRegionTest {

    @Test
    void normalizaAliasHistoricos() {
        assertEquals(EstadoGuia.EN_SUCURSAL, EstadoGuia.normalizar("En Bodega"));
        assertEquals(EstadoGuia.EN_SUCURSAL, EstadoGuia.normalizar("en sucursal"));
        assertEquals(EstadoGuia.EN_TRANSITO, EstadoGuia.normalizar("En tránsito"));
        assertEquals(EstadoGuia.EN_TRANSITO, EstadoGuia.normalizar("En Reparto"));
        assertEquals(EstadoGuia.NO_RECOGIDO, EstadoGuia.normalizar("no recogido"));
        assertEquals(EstadoGuia.RECOGIDO, EstadoGuia.normalizar("Recogido"));
        assertTrue(EstadoGuia.es("En Bodega", EstadoGuia.EN_SUCURSAL));
    }

    @Test
    void validaTransicionesDeRecogidaYEntrega() {
        assertTrue(EstadoGuia.transicionValida(EstadoGuia.PENDIENTE, EstadoGuia.RECOGIDO));
        assertTrue(EstadoGuia.transicionValida(EstadoGuia.PENDIENTE, EstadoGuia.NO_RECOGIDO));
        assertTrue(EstadoGuia.transicionValida(EstadoGuia.NO_RECOGIDO, EstadoGuia.PENDIENTE));
        assertTrue(EstadoGuia.transicionValida(EstadoGuia.RECOGIDO, EstadoGuia.EN_SUCURSAL));
        assertTrue(EstadoGuia.transicionValida("En Bodega", EstadoGuia.EN_TRANSITO));
        assertTrue(EstadoGuia.transicionValida(EstadoGuia.EN_TRANSITO, EstadoGuia.ENTREGADO));
        assertTrue(EstadoGuia.transicionValida(EstadoGuia.EN_TRANSITO, EstadoGuia.NOVEDAD));

        // Saltos inválidos entre fases
        assertFalse(EstadoGuia.transicionValida(EstadoGuia.PENDIENTE, EstadoGuia.EN_TRANSITO));
        assertFalse(EstadoGuia.transicionValida(EstadoGuia.PENDIENTE, EstadoGuia.ENTREGADO));
        assertFalse(EstadoGuia.transicionValida(EstadoGuia.NO_RECOGIDO, EstadoGuia.EN_SUCURSAL));
        assertFalse(EstadoGuia.transicionValida(EstadoGuia.ENTREGADO, EstadoGuia.PENDIENTE));
    }

    @Test
    void mapeaLasSeisRegionesYCapitales() {
        assertEquals(Region.CARIBE, Region.deDepartamento("Atlántico"));
        assertEquals(Region.ANDINA, Region.deDepartamento("Bogotá D.C."));
        assertEquals(Region.ANDINA, Region.deDepartamento("Antioquia"));
        assertEquals(Region.PACIFICA, Region.deDepartamento("Valle del Cauca"));
        assertEquals(Region.ORINOQUIA, Region.deDepartamento("Meta"));
        assertEquals(Region.AMAZONIA, Region.deDepartamento("Amazonas"));
        assertEquals(Region.INSULAR, Region.deDepartamento("San Andrés y Providencia"));

        assertTrue(Region.CARIBE.contieneCapital("BARRANQUILLA"));
        assertFalse(Region.CARIBE.contieneCapital("MEDELLÍN"));
        assertTrue(Region.ANDINA.contieneCapital("MEDELLÍN"));
    }
}

