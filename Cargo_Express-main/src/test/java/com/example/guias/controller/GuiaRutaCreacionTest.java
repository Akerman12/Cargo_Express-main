package com.example.guias.controller;

import com.example.guias.model.Guia;
import com.example.guias.security.CustomOAuth2UserService;
import com.example.guias.security.JwtUtil;
import com.example.guias.security.RolSuccessHandler;
import com.example.guias.security.SecurityConfig;
import com.example.guias.service.GuiaService;
import com.example.guias.service.UsuarioDetailsService;
import com.example.guias.service.WekaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RastreoController.class)
@Import(SecurityConfig.class)
class GuiaRutaCreacionTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private GuiaService guiaService;
    @MockBean private WekaService wekaService;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;
    @MockBean private RolSuccessHandler rolSuccessHandler;

    @Test
    void rastreoPublico_usaDistanciaVialAlmacenadaEnLugarDeHaversinePlano() throws Exception {
        Guia guia = new Guia();
        guia.setId(42L);
        guia.setCiudadOrigen("Bogotá D.C.");
        guia.setCiudadDestino("Medellín");
        guia.setPesoKg(10.0);
        guia.setEstado("Pendiente");
        guia.setDistanciaKm(415.6);

        when(guiaService.buscarPorId(42L)).thenReturn(Optional.of(guia));
        when(wekaService.predecirCO2(415.6, 10.0)).thenReturn(33.25);

        mockMvc.perform(get("/rastreo").param("id", "42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("distanciaKm", 415.6))
                .andExpect(model().attribute("co2Kg", 33.25))
                .andExpect(content().string(containsString("415.6")));

        verify(wekaService).predecirCO2(415.6, 10.0);
    }
}

