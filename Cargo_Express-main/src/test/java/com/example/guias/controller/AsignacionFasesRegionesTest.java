package com.example.guias.controller;

import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Guia;
import com.example.guias.model.Region;
import com.example.guias.model.Rol;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.DireccionRepository;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.security.CustomOAuth2UserService;
import com.example.guias.security.JwtUtil;
import com.example.guias.security.RolSuccessHandler;
import com.example.guias.security.SecurityConfig;
import com.example.guias.service.EmailService;
import com.example.guias.service.GuiaService;
import com.example.guias.service.MongoSyncService;
import com.example.guias.service.PdfService;
import com.example.guias.service.PriceCalculatorService;
import com.example.guias.service.UsuarioDetailsService;
import com.example.guias.service.VehiculoService;
import com.example.guias.service.WekaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {GuiaController.class, AuthController.class})
@org.springframework.context.annotation.Import(SecurityConfig.class)
class AsignacionFasesRegionesTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private GuiaService guiaService;
    @MockBean private VehiculoService vehiculoService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private MongoSyncService mongoSyncService;
    @MockBean private BCryptPasswordEncoder passwordEncoder;
    @MockBean private EmailService emailService;
    @MockBean private PriceCalculatorService priceCalculatorService;
    @MockBean private WekaService wekaService;
    @MockBean private PdfService pdfService;
    @MockBean private DireccionRepository direccionRepository;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private RolSuccessHandler rolSuccessHandler;
    @MockBean private UsuarioDetailsService usuarioDetailsService;

    private Usuario repartidor(Long id, String nombre, Region region) {
        Usuario u = new Usuario(nombre, nombre.toLowerCase() + "@x.com", "x", Rol.ROLE_REPARTIDOR);
        u.setId(id);
        u.setAprobado(true);
        u.setRegionOperacion(region);
        return u;
    }

    private Guia guiaBase(Long id, String estado, String deptoOrigen, String deptoDestino) {
        Guia g = new Guia();
        g.setId(id);
        g.setEstado(estado);
        g.setDepartamentoOrigen(deptoOrigen);
        g.setDepartamentoDestino(deptoDestino);
        g.setPesoKg(10.0);
        return g;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void entregaNoSePuedeAsignarSiGuiaSiguePendiente() throws Exception {
        Guia g = guiaBase(10L, EstadoGuia.PENDIENTE, "Atlántico", "Antioquia");
        when(guiaService.buscarPorId(10L)).thenReturn(Optional.of(g));

        mockMvc.perform(post("/guias/asignar-repartidor").with(csrf())
                        .param("guiaId", "10")
                        .param("repartidorId", "7")
                        .param("fase", "ENTREGA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guias/todas"))
                .andExpect(flash().attributeExists("error"));

        verify(guiaService, never()).guardarGuia(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recogidaBloqueaRepartidorDeOtraRegionYAceptaMismaRegion() throws Exception {
        Guia g = guiaBase(11L, EstadoGuia.PENDIENTE, "Atlántico", "Antioquia");
        Usuario repAndina = repartidor(7L, "Carlos", Region.ANDINA);
        Usuario repCaribe = repartidor(8L, "Luis", Region.CARIBE);
        Vehiculo van = new Vehiculo("CAR-100", "Master", "FURGONETA", 1000.0, 8.0);

        when(guiaService.buscarPorId(11L)).thenReturn(Optional.of(g));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(repAndina));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(repCaribe));
        when(vehiculoService.validarAsignacion(repCaribe, g)).thenReturn(null);
        when(vehiculoService.buscarPorRepartidor(repCaribe)).thenReturn(Optional.of(van));

        // Otra región -> error y no guarda
        mockMvc.perform(post("/guias/asignar-repartidor").with(csrf())
                        .param("guiaId", "11")
                        .param("repartidorId", "7")
                        .param("fase", "RECOGIDA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
        verify(guiaService, never()).guardarGuia(any());

        // Misma región -> guarda repartidorRecogida + repartidorAsignado
        mockMvc.perform(post("/guias/asignar-repartidor").with(csrf())
                        .param("guiaId", "11")
                        .param("repartidorId", "8")
                        .param("fase", "RECOGIDA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensaje"));

        assertEquals(repCaribe, g.getRepartidorRecogida());
        assertEquals(repCaribe, g.getRepartidorAsignado());
        verify(guiaService).guardarGuia(g);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void entregaSeHabilitaEnSucursalYValidaRegionDestino() throws Exception {
        Guia g = guiaBase(12L, EstadoGuia.EN_SUCURSAL, "Atlántico", "Antioquia");
        Usuario repAndina = repartidor(7L, "Carlos", Region.ANDINA);
        Vehiculo van = new Vehiculo("AND-200", "Urvan", "FURGONETA", 1000.0, 8.0);

        when(guiaService.buscarPorId(12L)).thenReturn(Optional.of(g));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(repAndina));
        when(vehiculoService.validarAsignacion(repAndina, g)).thenReturn(null);
        when(vehiculoService.buscarPorRepartidor(repAndina)).thenReturn(Optional.of(van));

        mockMvc.perform(post("/guias/asignar-repartidor").with(csrf())
                        .param("guiaId", "12")
                        .param("repartidorId", "7")
                        .param("fase", "ENTREGA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensaje"));

        assertEquals(repAndina, g.getRepartidorEntrega());
        assertEquals(repAndina, g.getRepartidorAsignado());
    }

    @Test
    void registroRepartidorExigeRegionDeOperacion() throws Exception {
        when(usuarioRepository.findByUsername("rep@x.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        // Sin aceptar términos (Ley 1581) -> rechaza y vuelve al formulario
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Cliente Sin Terminos")
                        .param("username", "cli@x.com")
                        .param("password", "secret123")
                        .param("rol", "ROLE_CLIENTE"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("error"));
        verify(usuarioRepository, never()).save(any());

        // Sin región -> rechaza y vuelve al formulario
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Rep Test")
                        .param("username", "rep@x.com")
                        .param("password", "secret123")
                        .param("rol", "ROLE_REPARTIDOR")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("error"));
        verify(usuarioRepository, never()).save(any());

        // Con región y términos aceptados -> guarda pendiente de aprobación
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Rep Test")
                        .param("username", "rep@x.com")
                        .param("password", "secret123")
                        .param("rol", "ROLE_REPARTIDOR")
                        .param("regionOperacion", "CARIBE")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered&pending"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Region.CARIBE, captor.getValue().getRegionOperacion());
        assertFalse(captor.getValue().isAprobado());
    }

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void adminPuedeOcultarGuiaSinBorrarDeBd() throws Exception {
        Usuario admin = new Usuario("Admin", "admin@x.com", "x", Rol.ROLE_ADMIN);
        admin.setId(1L);
        Usuario cliente = new Usuario("Cliente", "cli@x.com", "x", Rol.ROLE_CLIENTE);
        cliente.setId(2L);

        Guia guia = new Guia();
        guia.setId(77L);
        guia.setUsuario(cliente);
        guia.setEstado(EstadoGuia.PENDIENTE);

        when(usuarioRepository.findByUsername("admin@x.com")).thenReturn(Optional.of(admin));
        when(guiaService.buscarPorId(77L)).thenReturn(Optional.of(guia));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/guias/eliminar/77")
                        .header("Referer", "http://localhost:8080/guias/todas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guias/todas"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(guiaService).marcarComoEliminada(guia);
        verify(guiaService, never()).eliminarGuia(anyLong());
    }

    @Test
    @WithMockUser(username = "otro@x.com", roles = "CLIENTE")
    void clienteNoPropietarioNoPuedeOcultarGuiaAjena() throws Exception {
        Usuario dueno = new Usuario("Dueño", "dueno@x.com", "x", Rol.ROLE_CLIENTE);
        dueno.setId(2L);
        Usuario otro = new Usuario("Otro", "otro@x.com", "x", Rol.ROLE_CLIENTE);
        otro.setId(3L);

        Guia guia = new Guia();
        guia.setId(78L);
        guia.setUsuario(dueno);
        guia.setEstado(EstadoGuia.PENDIENTE);

        when(usuarioRepository.findByUsername("otro@x.com")).thenReturn(Optional.of(otro));
        when(guiaService.buscarPorId(78L)).thenReturn(Optional.of(guia));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/guias/eliminar/78")
                        .header("Referer", "http://localhost:8080/guias/mis-guias"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guias/mis-guias"))
                .andExpect(flash().attributeExists("error"));

        verify(guiaService, never()).marcarComoEliminada(any());
    }
}

