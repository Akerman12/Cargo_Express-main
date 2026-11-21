package com.example.guias.controller;

import com.example.guias.model.Rol;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.security.JwtUtil;
import com.example.guias.service.GuiaService;
import com.example.guias.service.MongoSyncService;
import com.example.guias.service.UsuarioDetailsService;
import com.example.guias.service.VehiculoService;
import com.example.guias.service.WekaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClienteController.class, MenuController.class, AdminController.class})
class ConsistenciaVisualTemplatesTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockBean private GuiaService guiaService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private BCryptPasswordEncoder passwordEncoder;
    @MockBean private MongoSyncService mongoSyncService;
    @MockBean private VehiculoService vehiculoService;
    @MockBean private WekaService wekaService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(username = "cli@x.com", roles = "CLIENTE")
    void portalCliente_usaSidebarCorporativoYSinEnlacesAdmin() throws Exception {
        Usuario cli = new Usuario("Ana Cliente", "cli@x.com", "x", Rol.ROLE_CLIENTE);
        cli.setId(1L);
        cli.setEmail("cli@x.com");
        when(usuarioRepository.findByUsername("cli@x.com")).thenReturn(Optional.of(cli));
        when(guiaService.obtenerPorUsuario(cli)).thenReturn(List.of());

        mockMvc.perform(get("/cliente"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/sai-theme.css")))
                .andExpect(content().string(containsString("id=\"saiSidebar\"")))
                .andExpect(content().string(containsString("Mi Portal")))
                .andExpect(content().string(containsString("Mis Direcciones")))
                .andExpect(content().string(not(containsString("/admin/vehiculos"))))
                .andExpect(content().string(not(containsString("/admin/powerbi"))));
    }

    @Test
    @WithMockUser(username = "admin@saicargo.com", roles = "ADMIN")
    void menuAdmin_renderizaSidebarCompletoYCanvasEstados() throws Exception {
        when(guiaService.obtenerTodas()).thenReturn(List.of());

        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/sai-theme.css")))
                .andExpect(content().string(containsString("id=\"chartEstados\"")))
                .andExpect(content().string(containsString("/admin/rutas")))
                .andExpect(content().string(containsString("/admin/vehiculos")))
                .andExpect(content().string(containsString("/pago/admin/ver")))
                .andExpect(content().string(containsString("/admin/powerbi")))
                .andExpect(content().string(containsString("/admin/warroom")));
    }

    @Test
    @WithMockUser(username = "admin@saicargo.com", roles = "ADMIN")
    void adminPowerBi_renderizaIframeParaAdmin() throws Exception {
        mockMvc.perform(get("/admin/powerbi"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/sai-theme.css")))
                .andExpect(content().string(containsString("id=\"saiSidebar\"")))
                .andExpect(content().string(containsString("title=\"CargoExpress\"")))
                .andExpect(content().string(containsString(
                        "https://app.powerbi.com/view?r=eyJrIjoiNTU3NGZlNzItMjk0Yi00NGNiLTgzYzEtYmM2ZWMyYjdlNmUxIiwidCI6IjlkMTJiZjNmLWU0ZjYtNDdhYi05MTJmLTFhMmYwZmM0OGFhNCIsImMiOjR9")));
    }

    @Test
    @WithMockUser(username = "cli@x.com", roles = "CLIENTE")
    void adminPowerBi_deniegaAccesoACliente() throws Exception {
        mockMvc.perform(get("/admin/powerbi"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rep@x.com", roles = "REPARTIDOR")
    void adminPowerBi_deniegaAccesoARepartidor() throws Exception {
        mockMvc.perform(get("/admin/powerbi"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@saicargo.com", roles = "ADMIN")
    void adminUsuarios_renderizaSidebarUnificado() throws Exception {
        when(usuarioRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/sai-theme.css")))
                .andExpect(content().string(containsString("/pago/admin/ver")))
                .andExpect(content().string(containsString("/admin/rutas")));
    }
}

