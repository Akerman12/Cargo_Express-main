package com.example.guias.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.guias.model.Guia;
import com.example.guias.model.Ruta;
import com.example.guias.model.Region;
import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Usuario;
import com.example.guias.model.Rol;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.EmailService;
import com.example.guias.service.GuiaService;
import com.example.guias.service.MongoSyncService; // <-- MODIFICACIÓN 1: Import del servicio
import com.example.guias.service.PdfService;
import com.example.guias.service.PriceCalculatorService;
import com.example.guias.service.RutaService;
import com.example.guias.service.WekaService;
import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/guias")
public class GuiaController {

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PriceCalculatorService priceCalculatorService;

    @Autowired
    private WekaService wekaService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private com.example.guias.repository.DireccionRepository direccionRepository;

    @Autowired
    private com.example.guias.service.VehiculoService vehiculoService;

    // <-- MODIFICACIÓN 2: Inyección del servicio de sincronización
    @Autowired
    private MongoSyncService mongoSyncService;

    @GetMapping("")
    public String index(Authentication auth) {
        if (esAdmin(auth) || esRepartidor(auth)) {
            return "redirect:/guias/todas";
        }
        return "redirect:/guias/mis-guias";
    }

    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (esAdminORepartidor(auth)) {
            redirectAttributes.addFlashAttribute("error", "Los administradores y repartidores no pueden crear guías.");
            return "redirect:/menu";
        }
        model.addAttribute("guia", new Guia());
        model.addAttribute("tarifaPorKm", PriceCalculatorService.TARIFA_POR_KM_VIAL);
        usuarioRepository.findByUsername(auth.getName()).ifPresent(u -> {
            model.addAttribute("direccionesGuardadas", direccionRepository.findByUsuario(u));
        });
        return "crear-guia";
    }

    // ÚNICO MÉTODO POST PARA CREAR (Incluye la lógica de Weka y Correos)
    @PostMapping("/crear")
    public String guardarGuia(@ModelAttribute @Valid Guia guia, BindingResult result,
                              Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (esAdminORepartidor(auth)) {
            redirectAttributes.addFlashAttribute("error", "Los administradores y repartidores no pueden crear guías.");
            return "redirect:/menu";
        }
        if (result.hasErrors()) {
            model.addAttribute("tarifaPorKm", PriceCalculatorService.TARIFA_POR_KM_VIAL);
            usuarioRepository.findByUsername(auth.getName()).ifPresent(u ->
                    model.addAttribute("direccionesGuardadas", direccionRepository.findByUsuario(u)));
            return "crear-guia";
        }

        // 1. Validar y asignar el usuario creador
        String username = auth.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró el usuario autenticado.");
            return "redirect:/menu";
        }

        Usuario creador = usuarioOpt.get();
        guia.setUsuario(creador);
        sanitizarCamposRuta(guia);

        // 2. Calcular y asignar el precio
        double precioCalculado = priceCalculatorService.calcularPrecio(guia);
        guia.setPrecio(precioCalculado);

        // 3. Calcular predicción de tiempo con Weka
        double diasEstimados = wekaService.predecirTiempo(
            guia.getCiudadOrigen(), 
            guia.getCiudadDestino(), 
            guia.getTipoCarga()
        );
        System.out.println("⏳ Weka predice que el envío tardará: " + diasEstimados + " días");
        
        // Asignar el tiempo estimado predicho por Weka
        guia.setTiempoEstimadoDias(diasEstimados);

        // 4. Guardar la guía en MySQL
        guiaService.guardarGuia(guia);

        // <-- MODIFICACIÓN 3: Sincronizar inmediatamente con MongoDB al crear
        mongoSyncService.sincronizarGuias();

        // 5. Enviar Correo Electrónico Profesional (HTML)
        try {
            String emailUsuario = (creador.getEmail() != null && !creador.getEmail().isBlank()) 
                                ? creador.getEmail() : creador.getUsername();
            String htmlEmail = """
                <div style="background-color: #f4f7fa; padding: 40px 0; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
                        <tr>
                            <td align="center" style="background: linear-gradient(135deg, #002e7d 0%%, #001f5c 100%%); padding: 40px 20px;">
                                <img src="cid:logo" alt="SAI Cargo" width="120" style="display: block; margin-bottom: 15px; border-radius: 8px;">
                                <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px;">SAI CARGO EXPRESS</h1>
                                <div style="height: 3px; width: 60px; background-color: #ffcc00; margin: 15px auto;"></div>
                                <p style="color: #ffcc00; margin: 0; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px;">Logística Inteligente de Nivel Global</p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding: 40px 50px;">
                                <h2 style="color: #1e293b; font-size: 22px; margin-top: 0; font-weight: 800;">¡Confirmación de Despacho!</h2>
                                <p style="color: #64748b; font-size: 15px; line-height: 1.6;">Estimado(a) <strong>%s</strong>,</p>
                                <p style="color: #64748b; font-size: 15px; line-height: 1.6;">Hemos registrado exitosamente tu nuevo envío en nuestra red logística nacional. Tu paquete ya está siendo procesado por nuestro equipo de expertos.</p>
                                
                                <div style="text-align: center; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 25px; margin: 30px 0;">
                                    <span style="color: #94a3b8; font-size: 12px; font-weight: 700; text-transform: uppercase;">Número de Tracking</span>
                                    <div style="color: #002e7d; font-size: 32px; font-weight: 800; margin: 5px 0;">#%d</div>
                                    <div style="display: inline-block; background-color: #dcfce7; color: #166534; padding: 4px 12px; border-radius: 50px; font-size: 11px; font-weight: 700;">PROCESO INICIADO</div>
                                </div>

                                <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 30px;">
                                    <tr>
                                        <td style="padding: 12px 0; border-bottom: 1px solid #f1f5f9;">
                                            <span style="color: #94a3b8; font-size: 12px; font-weight: 600; text-transform: uppercase;">Ruta de Envío</span>
                                            <div style="color: #1e293b; font-size: 14px; font-weight: 700; margin-top: 4px;">%s (%s) &rarr; %s (%s)</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 12px 0; border-bottom: 1px solid #f1f5f9;">
                                            <span style="color: #94a3b8; font-size: 12px; font-weight: 600; text-transform: uppercase;">Tiempo Estimado de Entrega</span>
                                            <div style="color: #16a34a; font-size: 14px; font-weight: 700; margin-top: 4px;">%.1f Días Laborales</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 12px 0;">
                                            <span style="color: #94a3b8; font-size: 12px; font-weight: 600; text-transform: uppercase;">Inversión del Servicio</span>
                                            <div style="color: #1e293b; font-size: 14px; font-weight: 700; margin-top: 4px;">$%,.2f COP</div>
                                        </td>
                                    </tr>
                                </table>

                                <table width="100%%" cellpadding="0" cellspacing="0">
                                    <tr>
                                        <td align="center" style="padding-bottom: 20px;">
                                            <a href="http://localhost:8080/pago/%d" style="background-color: #10b981; color: #ffffff; padding: 18px 40px; text-decoration: none; border-radius: 12px; font-weight: 800; font-size: 16px; display: inline-block; box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4); transition: 0.3s; width: 250px;">PAGAR MI ENVÍO AHORA</a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center">
                                            <a href="http://localhost:8080/rastreo?id=%d" style="background-color: #ffcc00; color: #002e7d; padding: 15px 40px; text-decoration: none; border-radius: 12px; font-weight: 800; font-size: 14px; display: inline-block; box-shadow: 0 4px 15px rgba(255, 204, 0, 0.4); transition: 0.3s; width: 250px;">RASTREAR MI PAQUETE</a>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <td style="background-color: #f8fafc; padding: 30px; text-align: center;">
                                <p style="color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0 0 15px 0;">
                                    <strong>¿Necesitas soporte técnico?</strong><br>
                                    Nuestro equipo está disponible 24/7 para apoyarte en tu proceso logístico.
                                </p>
                                <div style="margin-bottom: 20px;">
                                    <a href="#" style="color: #002e7d; text-decoration: none; font-weight: 700; font-size: 12px; margin: 0 10px;">CENTRO DE AYUDA</a>
                                    <a href="#" style="color: #002e7d; text-decoration: none; font-weight: 700; font-size: 12px; margin: 0 10px;">POLÍTICAS</a>
                                    <a href="#" style="color: #002e7d; text-decoration: none; font-weight: 700; font-size: 12px; margin: 0 10px;">CONTACTO</a>
                                </div>
                                <p style="color: #cbd5e1; font-size: 11px; margin: 0;">
                                    &copy; 2026 SAI CARGO COLOMBIA S.A.S. Todos los derechos reservados.<br>
                                    Este es un mensaje transaccional automatizado.
                                </p>
                            </td>
                        </tr>
                    </table>
                </div>
                """.formatted(
                    creador.getNombre(),
                    guia.getId(),
                    guia.getCiudadOrigen(), guia.getDepartamentoOrigen(),
                    guia.getCiudadDestino(), guia.getDepartamentoDestino(),
                    diasEstimados,
                    guia.getPrecio(),
                    guia.getId(),
                    guia.getId()
                );

            emailService.enviarCorreoHtml(emailUsuario, "📦 Tu envío #" + guia.getId() + " ha sido procesado - SAI CARGO", htmlEmail, "static/imagenes/LOGO Cargo Expres.jpg");
            redirectAttributes.addFlashAttribute("mensaje", "📦 Guía creada correctamente. El tiempo estimado de entrega es de " + diasEstimados + " días.");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorCorreo", "Guía creada (Llegará en aprox. " + diasEstimados + " días), pero hubo un problema al enviar el correo pro.");
        }

        return "redirect:/pago/" + guia.getId();
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model,
                                          Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
        if (guiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La guía con ID " + id + " no existe.");
            return "redirect:/menu";
        }

        Guia guia = guiaOpt.get();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario autenticado no encontrado.");
            return "redirect:/menu";
        }

        Usuario user = usuarioOpt.get();
        if (!guia.getUsuario().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para editar esta guía. Solo el propietario puede hacerlo.");
            return "redirect:/menu";
        }

        model.addAttribute("guia", guia);
        return "editar-guia";
    }

    @PostMapping("/editar")
    public String editarGuia(@ModelAttribute @Valid Guia guia, BindingResult result,
                             Authentication auth, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "editar-guia";
        }

        Optional<Guia> existingGuiaOpt = guiaService.buscarPorId(guia.getId());
        if (existingGuiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La guía que intentas editar no existe.");
            return "redirect:/guias";
        }

        Guia existingGuia = existingGuiaOpt.get();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Error crítico de sesión.");
            return "redirect:/login";
        }

        Usuario user = usuarioOpt.get();

        // 1. Verificación de Seguridad Profunda
        if (!existingGuia.getUsuario().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Intento de acceso no autorizado detectado. Solo el dueño puede editar.");
            return "redirect:/guias";
        }

        // 2. Lógica de Protección de Estado "Pro"
        guia.setEstado(existingGuia.getEstado());

        // 3. Persistencia de Metadatos Inmutables
        guia.setUsuario(existingGuia.getUsuario());
        guia.setFechaCreacion(existingGuia.getFechaCreacion());
        guia.setModificadoPor(user.getNombre() + " (" + user.getRol() + ")");
        preservarDatosLogisticos(guia, existingGuia);
        sanitizarCamposRuta(guia);
        
        // 4. Recalcular precio con la lógica oficial
        guia.setPrecio(priceCalculatorService.calcularPrecio(guia));

        guiaService.guardarGuia(guia); // Guarda en MySQL

        // <-- MODIFICACIÓN 4: Sincronizar inmediatamente con MongoDB al editar
        mongoSyncService.sincronizarGuias();

        redirectAttributes.addFlashAttribute("mensaje", "📦 Operación Exitosa: Guía #" + guia.getId() + " actualizada correctamente.");
        
        return "redirect:/guias";
    }


    @GetMapping("/eliminar/{id}")
    public String eliminarGuia(@PathVariable Long id,
                               Authentication auth,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        String destinoRedirect = resolverDestinoTrasEliminar(request, auth);
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
        if (guiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La guía con ID " + id + " no existe.");
            return destinoRedirect;
        }

        Guia guia = guiaOpt.get();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario autenticado no encontrado.");
            return destinoRedirect;
        }

        Usuario user = usuarioOpt.get();

        // RESTRICCIÓN TOTAL PARA REPARTIDORES
        if (esRepartidor(auth)) {
            redirectAttributes.addFlashAttribute("error", "Los repartidores tienen prohibido eliminar registros del sistema.");
            return "redirect:/reparto";
        }

        // Permitir al Administrador o al cliente creador de la guía (ocultamiento visual, conserva BD)
        boolean esPropietario = guia.getUsuario() != null && guia.getUsuario().getId().equals(user.getId());
        if (!esAdmin(auth) && !esPropietario) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para retirar esta guía. Solo el propietario o un administrador pueden hacerlo.");
            return destinoRedirect;
        }

        // Borrado lógico (Soft Delete): se oculta en toda la interfaz, pero permanece intacta en la BD
        guia.setModificadoPor(auth.getName());
        guiaService.marcarComoEliminada(guia);

        if (mongoSyncService != null) {
            try {
                mongoSyncService.sincronizarGuias();
            } catch (Exception e) {
                System.err.println("⚠️ Error no crítico al sincronizar MongoDB tras ocultar guía #" + id + ": " + e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("mensaje",
                "La guía #" + id + " se quitó de la vista operativa (su registro sigue conservado en la base de datos).");
        return destinoRedirect;
    }

    private String resolverDestinoTrasEliminar(HttpServletRequest request, Authentication auth) {
        String referer = request != null ? request.getHeader("Referer") : null;
        if (referer != null) {
            if (referer.contains("/guias/todas")) return "redirect:/guias/todas";
            if (referer.contains("/guias/mis-guias")) return "redirect:/guias/mis-guias";
            if (referer.contains("/portal")) return "redirect:/portal";
        }
        return esAdmin(auth) ? "redirect:/guias/todas" : "redirect:/menu";
    }


    @GetMapping("/mis-guias")
    public String verMisGuias(Model model, Authentication auth) {
        model.addAttribute("esVistaGlobal", false);
        String username = auth.getName();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("guias", guiaService.obtenerPorUsuario(usuario));
        } else {
            model.addAttribute("error", "Usuario no encontrado.");
            model.addAttribute("guias", List.of());
        }
        return "lista-guias";
    }

    @GetMapping("/todas")
    public String verTodasGuias(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (esAdminORepartidor(auth)) {
            model.addAttribute("esVistaGlobal", true);
            model.addAttribute("guias", guiaService.obtenerTodas());
            if (esAdmin(auth)) {
                // Solo listamos repartidores con vehículo asignado y mostramos su carga en el DTO
                List<com.example.guias.dto.RepartidorVehiculoDTO> repartidores = vehiculoService.obtenerRepartidoresConVehiculoDTO();
                model.addAttribute("repartidores", repartidores);
            }
            return "lista-guias";
        }
        redirectAttributes.addFlashAttribute("error", "No tienes permiso para ver todas las guías.");
        return "redirect:/menu";
    }

    /**
     * Asigna el repartidor de una fase:
     *  - RECOGIDA: la guía debe estar Pendiente (o No Recogido, que se reabre) y el repartidor debe operar
     *    en la región del departamento de origen.
     *  - ENTREGA: solo cuando la recogida terminó (guía En Sucursal) y el repartidor opera en la región
     *    del departamento de destino.
     */
    @PostMapping("/asignar-repartidor")
    public String asignarRepartidor(@RequestParam Long guiaId,
                                    @RequestParam(required = false) Long repartidorId,
                                    @RequestParam(required = false) String fase,
                                    RedirectAttributes redirectAttributes,
                                    Authentication auth) {
        if (!esAdmin(auth)) {
            redirectAttributes.addFlashAttribute("error", "Solo los administradores pueden asignar guías.");
            return "redirect:/menu";
        }
        Optional<Guia> guiaOpt = guiaService.buscarPorId(guiaId);
        if (guiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Guía no encontrada.");
            return "redirect:/guias/todas";
        }
        Guia guia = guiaOpt.get();

        boolean recogida;
        if (fase == null || fase.isBlank()) {
            recogida = guia.getEstado() == null
                    || EstadoGuia.esUnoDe(guia.getEstado(), EstadoGuia.PENDIENTE, EstadoGuia.NO_RECOGIDO);
        } else {
            recogida = RutaService.TIPO_RECOGIDA.equals(RutaService.normalizarTipo(fase));
        }
        String nombreFase = recogida ? "recogida" : "entrega";

        // 1) Reglas de estado: recogida primero, entrega después
        if (recogida) {
            if (guia.getEstado() != null
                    && !EstadoGuia.esUnoDe(guia.getEstado(), EstadoGuia.PENDIENTE, EstadoGuia.NO_RECOGIDO)) {
                redirectAttributes.addFlashAttribute("error", "⚠️ La recogida solo se asigna a guías Pendientes o No Recogidas (estado actual: "
                        + guia.getEstado() + ").");
                return "redirect:/guias/todas";
            }
        } else if (!EstadoGuia.es(guia.getEstado(), EstadoGuia.EN_SUCURSAL)) {
            redirectAttributes.addFlashAttribute("error", "⚠️ La entrega solo puede asignarse cuando la recogida terminó y la guía está En Sucursal (estado actual: "
                    + guia.getEstado() + ").");
            return "redirect:/guias/todas";
        }

        // 2) No se reasigna una guía que ya está en una ruta activa de esa fase
        Ruta rutaFase = recogida ? guia.getRutaRecogida() : guia.getRuta();
        if (rutaFase != null
                && (RutaService.ASIGNADA.equalsIgnoreCase(rutaFase.getEstado())
                || RutaService.EN_CURSO.equalsIgnoreCase(rutaFase.getEstado()))) {
            redirectAttributes.addFlashAttribute("error", "⚠️ La guía #" + guiaId
                    + " ya forma parte de la ruta #" + rutaFase.getId()
                    + " (" + rutaFase.getEstado() + "). Cancele primero la ruta en el Planificador de Rutas.");
            return "redirect:/guias/todas";
        }

        if (repartidorId != null && repartidorId > 0) {
            Optional<Usuario> repOpt = usuarioRepository.findById(repartidorId);
            if (repOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Repartidor no encontrado.");
                return "redirect:/guias/todas";
            }
            Usuario rep = repOpt.get();

            if (rep.getRol() != Rol.ROLE_REPARTIDOR || !rep.isAprobado()) {
                redirectAttributes.addFlashAttribute("error", "El usuario seleccionado no es un repartidor aprobado.");
                return "redirect:/guias/todas";
            }

            // 3) Límite por región de operación
            if (rep.getRegionOperacion() == null) {
                redirectAttributes.addFlashAttribute("error", "⚠️ El repartidor " + rep.getNombre()
                        + " no tiene región de operación. Asígnela en Administración de Usuarios.");
                return "redirect:/guias/todas";
            }
            String deptoGuia = recogida ? guia.getDepartamentoOrigen() : guia.getDepartamentoDestino();
            Region regionGuia = Region.deDepartamento(deptoGuia);
            if (regionGuia == null) {
                redirectAttributes.addFlashAttribute("error", "⚠️ No se pudo determinar la región del departamento de "
                        + (recogida ? "origen" : "destino") + " de la guía #" + guiaId + " (" + deptoGuia + ").");
                return "redirect:/guias/todas";
            }
            if (regionGuia != rep.getRegionOperacion()) {
                redirectAttributes.addFlashAttribute("error", "⚠️ La " + nombreFase + " de la guía #" + guiaId
                        + " es en la " + regionGuia.getEtiqueta() + " y " + rep.getNombre() + " opera en la "
                        + rep.getRegionOperacion().getEtiqueta() + ".");
                return "redirect:/guias/todas";
            }

            // 4) Vehículo asignado + capacidad de peso + capacidad de volumen
            String errorCapacidad = vehiculoService.validarAsignacion(rep, guia);
            if (errorCapacidad != null) {
                redirectAttributes.addFlashAttribute("error", errorCapacidad);
                return "redirect:/guias/todas";
            }

            if (recogida) {
                if (EstadoGuia.es(guia.getEstado(), EstadoGuia.NO_RECOGIDO)) {
                    guia.setEstado(EstadoGuia.PENDIENTE); // se reabre la recogida
                }
                guia.setRutaRecogida(null);
                guia.setOrdenRecogida(null);
                guia.setRepartidorRecogida(rep);
            } else {
                guia.setRuta(null); // una ruta de entrega anterior ya terminada no se arrastra
                guia.setOrdenEntrega(null);
                guia.setRepartidorEntrega(rep);
            }
            guia.setRepartidorAsignado(rep);
            String placa = vehiculoService.buscarPorRepartidor(rep).map(com.example.guias.model.Vehiculo::getPlaca).orElse("-");
            redirectAttributes.addFlashAttribute("mensaje", "✅ " + rep.getNombre() + " asignado a la " + nombreFase
                    + " de la guía #" + guiaId + " con la furgoneta [" + placa + "]");
        } else {
            if (recogida) {
                guia.setRepartidorRecogida(null);
            } else {
                guia.setRepartidorEntrega(null);
            }
            guia.setRepartidorAsignado(null);
            redirectAttributes.addFlashAttribute("mensaje", "Asignación de " + nombreFase + " removida para la guía #" + guiaId);
        }
        guiaService.guardarGuia(guia);
        mongoSyncService.sincronizarGuias();
        return "redirect:/guias/todas";
    }


    @GetMapping("/buscar")
    public String buscarGuia(@RequestParam("id") Long id, Model model, Authentication auth) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);

        if (guiaOpt.isEmpty()) {
            model.addAttribute("error", "No se encontró ninguna guía con el ID " + id);
            model.addAttribute("guias", List.of());
            return "lista-guias";
        }

        Guia guia = guiaOpt.get();
        if (EstadoGuia.es(guia.getEstado(), EstadoGuia.ELIMINADA)) {
            model.addAttribute("error", "No se encontró ninguna guía activa con el ID " + id);
            model.addAttribute("guias", List.of());
            return "lista-guias";
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "Usuario autenticado no encontrado.");
            model.addAttribute("guias", List.of());
            return "lista-guias";
        }

        Usuario user = usuarioOpt.get();
        if (!guia.getUsuario().getId().equals(user.getId()) && !esAdminORepartidor(auth)) {
            model.addAttribute("error", "No tienes permiso para ver esta guía.");
            model.addAttribute("guias", List.of());
            return "lista-guias";
        }

        model.addAttribute("guias", List.of(guia));
        model.addAttribute("mensaje", "Mostrando resultados para la guía ID: " + id);
        return "lista-guias";
    }

    @GetMapping("/etiqueta/{id}")
    public ResponseEntity<byte[]> descargarEtiqueta(@PathVariable Long id, Authentication auth) {
        Optional<Guia> guiaOpt = guiaService.buscarPorId(id);
        if (guiaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Guia guia = guiaOpt.get();
        if (EstadoGuia.es(guia.getEstado(), EstadoGuia.ELIMINADA)) {
            return ResponseEntity.notFound().build();
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        
        if (usuarioOpt.isEmpty()) return ResponseEntity.status(401).build();
        Usuario user = usuarioOpt.get();

        if (!guia.getUsuario().getId().equals(user.getId()) && !esAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }

        try {
            byte[] pdfContent = pdfService.generarEtiqueta(guia);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Etiqueta_SAI_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean esAdminORepartidor(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_REPARTIDOR"));
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    private boolean esRepartidor(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_REPARTIDOR"));
    }

    private void sanitizarCamposRuta(Guia g) {
        if (!esCoordenadaValida(g.getLatOrigen(), g.getLonOrigen())) {
            g.setLatOrigen(null);
            g.setLonOrigen(null);
        }
        if (!esCoordenadaValida(g.getLatDestino(), g.getLonDestino())) {
            g.setLatDestino(null);
            g.setLonDestino(null);
        }
        if (g.getDistanciaKm() == null || !Double.isFinite(g.getDistanciaKm())
                || g.getDistanciaKm() <= 0 || g.getDistanciaKm() > 10000) {
            g.setDistanciaKm(null);
        } else {
            g.setDistanciaKm(Math.round(g.getDistanciaKm() * 100.0) / 100.0);
        }
        if (g.getTiempoEstimadoMinutos() == null || g.getTiempoEstimadoMinutos() <= 0
                || g.getTiempoEstimadoMinutos() > 20000) {
            g.setTiempoEstimadoMinutos(null);
        }
    }

    private boolean esCoordenadaValida(Double lat, Double lon) {
        if (lat == null || lon == null || !Double.isFinite(lat) || !Double.isFinite(lon)) return false;
        if (Math.abs(lat) < 0.0001 && Math.abs(lon) < 0.0001) return false;
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }

    private void preservarDatosLogisticos(Guia destino, Guia origen) {
        if (destino.getRepartidorAsignado() == null) destino.setRepartidorAsignado(origen.getRepartidorAsignado());
        if (destino.getRepartidorRecogida() == null) destino.setRepartidorRecogida(origen.getRepartidorRecogida());
        if (destino.getRepartidorEntrega() == null) destino.setRepartidorEntrega(origen.getRepartidorEntrega());
        if (destino.getRuta() == null) destino.setRuta(origen.getRuta());
        if (destino.getOrdenEntrega() == null) destino.setOrdenEntrega(origen.getOrdenEntrega());
        if (destino.getRutaRecogida() == null) destino.setRutaRecogida(origen.getRutaRecogida());
        if (destino.getOrdenRecogida() == null) destino.setOrdenRecogida(origen.getOrdenRecogida());
        if (destino.getMotivoNovedad() == null) destino.setMotivoNovedad(origen.getMotivoNovedad());
        if (destino.getLatOrigen() == null) {
            destino.setLatOrigen(origen.getLatOrigen());
            destino.setLonOrigen(origen.getLonOrigen());
        }
        if (destino.getLatDestino() == null) {
            destino.setLatDestino(origen.getLatDestino());
            destino.setLonDestino(origen.getLonDestino());
        }
        if (destino.getDistanciaKm() == null) destino.setDistanciaKm(origen.getDistanciaKm());
        if (destino.getTiempoEstimadoMinutos() == null) destino.setTiempoEstimadoMinutos(origen.getTiempoEstimadoMinutos());
    }
}