package com.example.guias.service;

import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GuiaService guiaService;
    @InjectMocks private VehiculoService service;

    private Usuario rep;
    private Vehiculo furgoneta;

    private Guia guia(long id, double peso, double ancho, double alto) {
        Guia g = new Guia();
        g.setId(id);
        g.setPesoKg(peso);
        g.setAnchoCm(ancho);
        g.setAlturaCm(alto);
        return g;
    }

    @BeforeEach
    void setUp() {
        rep = new Usuario();
        rep.setId(1L);
        rep.setNombre("Carlos");
        furgoneta = new Vehiculo("ABC-123", "Master", "FURGONETA", 1000.0, 8.0);
        lenient().when(vehiculoRepository.findByRepartidor(rep)).thenReturn(Optional.of(furgoneta));
    }

    @Test
    void sinVehiculo_rechaza() {
        Usuario otro = new Usuario();
        otro.setId(2L);
        otro.setNombre("Sin Furgoneta");
        when(vehiculoRepository.findByRepartidor(otro)).thenReturn(Optional.empty());
        String err = service.validarAsignacion(otro, guia(10, 5, 10, 10));
        assertNotNull(err);
        assertTrue(err.contains("no tiene ninguna furgoneta"));
    }

    @Test
    void dentroDeCapacidad_acepta() {
        when(guiaService.obtenerPorRepartidorAsignado(rep)).thenReturn(List.of(guia(1, 800, 10, 10)));
        assertNull(service.validarAsignacion(rep, guia(10, 200, 10, 10))); // 1000 == limite exacto
    }

    @Test
    void excedePeso_rechaza() {
        when(guiaService.obtenerPorRepartidorAsignado(rep)).thenReturn(List.of(guia(1, 800, 10, 10)));
        String err = service.validarAsignacion(rep, guia(10, 201, 10, 10));
        assertNotNull(err);
        assertTrue(err.contains("Capacidad superada"));
    }

    @Test
    void excedeVolumen_rechaza() {
        when(guiaService.obtenerPorRepartidorAsignado(rep)).thenReturn(List.of());
        // 400 x 500 x 20 cm = 4 m3 por paquete; el segundo ya no cabe en 8 m3 junto con uno de 5 m3
        String err = service.validarAsignacion(rep, guia(10, 10, 500, 1000)); // 10 m3 > 8 m3
        assertNotNull(err);
        assertTrue(err.contains("Volumen superado"));
    }

    @Test
    void reasignarMismaGuia_noCuentaDoble() {
        Guia yaAsignada = guia(5, 900, 10, 10);
        when(guiaService.obtenerPorRepartidorAsignado(rep)).thenReturn(List.of(yaAsignada));
        // Misma guia (id 5) de 900 kg sobre una furgoneta de 1000 kg: debe aceptar
        assertNull(service.validarAsignacion(rep, yaAsignada));
    }
}

