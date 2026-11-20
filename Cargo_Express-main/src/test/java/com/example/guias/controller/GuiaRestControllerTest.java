package com.example.guias.controller;

import com.example.guias.service.WekaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.guias.security.JwtUtil;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuiaRestController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test
class GuiaRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private com.example.guias.service.UsuarioDetailsService usuarioDetailsService;

    @MockBean
    private WekaService wekaService;

    @Test
    void testPredecirEmpaque() throws Exception {
        // Arrange
        when(wekaService.predecirEmpaque(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn("Caja de Cartón");

        // Act & Assert
        mockMvc.perform(get("/api/guias/predecir-empaque")
                .param("peso", "15.0")
                .param("ancho", "20.0")
                .param("alto", "30.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugerencia").value("Caja de Cartón"));
    }
}
