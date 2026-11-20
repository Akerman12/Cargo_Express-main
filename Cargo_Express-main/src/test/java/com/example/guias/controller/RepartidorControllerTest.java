package com.example.guias.controller;

import com.example.guias.dto.RutaPlanDTO;
import com.example.guias.dto.RutaResumenDTO;
import com.example.guias.model.Guia;
import com.example.guias.model.Rol;
import com.example.guias.model.Ruta;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.security.JwtUtil;
import com.example.guias.service.GuiaService;
import com.example.guias.service.MongoSyncService;
import com.example.guias.service.RutaService;
import com.example.guias.service.UsuarioDetailsService;
import com.example.guias.service.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RepartidorController.class)
class RepartidorControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockBean private GuiaService guiaService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private RutaService rutaService;
    @MockBean private VehiculoService vehiculoService;
    @MockBean private MongoSyncService mongoSyncService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    private Usuario crearRepartidor(Long id, String username, String nombre) {
        Usuario u = new Usuario(nombre, username, "pass", Rol.ROLE_REPARTIDOR);
        u.setId(id);
        u.setEmail(username);
        u.setAprobado(true);
        return u;
    }

    @Test
    @WithMockUser(username = "cliente@x.com", roles = "CLIENTE")
    void reparto_bloqueaClientesCon403() throws Exception {
        mockMvc.perform(get("/reparto"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "carlos@x.com", roles = "REPARTIDOR")
    void reparto_renderizaRutaAsignada_y_tabsEntregaYRecogida() throws Exception {
        Usuario carlos = crearRepartidor(7L, "carlos@x.com", "Carlos Repartidor");
        Vehiculo van = new Vehiculo("ABC-123", "Master", "FURGONETA", 1000.0, 8.0);

        Ruta ruta = new Ruta();
        ruta.setId(21L);
        ruta.setEstado("ASIGNADA");

        Guia entregaConCoors = new Guia();
        entregaConCoors.setId(101L);
        entregaConCoors.setRemitente("Empresa Uno");
        entregaConCoors.setDestinatario("Destinatario Uno");
        entregaConCoors.setDireccionEnvio("Calle 1");
        entregaConCoors.setDireccionRecibo("Cra 43A # 1-50");
        entregaConCoors.setCiudadOrigen("Medellin");
        entregaConCoors.setCiudadDestino("Envigado");
        entregaConCoors.setDepartamentoOrigen("Antioquia");
        entregaConCoors.setDepartamentoDestino("Antioquia");
        entregaConCoors.setPesoKg(12.5);
        entregaConCoors.setEstado("En Sucursal");
        entregaConCoors.setOrdenEntrega(1);
        entregaConCoors.setLatDestino(6.17);
        entregaConCoors.setLonDestino(-75.59);
        entregaConCoors.setRuta(ruta);
        entregaConCoors.setRepartidorAsignado(carlos);

        Guia recogidaSinRuta = new Guia();
        recogidaSinRuta.setId(102L);
        recogidaSinRuta.setRemitente("Remitente Dos");
        recogidaSinRuta.setDestinatario("Destinatario Dos");
        recogidaSinRuta.setDireccionEnvio("Calle 50 # 10-20");
        recogidaSinRuta.setDireccionRecibo("Calle 80");
        recogidaSinRuta.setCiudadOrigen("Bello");
        recogidaSinRuta.setCiudadDestino("Bogota");
        recogidaSinRuta.setDepartamentoOrigen("Antioquia");
        recogidaSinRuta.setDepartamentoDestino("Cundinamarca");
        recogidaSinRuta.setPesoKg(4.0);
        recogidaSinRuta.setEstado("Pendiente");
        recogidaSinRuta.setRepartidorAsignado(carlos);

        RutaResumenDTO resumen = new RutaResumenDTO();
        resumen.setId(21L);
        resumen.setEstado("ASIGNADA");
        resumen.setCapitalHub("MEDELLÍN");
        resumen.setCantidadParadas(1);
        resumen.setDistanciaTotalKm(15.2);
        resumen.setTiempoEstimadoMinutos(32);
        resumen.setPesoTotalKg(12.5);

        when(usuarioRepository.findByUsername("carlos@x.com")).thenReturn(Optional.of(carlos));
        when(vehiculoService.buscarPorRepartidor(carlos)).thenReturn(Optional.of(van));
        when(rutaService.obtenerRutaActivaPorRepartidor(carlos)).thenReturn(Optional.of(resumen));
        when(guiaService.obtenerPorRepartidorAsignado(carlos)).thenReturn(List.of(entregaConCoors, recogidaSinRuta));

        mockMvc.perform(get("/reparto"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DESPLEGAR MAPA DE LA RUTA ASIGNADA")))
                .andExpect(content().string(containsString("EMPEZAR RUTA")))
                .andExpect(content().string(containsString("ABC-123")))
                .andExpect(content().string(containsString("Parada #1")))
                .andExpect(content().string(containsString("Ruta de Entrega (<span>1</span>)")))
                .andExpect(content().string(containsString("Recogidas (<span>1</span>)")));
    }

    @Test
    @WithMockUser(username = "carlos@x.com", roles = "REPARTIDOR")
    void rutaMapaFull_renderiza_y_redirigeSinRuta() throws Exception {
        Usuario carlos = crearRepartidor(7L, "carlos@x.com", "Carlos");
        when(usuarioRepository.findByUsername("carlos@x.com")).thenReturn(Optional.of(carlos));

        RutaPlanDTO.ParadaDTO parada = new RutaPlanDTO.ParadaDTO();
        parada.setGuiaId(101L);
        parada.setOrdenEntrega(1);
        parada.setDestinatario("Destinatario Uno");
        parada.setDireccionRecibo("Cra 7 # 45-10");
        parada.setCiudadDestino("Bogota");
        parada.setEstado("EN_CURSO");

        RutaResumenDTO resumen = new RutaResumenDTO();
        resumen.setId(9L);
        resumen.setCapitalHub("BOGOTÁ");
        resumen.setEstado("EN_CURSO");
        resumen.setCantidadParadas(1);
        resumen.setParadas(List.of(parada));

        when(rutaService.obtenerRutaActivaPorRepartidor(carlos)).thenReturn(Optional.of(resumen));
        mockMvc.perform(get("/reparto/ruta-mapa"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Secuencia de Entregas")))
                .andExpect(content().string(containsString("Destinatario Uno")));

        when(rutaService.obtenerRutaActivaPorRepartidor(carlos)).thenReturn(Optional.empty());
        mockMvc.perform(get("/reparto/ruta-mapa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reparto"));
    }

    @Test
    @WithMockUser(username = "carlos@x.com", roles = "REPARTIDOR")
    void accionesPostReparto_validanPropiedad_inicianRuta_reportanNovedad_y_finalizanEntrega() throws Exception {
        Usuario carlos = crearRepartidor(7L, "carlos@x.com", "Carlos");
        Usuario otro = crearRepartidor(8L, "otro@x.com", "Otro");

        Guia deCarlos = new Guia();
        deCarlos.setId(50L);
        deCarlos.setRepartidorAsignado(carlos);
        deCarlos.setEstado("En Tránsito");

        Guia deOtro = new Guia();
        deOtro.setId(51L);
        deOtro.setRepartidorAsignado(otro);
        deOtro.setEstado("Pendiente");

        when(usuarioRepository.findByUsername("carlos@x.com")).thenReturn(Optional.of(carlos));
        when(guiaService.buscarPorId(50L)).thenReturn(Optional.of(deCarlos));
        when(guiaService.buscarPorId(51L)).thenReturn(Optional.of(deOtro));

        RutaResumenDTO rutaIniciada = new RutaResumenDTO();
        rutaIniciada.setId(30L);
        rutaIniciada.setEstado("EN_CURSO");
        when(rutaService.iniciarRutaRepartidor(eq(carlos), any())).thenReturn(Optional.of(rutaIniciada));

        // 1. Iniciar ruta JSON
        mockMvc.perform(post("/reparto/iniciar-ruta").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.ruta.estado").value("EN_CURSO"));

        // 2. Actualizar estado ajeno -> 403
        mockMvc.perform(post("/reparto/actualizar-estado").with(csrf())
                        .param("id", "51")
                        .param("estado", "En Bodega"))
                .andExpect(status().isForbidden());

        // 3. Reportar novedad propio
        mockMvc.perform(post("/reparto/reportar-novedad").with(csrf())
                        .param("guiaId", "50")
                        .param("motivo", "Cliente ausente"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reparto"))
                .andExpect(flash().attributeExists("mensaje"));

        // 4. Finalizar entrega verifica completitud de ruta
        String firmaLarga = "data:image/jpeg;base64," + "A".repeat(120);
        mockMvc.perform(post("/reparto/finalizar-entrega").with(csrf())
                        .param("guiaId", "50")
                        .param("firma", firmaLarga)
                        .param("notas", "Entregado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reparto"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(rutaService).verificarCompletarRutaPorGuia(deCarlos);
    }

    @Test
    @WithMockUser(username = "carlos@x.com", roles = "REPARTIDOR")
    void flujoRecogida_recogido_enSucursal_noRecogido_y_transicionesInvalidas() throws Exception {
        Usuario carlos = crearRepartidor(7L, "carlos@x.com", "Carlos");
        Guia pendiente = new Guia();
        pendiente.setId(60L);
        pendiente.setRepartidorAsignado(carlos);
        pendiente.setEstado("Pendiente");

        when(usuarioRepository.findByUsername("carlos@x.com")).thenReturn(Optional.of(carlos));
        when(guiaService.buscarPorId(60L)).thenReturn(Optional.of(pendiente));

        // No puede saltar directo de Pendiente a En Tránsito
        mockMvc.perform(post("/reparto/actualizar-estado").with(csrf())
                        .param("id", "60")
                        .param("estado", "En Tránsito"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("INVALID_TRANSITION"));

        // Pendiente -> Recogido -> En Sucursal
        mockMvc.perform(post("/reparto/actualizar-estado").with(csrf())
                        .param("id", "60")
                        .param("estado", "Recogido"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        mockMvc.perform(post("/reparto/actualizar-estado").with(csrf())
                        .param("id", "60")
                        .param("estado", "En Sucursal"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
        assertNull(pendiente.getRepartidorAsignado());

        // Otra guía Pendiente -> No Recogido
        Guia paraNoRecoger = new Guia();
        paraNoRecoger.setId(61L);
        paraNoRecoger.setRepartidorAsignado(carlos);
        paraNoRecoger.setEstado("Pendiente");
        when(guiaService.buscarPorId(61L)).thenReturn(Optional.of(paraNoRecoger));

        mockMvc.perform(post("/reparto/no-recogido").with(csrf())
                        .param("guiaId", "61")
                        .param("motivo", "Remitente ausente"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reparto"))
                .andExpect(flash().attributeExists("mensaje"));

        assertEquals("No Recogido", paraNoRecoger.getEstado());
        assertEquals("Remitente ausente", paraNoRecoger.getMotivoNovedad());
        verify(rutaService).verificarCompletarRutaPorGuia(paraNoRecoger);
    }
}


