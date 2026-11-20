package com.example.guias.controller;

import com.example.guias.dto.RepartidorVehiculoDTO;
import com.example.guias.model.Guia;
import com.example.guias.model.Rol;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.DireccionRepository;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.security.JwtUtil;
import com.example.guias.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderiza de verdad las plantillas Thymeleaf modificadas (javac no valida las expresiones ${...}).
 */
@WebMvcTest(controllers = {VehiculoController.class, GuiaController.class})
class PlantillasFlotaRenderTest {

    @Autowired private MockMvc mockMvc;

    // dependencias de VehiculoController / GuiaController
    @MockBean private VehiculoService vehiculoService;
    @MockBean private GuiaService guiaService;
    @MockBean private EmailService emailService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private PriceCalculatorService priceCalculatorService;
    @MockBean private WekaService wekaService;
    @MockBean private PdfService pdfService;
    @MockBean private DireccionRepository direccionRepository;
    @MockBean private MongoSyncService mongoSyncService;
    // beans que exige el filtro JWT / seguridad
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    private Usuario repartidor() {
        Usuario u = new Usuario("Carlos Perez", "carlos@x.com", "x", Rol.ROLE_REPARTIDOR);
        u.setId(7L);
        u.setAprobado(true);
        return u;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminVehiculos_renderiza_con_y_sin_datos() throws Exception {
        Usuario carlos = repartidor();
        Vehiculo v = new Vehiculo("ABC-123", "Renault Master", "FURGONETA", 1200.0, 8.5);
        v.setId(1L);
        v.setRepartidor(carlos);
        Vehiculo libre = new Vehiculo("XYZ-999", "Nissan Urvan", "FURGONETA", 900.0, 6.0);
        libre.setId(2L);

        when(vehiculoService.obtenerTodos()).thenReturn(List.of(v, libre));
        when(vehiculoService.obtenerRepartidoresDisponiblesSinVehiculo()).thenReturn(List.of(repartidor()));

        mockMvc.perform(get("/admin/vehiculos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ABC-123")))
                .andExpect(content().string(containsString("Carlos Perez")))
                .andExpect(content().string(containsString("Sin conductor")))
                .andExpect(content().string(containsString("1200 KG")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminVehiculos_renderiza_lista_vacia() throws Exception {
        when(vehiculoService.obtenerTodos()).thenReturn(List.of());
        when(vehiculoService.obtenerRepartidoresDisponiblesSinVehiculo()).thenReturn(List.of());

        mockMvc.perform(get("/admin/vehiculos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay vehículos registrados")));
    }

    @Test
    @WithMockUser(username = "admin@saicargo.com", roles = "ADMIN")
    void listaGuias_muestra_conductor_con_placa_y_etiqueta_de_ocupacion() throws Exception {
        Usuario cliente = new Usuario("Cliente", "cli@x.com", "x", Rol.ROLE_CLIENTE);
        Usuario carlos = repartidor();
        Vehiculo v = new Vehiculo("ABC-123", "Renault Master", "FURGONETA", 1200.0, 8.5);
        v.setRepartidor(carlos);
        carlos.setVehiculo(v);

        Guia g = new Guia();
        g.setId(5L);
        g.setRemitente("Juan");
        g.setDestinatario("Ana");
        g.setDireccionEnvio("Calle 1");
        g.setDireccionRecibo("Calle 2");
        g.setCiudadOrigen("Bogota");
        g.setCiudadDestino("Medellin");
        g.setDepartamentoOrigen("Cundinamarca");
        g.setDepartamentoDestino("Antioquia");
        g.setTipoCarga("Caja");
        g.setPesoKg(150);
        g.setPrecio(25000);
        g.setUsuario(cliente);
        g.setRepartidorAsignado(carlos);

        when(guiaService.obtenerTodas()).thenReturn(List.of(g));
        when(vehiculoService.obtenerRepartidoresConVehiculoDTO()).thenReturn(List.of(
                new RepartidorVehiculoDTO(7L, "Carlos Perez", "carlos@x.com", "ABC-123", "Renault Master", 1200.0, 780.0, 65.0)));

        mockMvc.perform(get("/guias/todas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("[ABC-123]")))                          // badge del conductor con placa
                .andExpect(content().string(containsString("Furgoneta [ABC-123] (Carga: 65% | 780/1200 kg)"))) // opción del modal
                .andExpect(content().string(containsString("assignRepartidorSelect")));
    }

    @Test
    @WithMockUser(username = "admin@saicargo.com", roles = "ADMIN")
    void listaGuias_avisa_si_no_hay_repartidores_con_furgoneta() throws Exception {
        when(guiaService.obtenerTodas()).thenReturn(List.of());
        when(vehiculoService.obtenerRepartidoresConVehiculoDTO()).thenReturn(List.of());

        mockMvc.perform(get("/guias/todas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay repartidores con furgoneta asignada")));
    }

    @Test
    @WithMockUser(username = "cliente@saicargo.com", roles = "CLIENTE")
    void crearGuia_renderiza_panel_de_rutas_osrm_y_campos_ocultos() throws Exception {
        Usuario cli = new Usuario("Cliente Prueba", "cliente@saicargo.com", "x", Rol.ROLE_CLIENTE);
        cli.setId(9L);
        when(usuarioRepository.findByUsername("cliente@saicargo.com")).thenReturn(Optional.of(cli));
        when(direccionRepository.findByUsuario(cli)).thenReturn(List.of());

        mockMvc.perform(get("/guias/crear"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Buscar rutas")))
                .andExpect(content().string(containsString("id=\"latOrigen\"")))
                .andExpect(content().string(containsString("id=\"lonOrigen\"")))
                .andExpect(content().string(containsString("id=\"latDestino\"")))
                .andExpect(content().string(containsString("id=\"lonDestino\"")))
                .andExpect(content().string(containsString("id=\"distanciaKm\"")))
                .andExpect(content().string(containsString("id=\"tiempoEstimadoMinutos\"")))
                .andExpect(content().string(containsString("Distancia Vial")));
    }

    @Test
    @WithMockUser(username = "cliente@saicargo.com", roles = "CLIENTE")
    void guardarGuia_persiste_coordenadas_distancia_y_tiempo_osrm() throws Exception {
        Usuario cli = new Usuario("Cliente Prueba", "cliente@saicargo.com", "x", Rol.ROLE_CLIENTE);
        cli.setId(9L);
        cli.setEmail("cliente@saicargo.com");
        when(usuarioRepository.findByUsername("cliente@saicargo.com")).thenReturn(Optional.of(cli));
        when(priceCalculatorService.calcularPrecio(any(Guia.class))).thenReturn(60390.0);
        when(wekaService.predecirTiempo(anyString(), anyString(), anyString())).thenReturn(2.0);
        when(guiaService.guardarGuia(any(Guia.class))).thenAnswer(inv -> {
            Guia saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        mockMvc.perform(post("/guias/crear")
                        .with(csrf())
                        .param("remitente", "Carlos Gomez")
                        .param("destinatario", "Ana Perez")
                        .param("cedulaRemitente", "10203040")
                        .param("cedulaDestinatario", "50607080")
                        .param("departamentoOrigen", "Cundinamarca")
                        .param("ciudadOrigen", "Bogotá D.C.")
                        .param("departamentoDestino", "Antioquia")
                        .param("ciudadDestino", "Medellín")
                        .param("direccionEnvio", "Calle 26 # 10-20")
                        .param("direccionRecibo", "Carrera 43A # 1-50")
                        .param("tipoCarga", "Caja Estándar")
                        .param("pesoKg", "10")
                        .param("anchoCm", "30")
                        .param("alturaCm", "30")
                        .param("valorDeclarado", "100000")
                        .param("latOrigen", "4.7110")
                        .param("lonOrigen", "-74.0721")
                        .param("latDestino", "6.2442")
                        .param("lonDestino", "-75.5812")
                        .param("distanciaKm", "415.638")
                        .param("tiempoEstimadoMinutos", "480"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Guia> captor = ArgumentCaptor.forClass(Guia.class);
        verify(guiaService).guardarGuia(captor.capture());
        Guia guardada = captor.getValue();
        assertEquals(4.7110, guardada.getLatOrigen(), 0.0001);
        assertEquals(-74.0721, guardada.getLonOrigen(), 0.0001);
        assertEquals(6.2442, guardada.getLatDestino(), 0.0001);
        assertEquals(-75.5812, guardada.getLonDestino(), 0.0001);
        assertEquals(415.64, guardada.getDistanciaKm(), 0.001);
        assertEquals(480, guardada.getTiempoEstimadoMinutos());
    }

    @Test
    @WithMockUser(username = "cliente@saicargo.com", roles = "CLIENTE")
    void guardarGuia_sanitiza_coordenadas_y_distancia_fuera_de_rango() throws Exception {
        Usuario cli = new Usuario("Cliente Prueba", "cliente@saicargo.com", "x", Rol.ROLE_CLIENTE);
        cli.setId(9L);
        when(usuarioRepository.findByUsername("cliente@saicargo.com")).thenReturn(Optional.of(cli));
        when(priceCalculatorService.calcularPrecio(any(Guia.class))).thenReturn(49500.0);
        when(wekaService.predecirTiempo(anyString(), anyString(), anyString())).thenReturn(1.5);
        when(guiaService.guardarGuia(any(Guia.class))).thenAnswer(inv -> {
            Guia saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        mockMvc.perform(post("/guias/crear")
                        .with(csrf())
                        .param("remitente", "Carlos Gomez")
                        .param("destinatario", "Ana Perez")
                        .param("cedulaRemitente", "10203040")
                        .param("cedulaDestinatario", "50607080")
                        .param("departamentoOrigen", "Cundinamarca")
                        .param("ciudadOrigen", "Bogotá D.C.")
                        .param("departamentoDestino", "Antioquia")
                        .param("ciudadDestino", "Medellín")
                        .param("direccionEnvio", "Calle 26 # 10-20")
                        .param("direccionRecibo", "Carrera 43A # 1-50")
                        .param("tipoCarga", "Caja Estándar")
                        .param("pesoKg", "10")
                        .param("anchoCm", "30")
                        .param("alturaCm", "30")
                        .param("valorDeclarado", "100000")
                        .param("latOrigen", "999.0")
                        .param("lonOrigen", "-74.0")
                        .param("distanciaKm", "-15.0")
                        .param("tiempoEstimadoMinutos", "0"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Guia> captor = ArgumentCaptor.forClass(Guia.class);
        verify(guiaService).guardarGuia(captor.capture());
        Guia guardada = captor.getValue();
        assertNull(guardada.getLatOrigen());
        assertNull(guardada.getLonOrigen());
        assertNull(guardada.getDistanciaKm());
        assertNull(guardada.getTiempoEstimadoMinutos());
    }
}

