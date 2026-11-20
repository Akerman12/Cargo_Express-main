package com.example.guias.service;

import com.example.guias.dto.RutaPlanDTO;
import com.example.guias.dto.RutaResumenDTO;
import com.example.guias.model.Guia;
import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Region;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({RutaService.class, VehiculoService.class, GuiaService.class})
class RutaPersistenciaTest {

    @Autowired private RutaService rutaService;
    @Autowired private VehiculoService vehiculoService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private RutaRepository rutaRepository;
    @Autowired private EntityManager em;

    private Usuario cliente;
    private Usuario carlos;
    private Usuario maria;

    private Usuario crearUsuario(String nombre, String email, Rol rol) {
        Usuario u = new Usuario(nombre, email, "pass", rol);
        u.setEmail(email);
        u.setAprobado(true);
        return usuarioRepository.save(u);
    }

    private Guia crearGuia(String destinatario, String direccion, String ciudad, String depto,
                           Usuario repartidor, String estado) {
        Guia g = new Guia();
        g.setRemitente("Empresa Origen");
        g.setDestinatario(destinatario);
        g.setDireccionEnvio("Calle 1 # 2-3");
        g.setDireccionRecibo(direccion);
        g.setCedulaRemitente("100");
        g.setCedulaDestinatario("200");
        g.setCiudadOrigen("Bogota");
        g.setCiudadDestino(ciudad);
        g.setDepartamentoOrigen("Cundinamarca");
        g.setDepartamentoDestino(depto);
        g.setTipoCarga("Paquete");
        g.setPesoKg(10.0);
        g.setAlturaCm(20);
        g.setAnchoCm(20);
        g.setValorDeclarado(50000);
        g.setPrecio(25000);
        g.setUsuario(cliente);
        g.setRepartidorAsignado(repartidor);
        g.setEstado(estado);
        return guiaRepository.save(g);
    }

    @BeforeEach
    void setUp() {
        cliente = crearUsuario("Cliente Test", "cli-ruta@x.com", Rol.ROLE_CLIENTE);
        carlos = crearUsuario("Carlos Ruta", "carlos-ruta@x.com", Rol.ROLE_REPARTIDOR);
        carlos.setRegionOperacion(Region.ANDINA);
        usuarioRepository.save(carlos);
        maria = crearUsuario("Maria Ruta", "maria-ruta@x.com", Rol.ROLE_REPARTIDOR);
        maria.setRegionOperacion(Region.CARIBE);
        usuarioRepository.save(maria);

        Vehiculo v1 = vehiculoRepository.save(new Vehiculo("RUT-101", "Master", "FURGONETA", 1000.0, 8.0));
        Vehiculo v2 = vehiculoRepository.save(new Vehiculo("RUT-202", "Urvan", "FURGONETA", 800.0, 6.0));
        assertNull(vehiculoService.asignarRepartidor(v1.getId(), carlos.getId()));
        assertNull(vehiculoService.asignarRepartidor(v2.getId(), maria.getId()));
    }

    @Test
    void obtenerDatosPlanificacion_sugiereCapitalRegionalYFiltraPendientes() {
        Guia g1 = crearGuia("Ana", "Cra 43A # 1-50", "Medellin", "Antioquia", carlos, EstadoGuia.EN_SUCURSAL);
        Guia g2 = crearGuia("Luis", "Calle 10 # 35-20", "Envigado", "Antioquia", carlos, "En Bodega");
        crearGuia("Ya Entregada", "Calle 50", "Medellin", "Antioquia", carlos, "Entregado");

        em.flush();
        em.clear();

        RutaPlanDTO plan = rutaService.obtenerDatosRepartidorParaPlanificar(carlos.getId());
        assertEquals("RUT-101", plan.getVehiculoPlaca());
        assertEquals("MEDELLÍN", plan.getCapitalSugerida());
        assertFalse(plan.isTieneRutaActiva());
        assertEquals(2, plan.getParadas().size());
        assertEquals(List.of(g1.getId(), g2.getId()),
                plan.getParadas().stream().map(RutaPlanDTO.ParadaDTO::getGuiaId).toList());
    }

    @Test
    void crearYAsignarRuta_persisteOrdenYCoordenadasGeocodificadas() {
        Guia g1 = crearGuia("Parada A", "Cra 7 # 71-21", "Bogota", "Cundinamarca", carlos, EstadoGuia.EN_SUCURSAL);
        Guia g2 = crearGuia("Parada B", "Av Boyaca # 80-10", "Bogota", "Cundinamarca", carlos, EstadoGuia.EN_SUCURSAL);

        String geoJson = "{\"type\":\"LineString\",\"coordinates\":[[-74.0817,4.6097],[-74.055,4.655],[-74.09,4.69]]}";
        Ruta ruta = rutaService.crearYAsignarRuta(
                carlos.getId(),
                "BOGOTÁ",
                List.of(g2.getId(), g1.getId()),
                Map.of(
                        g2.getId(), new double[]{4.6901, -74.0902},
                        g1.getId(), new double[]{4.6551, -74.0552}
                ),
                18.45,
                42,
                geoJson
        );

        em.flush();
        em.clear();

        RutaResumenDTO detalle = rutaService.obtenerDetalleRuta(ruta.getId());
        assertEquals("ASIGNADA", detalle.getEstado());
        assertEquals("BOGOTÁ", detalle.getCapitalHub());
        assertEquals(18.45, detalle.getDistanciaTotalKm());
        assertEquals(42, detalle.getTiempoEstimadoMinutos());
        assertEquals(geoJson, detalle.getGeometriaJson());
        assertEquals(2, detalle.getParadas().size());

        // Verifica orden 1 -> g2, orden 2 -> g1 y persistencia de lat/lon destino
        assertEquals(g2.getId(), detalle.getParadas().get(0).getGuiaId());
        assertEquals(1, detalle.getParadas().get(0).getOrdenEntrega());
        assertEquals(4.6901, detalle.getParadas().get(0).getLatDestino());
        assertEquals(g1.getId(), detalle.getParadas().get(1).getGuiaId());
        assertEquals(2, detalle.getParadas().get(1).getOrdenEntrega());
        assertEquals(4.6551, detalle.getParadas().get(1).getLatDestino());

        // Ya no quedan guías pendientes por enrutar y el repartidor tiene ruta activa
        RutaPlanDTO nuevoPlan = rutaService.obtenerDatosRepartidorParaPlanificar(carlos.getId());
        assertTrue(nuevoPlan.isTieneRutaActiva());
        assertEquals(0, nuevoPlan.getParadas().size());
    }

    @Test
    void crearYAsignarRuta_bloqueaDobleRutaActivaYGuiasAjenas() {
        Guia gCarlos = crearGuia("Destinatario Uno", "Calle 10", "Bogota", "Cundinamarca", carlos, EstadoGuia.EN_SUCURSAL);
        Guia gMaria = crearGuia("Destinatario Dos", "Calle 20", "Barranquilla", "Atlantico", maria, EstadoGuia.EN_SUCURSAL);

        // No puede meter guía de Maria en ruta de Carlos
        IllegalArgumentException ajenaEx = assertThrows(IllegalArgumentException.class, () ->
                rutaService.crearYAsignarRuta(carlos.getId(), "BOGOTÁ", List.of(gMaria.getId()), Map.of(), 5.0, 15, "{}"));
        assertTrue(ajenaEx.getMessage().contains("no está asignada"));

        // Asigna ruta válida a Carlos
        rutaService.crearYAsignarRuta(carlos.getId(), "BOGOTÁ", List.of(gCarlos.getId()), Map.of(), 5.0, 15, "{}");
        Guia gCarlos2 = crearGuia("Destinatario Tres", "Calle 30", "Bogota", "Cundinamarca", carlos, EstadoGuia.EN_SUCURSAL);

        // Bloquea crear una segunda ruta simultánea mientras haya una ASIGNADA
        IllegalArgumentException activaEx = assertThrows(IllegalArgumentException.class, () ->
                rutaService.crearYAsignarRuta(carlos.getId(), "BOGOTÁ", List.of(gCarlos2.getId()), Map.of(), 8.0, 20, "{}"));
        assertTrue(activaEx.getMessage().contains("ya tiene una ruta"));
    }

    @Test
    void cancelarRuta_liberaGuiasSiEstaAsignadaYBloqueaSiEstaEnCurso() {
        Guia g1 = crearGuia("Destinatario Cuatro", "Calle 1", "Medellin", "Antioquia", carlos, EstadoGuia.EN_SUCURSAL);
        Ruta ruta = rutaService.crearYAsignarRuta(carlos.getId(), "MEDELLÍN", List.of(g1.getId()), Map.of(), 12.0, 30, "{}");

        em.flush();
        em.clear();

        rutaService.cancelarRuta(ruta.getId());
        em.flush();
        em.clear();

        assertTrue(rutaRepository.findById(ruta.getId()).isEmpty());
        Guia liberada = guiaRepository.findById(g1.getId()).orElseThrow();
        assertNull(liberada.getRuta());
        assertNull(liberada.getOrdenEntrega());

        // Crea nueva ruta y pásala a EN_CURSO: ya no se puede cancelar
        Ruta ruta2 = rutaService.crearYAsignarRuta(carlos.getId(), "MEDELLÍN", List.of(g1.getId()), Map.of(), 12.0, 30, "{}");
        ruta2.setEstado(RutaService.EN_CURSO);
        rutaRepository.saveAndFlush(ruta2);

        IllegalArgumentException enCursoEx = assertThrows(IllegalArgumentException.class, () ->
                rutaService.cancelarRuta(ruta2.getId()));
        assertTrue(enCursoEx.getMessage().contains("ASIGNADA"));
    }

    @Test
    void iniciarRutaYCompletarAlEntregarUltimaParada() {
        Guia g1 = crearGuia("Cliente Uno", "Cra 10 # 20-30", "Bogota", "Cundinamarca", carlos, EstadoGuia.EN_SUCURSAL);
        Guia g2 = crearGuia("Cliente Dos", "Calle 40 # 15-20", "Bogota", "Cundinamarca", carlos, "En Bodega");
        Ruta ruta = rutaService.crearYAsignarRuta(
                carlos.getId(), "BOGOTÁ", List.of(g1.getId(), g2.getId()), Map.of(), 10.0, 25, "{}");

        em.flush();
        em.clear();

        RutaResumenDTO iniciada = rutaService.iniciarRutaRepartidor(carlos, "carlos@x.com").orElseThrow();
        assertEquals(RutaService.EN_CURSO, iniciada.getEstado());

        em.flush();
        em.clear();

        Guia g1Actual = guiaRepository.findById(g1.getId()).orElseThrow();
        Guia g2Actual = guiaRepository.findById(g2.getId()).orElseThrow();
        assertEquals("En Tránsito", g1Actual.getEstado());
        assertEquals("En Tránsito", g2Actual.getEstado());

        // Entregar primera parada: ruta sigue EN_CURSO y no permite finalizar sin forzar
        g1Actual.setEstado("Entregado");
        guiaRepository.saveAndFlush(g1Actual);
        rutaService.verificarCompletarRutaPorGuia(g1Actual);
        assertEquals(RutaService.EN_CURSO, rutaRepository.findById(ruta.getId()).orElseThrow().getEstado());
        assertThrows(IllegalArgumentException.class, () -> rutaService.finalizarRutaRepartidor(carlos, false));

        // Entregar segunda parada: ruta pasa automáticamente a COMPLETADA
        g2Actual.setEstado("Entregado");
        guiaRepository.saveAndFlush(g2Actual);
        rutaService.verificarCompletarRutaPorGuia(g2Actual);
        em.flush();
        em.clear();

        assertEquals(RutaService.COMPLETADA, rutaRepository.findById(ruta.getId()).orElseThrow().getEstado());
        assertTrue(rutaService.obtenerRutaActivaPorRepartidor(carlos).isEmpty());
    }

    @Test
    void rutaRecogida_multiParada_usaCoordenadasOrigenYFinalizaEnSucursalYNoRecogido() {
        Guia p1 = crearGuia("Destinatario Uno", "Calle Dest", "Barranquilla", "Atlantico", carlos, EstadoGuia.PENDIENTE);
        Guia p2 = crearGuia("Destinatario Dos", "Calle Dest 2", "Cali", "Valle del Cauca", carlos, EstadoGuia.PENDIENTE);

        RutaPlanDTO plan = rutaService.obtenerDatosRepartidorParaPlanificar(carlos.getId(), RutaService.TIPO_RECOGIDA);
        assertEquals(RutaService.TIPO_RECOGIDA, plan.getTipo());
        assertEquals(2, plan.getParadas().size());

        Ruta ruta = rutaService.crearYAsignarRuta(
                carlos.getId(),
                "BOGOTÁ",
                List.of(p2.getId(), p1.getId()),
                Map.of(p2.getId(), new double[]{4.65, -74.06}, p1.getId(), new double[]{4.68, -74.08}),
                11.0,
                28,
                "{}",
                RutaService.TIPO_RECOGIDA
        );
        assertEquals(RutaService.TIPO_RECOGIDA, ruta.getTipo());

        // Iniciar ruta de recogida NO cambia Pendiente -> En Tránsito
        rutaService.iniciarRutaRepartidor(carlos, "carlos-ruta@x.com");
        em.flush();
        em.clear();
        Guia g1 = guiaRepository.findById(p1.getId()).orElseThrow();
        Guia g2 = guiaRepository.findById(p2.getId()).orElseThrow();
        assertEquals(EstadoGuia.PENDIENTE, g1.getEstado());
        assertEquals(1, g2.getOrdenRecogida());
        assertEquals(2, g1.getOrdenRecogida());
        assertEquals(4.65, g2.getLatOrigen());

        // Marca p2 como Recogido, p1 queda Pendiente -> no deja cerrar sin forzar
        g2.setEstado(EstadoGuia.RECOGIDO);
        guiaRepository.saveAndFlush(g2);
        assertThrows(IllegalArgumentException.class, () -> rutaService.finalizarRutaRepartidor(carlos, false));

        // Al forzar cierre: p2 pasa a En Sucursal, p1 pasa a No Recogido, ambos sin repartidorAsignado
        rutaService.finalizarRutaRepartidor(carlos, true);
        em.flush();
        em.clear();

        assertEquals(RutaService.COMPLETADA, rutaRepository.findById(ruta.getId()).orElseThrow().getEstado());
        Guia finalP2 = guiaRepository.findById(p2.getId()).orElseThrow();
        Guia finalP1 = guiaRepository.findById(p1.getId()).orElseThrow();
        assertEquals(EstadoGuia.EN_SUCURSAL, finalP2.getEstado());
        assertNull(finalP2.getRepartidorAsignado());
        assertEquals(EstadoGuia.NO_RECOGIDO, finalP1.getEstado());
        assertNotNull(finalP1.getMotivoNovedad());
        assertNull(finalP1.getRepartidorAsignado());
    }

    @Test
    void crearYAsignarRuta_bloqueaHubOParadaFueraDeLaRegionDelRepartidor() {
        Guia enAndina = crearGuia("Destinatario Uno", "Cra 10", "Medellin", "Antioquia", maria, EstadoGuia.EN_SUCURSAL);
        // Maria opera en CARIBE -> rechaza hub MEDELLÍN
        IllegalArgumentException hubEx = assertThrows(IllegalArgumentException.class, () ->
                rutaService.crearYAsignarRuta(maria.getId(), "MEDELLÍN", List.of(enAndina.getId()), Map.of(), 5.0, 15, "{}"));
        assertTrue(hubEx.getMessage().contains("no pertenece"));

        // Maria opera en CARIBE -> con hub BARRANQUILLA rechaza parada en Antioquia (ANDINA)
        IllegalArgumentException paradaEx = assertThrows(IllegalArgumentException.class, () ->
                rutaService.crearYAsignarRuta(maria.getId(), "BARRANQUILLA", List.of(enAndina.getId()), Map.of(), 5.0, 15, "{}"));
        assertTrue(paradaEx.getMessage().contains("opera en la"));
    }
}
