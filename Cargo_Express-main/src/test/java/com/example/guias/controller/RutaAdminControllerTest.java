package com.example.guias.controller;

import com.example.guias.dto.RepartidorVehiculoDTO;
import com.example.guias.dto.RutaPlanDTO;
import com.example.guias.dto.RutaResumenDTO;
import com.example.guias.model.Ruta;
import com.example.guias.model.Usuario;
import com.example.guias.security.JwtUtil;
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
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RutaAdminController.class)
class RutaAdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockBean private RutaService rutaService;
    @MockBean private VehiculoService vehiculoService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void planificador_renderiza_vacio_y_con_rutas() throws Exception {
        RutaResumenDTO rutaDto = new RutaResumenDTO();
        rutaDto.setId(9L);
        rutaDto.setEstado("ASIGNADA");
        rutaDto.setCapitalHub("MEDELLÍN");
        rutaDto.setRepartidorNombre("Carlos Perez");
        rutaDto.setVehiculoPlaca("ABC-123");
        rutaDto.setCantidadParadas(3);
        rutaDto.setPesoTotalKg(45.5);
        rutaDto.setDistanciaTotalKm(21.4);
        rutaDto.setTiempoEstimadoMinutos(52);

        when(vehiculoService.obtenerRepartidoresConVehiculoDTO()).thenReturn(List.of(
                new RepartidorVehiculoDTO(7L, "Carlos Perez", "carlos@x.com", "ABC-123", "Master", 1000.0, 45.5, 5.0)));
        when(rutaService.obtenerTodasLasRutas()).thenReturn(List.of(rutaDto));

        mockMvc.perform(get("/admin/rutas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planificador de Rutas Multi-Parada")))
                .andExpect(content().string(containsString("ABC-123")))
                .andExpect(content().string(containsString("MEDELLÍN")))
                .andExpect(content().string(containsString("21.4 km")))
                .andExpect(content().string(containsString("#R-9")));
    }

    @Test
    @WithMockUser(roles = "REPARTIDOR")
    void rutasAdmin_rechazaNoAdministradoresCon403() throws Exception {
        mockMvc.perform(get("/admin/rutas"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/rutas/asignar").with(csrf())
                        .param("repartidorId", "7")
                        .param("capitalHub", "BOGOTÁ")
                        .param("guiaIdsJson", "[1]")
                        .param("distanciaTotalKm", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void endpointsJson_y_postAsignar_funcionanCorrectamente() throws Exception {
        RutaPlanDTO plan = new RutaPlanDTO();
        plan.setRepartidorId(7L);
        plan.setRepartidorNombre("Carlos Perez");
        plan.setVehiculoPlaca("ABC-123");
        plan.setCapitalSugerida("BOGOTÁ");
        when(rutaService.obtenerDatosRepartidorParaPlanificar(7L, "ENTREGA")).thenReturn(plan);
        when(rutaService.obtenerDatosRepartidorParaPlanificar(7L, "RECOGIDA")).thenReturn(plan);

        mockMvc.perform(get("/admin/rutas/datos-repartidor/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiculoPlaca").value("ABC-123"))
                .andExpect(jsonPath("$.capitalSugerida").value("BOGOTÁ"));

        Ruta guardada = new Ruta();
        guardada.setId(15L);
        Usuario rep = new Usuario();
        rep.setNombre("Carlos Perez");
        guardada.setRepartidor(rep);
        guardada.setCapitalHub("BOGOTÁ");
        guardada.setDistanciaTotalKm(14.2);

        when(rutaService.crearYAsignarRuta(
                eq(7L), eq("BOGOTÁ"), eq(List.of(10L, 12L)), anyMap(), eq(14.2), eq(35), eq("{\"type\":\"LineString\"}"), eq("ENTREGA")))
                .thenReturn(guardada);

        mockMvc.perform(post("/admin/rutas/asignar").with(csrf())
                        .param("repartidorId", "7")
                        .param("capitalHub", "BOGOTÁ")
                        .param("guiaIdsJson", "[10, 12]")
                        .param("coordenadasJson", "{\"10\":{\"lat\":4.65,\"lon\":-74.05}}")
                        .param("distanciaTotalKm", "14.2")
                        .param("tiempoEstimadoMinutos", "35")
                        .param("geometriaJson", "{\"type\":\"LineString\"}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/rutas"))
                .andExpect(flash().attributeExists("mensaje"));

        mockMvc.perform(post("/admin/rutas/cancelar/15").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/rutas"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(rutaService).cancelarRuta(15L);
    }
}

