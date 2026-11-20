package com.example.guias.controller;

import com.example.guias.ai.CalculadoraTools;
import com.example.guias.ai.GuiaTools;
import com.example.guias.model.Guia;
import com.example.guias.model.Rol;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.security.JwtUtil;
import com.example.guias.service.ChatRagService;
import com.example.guias.service.CodigoOtpService;
import com.example.guias.service.GuiaService;
import com.example.guias.service.SesionChatService;
import com.example.guias.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpringAiChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpringAiChatControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ChatRagService chatRagService;
    @MockBean private CalculadoraTools calculadoraTools;
    @MockBean private GuiaTools guiaTools;
    @MockBean private CodigoOtpService codigoOtpService;
    @MockBean private SesionChatService sesionChatService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private GuiaService guiaService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    private Usuario cliente() {
        Usuario u = new Usuario("Ana Cliente", "ana@x.com", "x", Rol.ROLE_CLIENTE);
        u.setId(5L);
        u.setAprobado(true);
        return u;
    }

    @Test
    void preguntaPublica_devuelveRespuestaDelRag() throws Exception {
        when(chatRagService.preguntar(anyString(), any(), any())).thenReturn("Hola, cobramos por peso.");

        mockMvc.perform(get("/chat/ia/spring-ai").param("q", "¿Cómo se calcula el precio?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.respuesta").value("Hola, cobramos por peso."));
    }

    @Test
    void preguntaVacia_otraMuyLarga_devuelven400() throws Exception {
        mockMvc.perform(get("/chat/ia/spring-ai").param("q", "   "))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/chat/ia/spring-ai").param("q", "a".repeat(1001)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errorDelModelo_devuelve500_para_que_el_frontend_use_el_respaldo() throws Exception {
        when(chatRagService.preguntar(anyString(), any(), any())).thenThrow(new RuntimeException("sin saldo"));

        mockMvc.perform(get("/chat/ia/spring-ai").param("q", "hola"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void limitePorMinuto_devuelve429_tras_25_consultas() throws Exception {
        when(chatRagService.preguntar(anyString(), any(), any())).thenReturn("ok");

        // IP propia para no compartir contador con otras pruebas (el controlador es singleton)
        for (int i = 0; i < 25; i++) {
            mockMvc.perform(get("/chat/ia/spring-ai").param("q", "hola")
                            .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/chat/ia/spring-ai").param("q", "hola")
                        .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void consultarGuia_sinVerificarCorreo_devuelve401() throws Exception {
        when(sesionChatService.obtenerClienteAutorizado(any())).thenReturn(null);

        mockMvc.perform(post("/chat/ia/consultar-guia").param("numeroGuia", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultarGuia_ajena_devuelve404_y_busca_solo_con_el_usuario_autorizado() throws Exception {
        Usuario ana = cliente();
        when(sesionChatService.obtenerClienteAutorizado(any())).thenReturn(5L);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(ana));
        when(guiaService.buscarPorIdYUsuario(99L, ana)).thenReturn(Optional.empty());

        mockMvc.perform(post("/chat/ia/consultar-guia").param("numeroGuia", "99"))
                .andExpect(status().isNotFound());

        verify(guiaService).buscarPorIdYUsuario(99L, ana);
    }

    @Test
    void consultarGuia_propia_devuelveDatosBasicos() throws Exception {
        Usuario ana = cliente();
        Guia guia = new Guia();
        guia.setId(10L);
        guia.setEstado("Pendiente");
        guia.setCiudadOrigen("Bogota");
        guia.setCiudadDestino("Medellin");
        when(sesionChatService.obtenerClienteAutorizado(any())).thenReturn(5L);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(ana));
        when(guiaService.buscarPorIdYUsuario(10L, ana)).thenReturn(Optional.of(guia));

        mockMvc.perform(post("/chat/ia/consultar-guia").param("numeroGuia", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guia.numero").value(10))
                .andExpect(jsonPath("$.guia.estado").value("Pendiente"))
                .andExpect(jsonPath("$.guia.destino").value("Medellin"));
    }

    @Test
    void verificarCodigo_invalido_devuelve401() throws Exception {
        when(codigoOtpService.verificarCodigo(anyString(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/chat/ia/verificar-codigo").param("codigo", "123456"))
                .andExpect(status().isUnauthorized());
    }
}

