package com.example.guias.controller;

import com.example.guias.model.Guia;
import com.example.guias.security.JwtUtil;
import com.example.guias.service.PriceCalculatorService;
import com.example.guias.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CotizacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CotizacionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PriceCalculatorService priceCalculatorService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    @Test
    void calcular_devuelvePesosYPrecio_y_enviaElLargoAlCalculador() throws Exception {
        when(priceCalculatorService.calcularPrecio(any(Guia.class))).thenReturn(48500.0);

        mockMvc.perform(post("/cotizacion/calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"Bogota\",\"destino\":\"Medellin\",\"altura\":10,"
                                + "\"ancho\":10,\"largo\":400,\"peso\":2,\"valorDeclarado\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(48500.0))
                .andExpect(jsonPath("$.pesoVolumetrico").value(10.0))
                .andExpect(jsonPath("$.pesoCobrable").value(10.0));

        ArgumentCaptor<Guia> captor = ArgumentCaptor.forClass(Guia.class);
        verify(priceCalculatorService).calcularPrecio(captor.capture());
        assertEquals(400.0, captor.getValue().getLargoCm());
    }

    @Test
    void calcular_conDatosInvalidos_devuelve400() throws Exception {
        mockMvc.perform(post("/cotizacion/calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"Bogota\",\"destino\":\"\",\"altura\":10,"
                                + "\"ancho\":10,\"largo\":10,\"peso\":0,\"valorDeclarado\":0}"))
                .andExpect(status().isBadRequest());
    }
}

