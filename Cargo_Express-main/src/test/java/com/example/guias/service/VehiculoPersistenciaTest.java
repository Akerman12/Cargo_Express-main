package com.example.guias.service;

import com.example.guias.model.Guia;
import com.example.guias.model.Rol;
import com.example.guias.model.Ruta;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.GuiaRepository;
import com.example.guias.repository.RutaRepository;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.repository.VehiculoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integración con Hibernate + H2 reales (sin IA, sin Mongo, sin red).
 * Verifica el mapeo de las entidades nuevas y las reglas de flota contra una base de datos de verdad.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({VehiculoService.class, GuiaService.class})
class VehiculoPersistenciaTest {

    @Autowired private VehiculoService service;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private RutaRepository rutaRepository;
    @Autowired private EntityManager em;

    private Usuario cliente;
    private Usuario carlos;
    private Usuario maria;
    private Vehiculo furgonetaA;
    private Vehiculo furgonetaB;

    private Usuario usuario(String nombre, String email, Rol rol, boolean aprobado) {
        Usuario u = new Usuario(nombre, email, "x", rol);
        u.setEmail(email);
        u.setAprobado(aprobado);
        return usuarioRepository.save(u);
    }

    private Guia guia(double peso) {
        Guia g = new Guia();
        g.setRemitente("Juan Perez");
        g.setDestinatario("Ana Gomez");
        g.setDireccionEnvio("Calle 1");
        g.setDireccionRecibo("Calle 2");
        g.setCedulaRemitente("1");
        g.setCedulaDestinatario("2");
        g.setCiudadOrigen("Bogota");
        g.setCiudadDestino("Medellin");
        g.setDepartamentoOrigen("Cundinamarca");
        g.setDepartamentoDestino("Antioquia");
        g.setTipoCarga("Caja");
        g.setPesoKg(peso);
        g.setAlturaCm(10);
        g.setAnchoCm(10);
        g.setValorDeclarado(0);
        g.setPrecio(1000);
        g.setUsuario(cliente);
        return guiaRepository.save(g);
    }

    @BeforeEach
    void setUp() {
        cliente = usuario("Cliente", "cli@x.com", Rol.ROLE_CLIENTE, true);
        carlos = usuario("Carlos", "carlos@x.com", Rol.ROLE_REPARTIDOR, true);
        maria = usuario("Maria", "maria@x.com", Rol.ROLE_REPARTIDOR, true);
        furgonetaA = vehiculoRepository.save(new Vehiculo("AAA-111", "Master", "FURGONETA", 1000.0, 8.0));
        furgonetaB = vehiculoRepository.save(new Vehiculo("BBB-222", "Urvan", "FURGONETA", 800.0, 6.0));
    }

    @Test
    void mapeoJpa_guardaYRecuperaVehiculoConConductor() {
        assertNull(service.asignarRepartidor(furgonetaA.getId(), carlos.getId()));
        em.flush();
        em.clear();

        Vehiculo v = vehiculoRepository.findByRepartidor(usuarioRepository.findById(carlos.getId()).get()).orElseThrow();
        assertEquals("AAA-111", v.getPlaca());
        assertEquals("ASIGNADO", v.getEstado());
        assertEquals(1, service.obtenerRepartidoresConVehiculo().size());
        // maria sigue sin furgoneta
        assertEquals(List.of("Maria"), service.obtenerRepartidoresDisponiblesSinVehiculo().stream().map(Usuario::getNombre).toList());
    }

    @Test
    void mover_conductor_de_una_furgoneta_a_otra_no_viola_unicidad() {
        assertNull(service.asignarRepartidor(furgonetaA.getId(), carlos.getId()));
        em.flush();
        // Carlos pasa de la A a la B: la columna repartidor_id es UNIQUE, el orden de los UPDATE importa
        assertNull(service.asignarRepartidor(furgonetaB.getId(), carlos.getId()));
        em.flush();
        em.clear();

        assertEquals("DISPONIBLE", vehiculoRepository.findById(furgonetaA.getId()).get().getEstado());
        assertNull(vehiculoRepository.findById(furgonetaA.getId()).get().getRepartidor());
        assertEquals("ASIGNADO", vehiculoRepository.findById(furgonetaB.getId()).get().getEstado());
    }

    @Test
    void noPermite_asignar_conductor_a_furgoneta_en_mantenimiento() {
        assertNull(service.cambiarEstado(furgonetaA.getId(), "MANTENIMIENTO"));
        String err = service.asignarRepartidor(furgonetaA.getId(), carlos.getId());
        assertNotNull(err);
        assertTrue(err.contains("mantenimiento"));
    }

    @Test
    void desvincular_noSaca_de_mantenimiento() {
        service.cambiarEstado(furgonetaA.getId(), "MANTENIMIENTO");
        assertNull(service.asignarRepartidor(furgonetaA.getId(), null));
        assertEquals("MANTENIMIENTO", vehiculoRepository.findById(furgonetaA.getId()).get().getEstado());
    }

    @Test
    void noPermite_asignar_usuario_que_no_es_repartidor() {
        assertNotNull(service.asignarRepartidor(furgonetaA.getId(), cliente.getId()));
    }

    @Test
    void noPermite_desvincular_si_el_conductor_tiene_paquetes_activos() {
        service.asignarRepartidor(furgonetaA.getId(), carlos.getId());
        Guia g = guia(100);
        g.setRepartidorAsignado(carlos);
        guiaRepository.saveAndFlush(g);

        String err = service.asignarRepartidor(furgonetaA.getId(), null);
        assertNotNull(err);
        assertTrue(err.contains("guía"));
        assertNotNull(service.cambiarEstado(furgonetaA.getId(), "MANTENIMIENTO"));
        assertNotNull(service.eliminarVehiculo(furgonetaA.getId()));
        assertEquals("ASIGNADO", vehiculoRepository.findById(furgonetaA.getId()).get().getEstado());
    }

    @Test
    void cambiarEstado_rechaza_valores_invalidos() {
        assertNotNull(service.cambiarEstado(furgonetaA.getId(), "ASIGNADO"));
        assertNotNull(service.cambiarEstado(furgonetaA.getId(), "LO_QUE_SEA"));
    }

    @Test
    void validarAsignacion_contra_base_real() {
        service.asignarRepartidor(furgonetaA.getId(), carlos.getId()); // 1000 kg
        Guia g1 = guia(900);
        g1.setRepartidorAsignado(carlos);
        guiaRepository.saveAndFlush(g1);

        assertNull(service.validarAsignacion(carlos, guia(100)));          // 900 + 100 = límite exacto
        assertNotNull(service.validarAsignacion(carlos, guia(101)));       // se pasa
        assertNull(service.validarAsignacion(carlos, g1));                 // reasignar la misma: no cuenta doble
        assertNotNull(service.validarAsignacion(maria, guia(1)));          // maria no tiene furgoneta
    }

    @Test
    void guia_con_campos_nuevos_nulos_se_guarda_y_ruta_ordena_paradas() {
        Guia g1 = guia(10);
        Guia g2 = guia(20);
        assertNull(g1.getLatDestino());   // campos nuevos opcionales: guías viejas siguen válidas

        Ruta ruta = new Ruta();
        ruta.setRepartidor(carlos);
        ruta.setCapitalHub("Medellin");
        ruta.agregarGuia(g1);
        ruta.agregarGuia(g2);
        rutaRepository.saveAndFlush(ruta);
        em.clear();

        Ruta r = rutaRepository.findById(ruta.getId()).orElseThrow();
        assertEquals(2, r.getGuias().size());
        assertEquals(1, r.getGuias().get(0).getOrdenEntrega());
        assertEquals(2, r.getGuias().get(1).getOrdenEntrega());
    }
}

