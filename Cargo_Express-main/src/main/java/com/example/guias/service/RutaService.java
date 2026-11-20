package com.example.guias.service;

import com.example.guias.dto.RutaPlanDTO;
import com.example.guias.dto.RutaResumenDTO;
import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Guia;
import com.example.guias.model.Region;
import com.example.guias.model.Rol;
import com.example.guias.model.Ruta;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.GuiaRepository;
import com.example.guias.repository.RutaRepository;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.util.DistanciaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RutaService {

    public static final String ASIGNADA = "ASIGNADA";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String COMPLETADA = "COMPLETADA";

    public static final String TIPO_RECOGIDA = "RECOGIDA";
    public static final String TIPO_ENTREGA = "ENTREGA";

    public static String normalizarTipo(String tipo) {
        return TIPO_RECOGIDA.equalsIgnoreCase(tipo == null ? "" : tipo.trim()) ? TIPO_RECOGIDA : TIPO_ENTREGA;
    }

    @Autowired
    private RutaRepository rutaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GuiaRepository guiaRepository;

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private VehiculoService vehiculoService;

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public RutaPlanDTO obtenerDatosRepartidorParaPlanificar(Long repartidorId) {
        return obtenerDatosRepartidorParaPlanificar(repartidorId, TIPO_ENTREGA);
    }

    @Transactional(readOnly = true)
    public RutaPlanDTO obtenerDatosRepartidorParaPlanificar(Long repartidorId, String tipoSolicitado) {
        final String tipo = normalizarTipo(tipoSolicitado);
        if (repartidorId == null) {
            throw new IllegalArgumentException("Debe seleccionar un repartidor.");
        }
        Usuario rep = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new IllegalArgumentException("Repartidor no encontrado."));
        if (rep.getRol() != Rol.ROLE_REPARTIDOR || !rep.isAprobado()) {
            throw new IllegalArgumentException("El usuario seleccionado no es un repartidor aprobado.");
        }

        Vehiculo vehiculo = vehiculoService.buscarPorRepartidor(rep)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El repartidor " + rep.getNombre() + " no tiene una furgoneta asignada."));

        RutaPlanDTO dto = new RutaPlanDTO();
        dto.setRepartidorId(rep.getId());
        dto.setRepartidorNombre(rep.getNombre());
        dto.setVehiculoPlaca(vehiculo.getPlaca());
        dto.setVehiculoModelo(vehiculo.getModelo());
        dto.setCapacidadPesoKg(vehiculo.getCapacidadPesoKg());
        dto.setTipo(tipo);
        if (rep.getRegionOperacion() != null) {
            dto.setRegionOperacion(rep.getRegionOperacion().getEtiqueta());
            dto.setCapitalesRegion(rep.getRegionOperacion().getCapitales());
        }

        double pesoTotal = guiaService.obtenerPorRepartidorAsignado(rep).stream()
                .mapToDouble(Guia::getPesoKg)
                .sum();
        dto.setPesoCargadoKg(Math.round(pesoTotal * 10.0) / 10.0);

        Optional<Ruta> rutaActiva = rutaRepository.findTopByRepartidorAndEstadoInOrderByFechaDesc(
                rep, List.of(ASIGNADA, EN_CURSO));
        rutaActiva.ifPresent(r -> {
            dto.setTieneRutaActiva(true);
            dto.setRutaActivaId(r.getId());
            dto.setEstadoRutaActiva(r.getEstado());
        });

        List<Guia> pendientes = guiasPlanificables(rep, tipo);
        List<RutaPlanDTO.ParadaDTO> paradas = pendientes.stream()
                .map(g -> toParadaDTO(g, tipo))
                .collect(Collectors.toList());
        dto.setParadas(paradas);

        String capitalSugerida = sugerirCapitalHub(pendientes, tipo);
        if (rep.getRegionOperacion() != null && !rep.getRegionOperacion().contieneCapital(capitalSugerida)) {
            capitalSugerida = rep.getRegionOperacion().getCapitales().get(0);
        }
        dto.setCapitalSugerida(capitalSugerida);
        double[] hubCoords = coordsHubODefault(capitalSugerida);
        dto.setHubLat(hubCoords[0]);
        dto.setHubLon(hubCoords[1]);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<RutaResumenDTO> obtenerTodasLasRutas() {
        return rutaRepository.findAllByOrderByFechaDesc().stream()
                .map(r -> toResumenDTO(r, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RutaResumenDTO obtenerDetalleRuta(Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new IllegalArgumentException("Ruta #" + rutaId + " no encontrada."));
        return toResumenDTO(ruta, true);
    }

    // ------------------------------------------------------------------ escritura

    @Transactional
    public Ruta crearYAsignarRuta(Long repartidorId,
                                  String capitalHub,
                                  List<Long> guiaIdsOrdenadas,
                                  Map<Long, double[]> coordsActualizadas,
                                  Double distanciaTotalKm,
                                  Integer tiempoEstimadoMinutos,
                                  String geometriaJson) {
        return crearYAsignarRuta(repartidorId, capitalHub, guiaIdsOrdenadas, coordsActualizadas,
                distanciaTotalKm, tiempoEstimadoMinutos, geometriaJson, TIPO_ENTREGA);
    }

    @Transactional
    public Ruta crearYAsignarRuta(Long repartidorId,
                                  String capitalHub,
                                  List<Long> guiaIdsOrdenadas,
                                  Map<Long, double[]> coordsActualizadas,
                                  Double distanciaTotalKm,
                                  Integer tiempoEstimadoMinutos,
                                  String geometriaJson,
                                  String tipoSolicitado) {
        final String tipo = normalizarTipo(tipoSolicitado);
        final boolean recogida = TIPO_RECOGIDA.equals(tipo);
        if (repartidorId == null) {
            throw new IllegalArgumentException("Debe seleccionar un repartidor.");
        }
        Usuario rep = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new IllegalArgumentException("Repartidor no encontrado."));
        if (rep.getRol() != Rol.ROLE_REPARTIDOR || !rep.isAprobado()) {
            throw new IllegalArgumentException("El usuario seleccionado no es un repartidor aprobado.");
        }

        Vehiculo vehiculo = vehiculoService.buscarPorRepartidor(rep)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El repartidor " + rep.getNombre() + " debe tener una furgoneta asignada."));

        if (rutaRepository.existsByRepartidorAndEstadoIn(rep, List.of(ASIGNADA, EN_CURSO))) {
            throw new IllegalArgumentException("El repartidor " + rep.getNombre()
                    + " ya tiene una ruta asignada o en curso. Cancele o finalice la ruta actual primero.");
        }

        if (guiaIdsOrdenadas == null || guiaIdsOrdenadas.isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos una guía como parada de la ruta.");
        }

        String hubLimpio = capitalHub == null ? "" : capitalHub.trim().toUpperCase();
        if (hubLimpio.isEmpty() || DistanciaUtil.obtenerCoordenadasCiudad(hubLimpio) == null) {
            throw new IllegalArgumentException("Seleccione una capital hub regional válida.");
        }

        if (rep.getRegionOperacion() != null && !rep.getRegionOperacion().contieneCapital(hubLimpio)) {
            throw new IllegalArgumentException("El hub " + hubLimpio + " no pertenece a la "
                    + rep.getRegionOperacion().getEtiqueta() + " del repartidor " + rep.getNombre() + ".");
        }

        if (distanciaTotalKm == null || distanciaTotalKm < 0) {
            throw new IllegalArgumentException("La distancia total estimada no es válida.");
        }

        Set<Long> vistos = new HashSet<>();
        List<Guia> guiasOrdenadas = new ArrayList<>();
        for (Long guiaId : guiaIdsOrdenadas) {
            if (guiaId == null || !vistos.add(guiaId)) {
                throw new IllegalArgumentException("La secuencia de paradas contiene guías inválidas o duplicadas.");
            }
            Guia guia = guiaRepository.findById(guiaId)
                    .orElseThrow(() -> new IllegalArgumentException("Guía #" + guiaId + " no encontrada."));

            if (guia.getRepartidorAsignado() == null
                    || !guia.getRepartidorAsignado().getId().equals(rep.getId())) {
                throw new IllegalArgumentException("La guía #" + guiaId
                        + " no está asignada al repartidor " + rep.getNombre() + ".");
            }
            boolean estadoValido = recogida
                    ? (guia.getEstado() == null || EstadoGuia.es(guia.getEstado(), EstadoGuia.PENDIENTE))
                    : EstadoGuia.es(guia.getEstado(), EstadoGuia.EN_SUCURSAL);
            if (!estadoValido) {
                throw new IllegalArgumentException("La guía #" + guiaId + " tiene estado '"
                        + guia.getEstado() + "' y no puede incluirse en una ruta de "
                        + (recogida ? "recogida (debe estar Pendiente)." : "entrega (debe estar En Sucursal)."));
            }
            Ruta yaEnRuta = recogida ? guia.getRutaRecogida() : guia.getRuta();
            if (yaEnRuta != null) {
                throw new IllegalArgumentException("La guía #" + guiaId
                        + " ya pertenece a la ruta #" + yaEnRuta.getId() + ".");
            }
            if (rep.getRegionOperacion() != null) {
                Region regionParada = Region.deDepartamento(
                        recogida ? guia.getDepartamentoOrigen() : guia.getDepartamentoDestino());
                if (regionParada != null && regionParada != rep.getRegionOperacion()) {
                    throw new IllegalArgumentException("La guía #" + guiaId + " pertenece a la "
                            + regionParada.getEtiqueta() + " y el repartidor opera en la "
                            + rep.getRegionOperacion().getEtiqueta() + ".");
                }
            }

            if (coordsActualizadas != null && coordsActualizadas.containsKey(guiaId)) {
                double[] latLon = coordsActualizadas.get(guiaId);
                if (coordenadasValidas(latLon)) {
                    if (recogida) {
                        guia.setLatOrigen(latLon[0]);
                        guia.setLonOrigen(latLon[1]);
                    } else {
                        guia.setLatDestino(latLon[0]);
                        guia.setLonDestino(latLon[1]);
                    }
                }
            }
            guiasOrdenadas.add(guia);
        }

        Ruta ruta = new Ruta();
        ruta.setRepartidor(rep);
        ruta.setVehiculo(vehiculo);
        ruta.setTipo(tipo);
        ruta.setEstado(ASIGNADA);
        ruta.setCapitalHub(hubLimpio);
        ruta.setDistanciaTotalKm(Math.round(distanciaTotalKm * 100.0) / 100.0);
        ruta.setTiempoEstimadoMinutos(tiempoEstimadoMinutos != null ? Math.max(1, tiempoEstimadoMinutos) : null);
        ruta.setGeometriaJson(geometriaJson);

        for (Guia guia : guiasOrdenadas) {
            ruta.agregarParada(guia);
        }

        Ruta guardada = rutaRepository.save(ruta);
        guiaRepository.saveAll(guiasOrdenadas);
        return guardada;
    }

    @Transactional
    public void cancelarRuta(Long rutaId) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new IllegalArgumentException("Ruta #" + rutaId + " no encontrada."));

        if (!ASIGNADA.equalsIgnoreCase(ruta.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden cancelar rutas en estado ASIGNADA (estado actual: "
                    + ruta.getEstado() + ").");
        }

        List<Guia> guiasDesvinculadas = new ArrayList<>(ruta.getParadas());
        for (Guia guia : guiasDesvinculadas) {
            if (ruta.esRecogida()) {
                guia.setRutaRecogida(null);
                guia.setOrdenRecogida(null);
            } else {
                guia.setRuta(null);
                guia.setOrdenEntrega(null);
            }
        }
        ruta.getParadas().clear();
        guiaRepository.saveAll(guiasDesvinculadas);
        rutaRepository.delete(ruta);
    }

    // ------------------------------------------------------------------ ciclo repartidor

    @Transactional(readOnly = true)
    public Optional<RutaResumenDTO> obtenerRutaActivaPorRepartidor(Usuario repartidor) {
        if (repartidor == null) return Optional.empty();
        return rutaRepository.findTopByRepartidorAndEstadoInOrderByFechaDesc(
                        repartidor, List.of(ASIGNADA, EN_CURSO))
                .map(r -> toResumenDTO(r, true));
    }

    @Transactional
    public Optional<RutaResumenDTO> iniciarRutaRepartidor(Usuario repartidor, String modificadoPor) {
        if (repartidor == null) return Optional.empty();
        Optional<Ruta> rutaOpt = rutaRepository.findTopByRepartidorAndEstadoInOrderByFechaDesc(
                repartidor, List.of(ASIGNADA, EN_CURSO));
        if (rutaOpt.isEmpty()) {
            return Optional.empty();
        }

        Ruta ruta = rutaOpt.get();
        if (ASIGNADA.equalsIgnoreCase(ruta.getEstado())) {
            ruta.setEstado(EN_CURSO);
            rutaRepository.save(ruta);
        }

        List<Guia> actualizadas = new ArrayList<>();
        // La ruta de recogida no cambia estados al iniciar: cada parada se marca Recogido / No Recogido.
        for (Guia g : ruta.esRecogida() ? List.<Guia>of() : ruta.getGuias()) {
            String estado = g.getEstado();
            if (estado == null || EstadoGuia.esUnoDe(estado, EstadoGuia.PENDIENTE,
                    EstadoGuia.EN_SUCURSAL, EstadoGuia.RECOGIDO)) {
                g.setEstado(EstadoGuia.EN_TRANSITO);
                if (modificadoPor != null) {
                    g.setModificadoPor(modificadoPor);
                }
                actualizadas.add(g);
            }
        }
        if (!actualizadas.isEmpty()) {
            guiaRepository.saveAll(actualizadas);
        }
        return Optional.of(toResumenDTO(ruta, true));
    }

    @Transactional
    public void finalizarRutaRepartidor(Usuario repartidor, boolean forzar) {
        if (repartidor == null) {
            throw new IllegalArgumentException("Usuario repartidor no válido.");
        }
        Ruta ruta = rutaRepository.findTopByRepartidorAndEstadoInOrderByFechaDesc(
                        repartidor, List.of(EN_CURSO, ASIGNADA))
                .orElseThrow(() -> new IllegalArgumentException("No tienes una ruta activa para finalizar."));

        long pendientes = ruta.getParadas().stream()
                .filter(g -> !paradaResuelta(ruta, g))
                .count();

        if (!forzar && pendientes > 0) {
            throw new IllegalArgumentException("Aún quedan " + pendientes
                    + (ruta.esRecogida()
                    ? " parada(s) sin marcar como recogida ni reportar como no recogida."
                    : " parada(s) pendientes sin entregar ni reportar novedad."));
        }

        completarRuta(ruta);
    }

    /** Una parada está resuelta cuando ya no requiere acción del repartidor en esa ruta. */
    private boolean paradaResuelta(Ruta ruta, Guia g) {
        if (EstadoGuia.esUnoDe(g.getEstado(), EstadoGuia.CANCELADO, EstadoGuia.ELIMINADA)) {
            return true;
        }
        if (ruta.esRecogida()) {
            return EstadoGuia.esUnoDe(g.getEstado(), EstadoGuia.RECOGIDO, EstadoGuia.NO_RECOGIDO,
                    EstadoGuia.EN_SUCURSAL);
        }
        return EstadoGuia.esUnoDe(g.getEstado(), EstadoGuia.ENTREGADO, EstadoGuia.NOVEDAD);
    }

    /**
     * Cierra la ruta y aplica el efecto sobre las guías:
     * - Recogida: Recogido -> En Sucursal; las no recogidas vuelven al administrador. En ambos casos
     *   el repartidor de la fase deja de tener la guía asignada.
     * - Entrega: las guías con Novedad o sin entregar regresan a la sucursal para un nuevo intento.
     */
    private void completarRuta(Ruta ruta) {
        List<Guia> modificadas = new ArrayList<>();
        for (Guia g : ruta.getParadas()) {
            String estado = g.getEstado();
            if (ruta.esRecogida()) {
                if (EstadoGuia.es(estado, EstadoGuia.RECOGIDO)) {
                    g.setEstado(EstadoGuia.EN_SUCURSAL);
                } else if (estado == null || EstadoGuia.es(estado, EstadoGuia.PENDIENTE)) {
                    g.setEstado(EstadoGuia.NO_RECOGIDO);
                    if (g.getMotivoNovedad() == null || g.getMotivoNovedad().isBlank()) {
                        g.setMotivoNovedad("Ruta de recogida finalizada sin recoger este paquete.");
                    }
                } else if (!EstadoGuia.esUnoDe(estado, EstadoGuia.NO_RECOGIDO, EstadoGuia.EN_SUCURSAL)) {
                    continue;
                }
                g.setRepartidorAsignado(null);
                modificadas.add(g);
            } else if (EstadoGuia.esUnoDe(estado, EstadoGuia.NOVEDAD, EstadoGuia.EN_TRANSITO)) {
                g.setEstado(EstadoGuia.EN_SUCURSAL);
                g.setRepartidorAsignado(null);
                modificadas.add(g);
            }
        }
        if (!modificadas.isEmpty()) {
            guiaRepository.saveAll(modificadas);
        }
        ruta.setEstado(COMPLETADA);
        rutaRepository.save(ruta);
    }

    @Transactional
    public void verificarCompletarRutaPorGuia(Guia guia) {
        if (guia == null) return;
        Guia gestionada = guia.getId() != null
                ? guiaRepository.findById(guia.getId()).orElse(guia)
                : guia;

        // Ruta de recogida: se completa cuando todas las paradas están resueltas (recogidas o no recogidas)
        if (gestionada.getRutaRecogida() != null && gestionada.getRutaRecogida().getId() != null) {
            Ruta ruta = rutaRepository.findById(gestionada.getRutaRecogida().getId()).orElse(null);
            if (ruta != null && rutaActiva(ruta) && !ruta.getParadas().isEmpty()
                    && ruta.getParadas().stream().allMatch(g -> paradaResuelta(ruta, g))) {
                completarRuta(ruta);
            }
        }

        // Ruta de entrega: se completa cuando todas las paradas están entregadas
        if (gestionada.getRuta() != null && gestionada.getRuta().getId() != null) {
            Ruta ruta = rutaRepository.findById(gestionada.getRuta().getId()).orElse(null);
            if (ruta != null && rutaActiva(ruta)) {
                boolean todasEntregadas = !ruta.getGuias().isEmpty() && ruta.getGuias().stream()
                        .allMatch(g -> EstadoGuia.es(g.getEstado(), EstadoGuia.ENTREGADO));
                if (todasEntregadas) {
                    completarRuta(ruta);
                }
            }
        }
    }

    private boolean rutaActiva(Ruta ruta) {
        return ASIGNADA.equalsIgnoreCase(ruta.getEstado()) || EN_CURSO.equalsIgnoreCase(ruta.getEstado());
    }

    // ------------------------------------------------------------------ helpers internos

    /** Guías del repartidor que todavía pueden agruparse en una ruta del tipo indicado. */
    private List<Guia> guiasPlanificables(Usuario rep, String tipo) {
        boolean recogida = TIPO_RECOGIDA.equals(tipo);
        return guiaService.obtenerPorRepartidorAsignado(rep).stream()
                .filter(g -> recogida
                        ? ((g.getEstado() == null || EstadoGuia.es(g.getEstado(), EstadoGuia.PENDIENTE))
                                && g.getRutaRecogida() == null)
                        : (EstadoGuia.es(g.getEstado(), EstadoGuia.EN_SUCURSAL) && g.getRuta() == null))
                .collect(Collectors.toList());
    }

    private String sugerirCapitalHub(List<Guia> guias, String tipo) {
        if (guias == null || guias.isEmpty()) return "BOGOTÁ";
        Map<String, Integer> conteo = new HashMap<>();
        for (Guia g : guias) {
            String capital = DistanciaUtil.obtenerCapitalPorDepartamento(
                    TIPO_RECOGIDA.equals(tipo) ? g.getDepartamentoOrigen() : g.getDepartamentoDestino());
            conteo.merge(capital, 1, Integer::sum);
        }
        return conteo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("BOGOTÁ");
    }

    private double[] coordsHubODefault(String capital) {
        double[] coords = DistanciaUtil.obtenerCoordenadasCiudad(capital);
        return coords != null ? coords : new double[]{4.6097, -74.0817};
    }

    /**
     * Parada genérica. En rutas de recogida los campos "destino" del DTO se rellenan con los datos del
     * origen (remitente, dirección de envío, ciudad/departamento y coordenadas de origen), de modo que
     * el planificador y los mapas funcionan igual para ambos tipos de ruta.
     */
    private RutaPlanDTO.ParadaDTO toParadaDTO(Guia g, String tipo) {
        boolean recogida = TIPO_RECOGIDA.equals(tipo);
        RutaPlanDTO.ParadaDTO p = new RutaPlanDTO.ParadaDTO();
        p.setGuiaId(g.getId());
        p.setOrdenEntrega(recogida ? g.getOrdenRecogida() : g.getOrdenEntrega());
        p.setDestinatario(recogida ? g.getRemitente() : g.getDestinatario());
        p.setDireccionRecibo(recogida ? g.getDireccionEnvio() : g.getDireccionRecibo());
        String ciudad = recogida ? g.getCiudadOrigen() : g.getCiudadDestino();
        String depto = recogida ? g.getDepartamentoOrigen() : g.getDepartamentoDestino();
        p.setCiudadDestino(ciudad);
        p.setDepartamentoDestino(depto);
        p.setPesoKg(g.getPesoKg());
        p.setEstado(g.getEstado());
        p.setLatDestino(recogida ? g.getLatOrigen() : g.getLatDestino());
        p.setLonDestino(recogida ? g.getLonOrigen() : g.getLonDestino());

        double[] ciudadCoords = DistanciaUtil.obtenerCoordenadasCiudad(ciudad);
        if (ciudadCoords == null) {
            ciudadCoords = coordsHubODefault(DistanciaUtil.obtenerCapitalPorDepartamento(depto));
        }
        p.setCiudadLat(ciudadCoords[0]);
        p.setCiudadLon(ciudadCoords[1]);
        return p;
    }

    private RutaResumenDTO toResumenDTO(Ruta r, boolean incluirGeometria) {
        RutaResumenDTO dto = new RutaResumenDTO();
        dto.setId(r.getId());
        dto.setFecha(r.getFecha());
        dto.setEstado(r.getEstado());
        dto.setTipo(r.getTipo());
        dto.setCapitalHub(r.getCapitalHub());
        double[] hubCoords = coordsHubODefault(r.getCapitalHub());
        dto.setHubLat(hubCoords[0]);
        dto.setHubLon(hubCoords[1]);
        dto.setDistanciaTotalKm(r.getDistanciaTotalKm());
        dto.setTiempoEstimadoMinutos(r.getTiempoEstimadoMinutos());
        if (r.getRepartidor() != null) {
            dto.setRepartidorId(r.getRepartidor().getId());
            dto.setRepartidorNombre(r.getRepartidor().getNombre());
        }
        if (r.getVehiculo() != null) {
            dto.setVehiculoPlaca(r.getVehiculo().getPlaca());
            dto.setVehiculoModelo(r.getVehiculo().getModelo());
        }

        final String tipoRuta = r.getTipo();
        Comparator<Guia> porOrden = r.esRecogida()
                ? Comparator.comparing(Guia::getOrdenRecogida, Comparator.nullsLast(Integer::compareTo))
                : Comparator.comparing(Guia::getOrdenEntrega, Comparator.nullsLast(Integer::compareTo));
        List<RutaPlanDTO.ParadaDTO> paradas = r.getParadas().stream()
                .filter(g -> !EstadoGuia.es(g.getEstado(), EstadoGuia.ELIMINADA))
                .sorted(porOrden.thenComparing(Guia::getId))
                .map(g -> toParadaDTO(g, tipoRuta))
                .collect(Collectors.toList());

        dto.setParadas(paradas);
        dto.setCantidadParadas(paradas.size());
        dto.setPesoTotalKg(Math.round(paradas.stream()
                .mapToDouble(p -> p.getPesoKg() != null ? p.getPesoKg() : 0.0)
                .sum() * 10.0) / 10.0);

        if (incluirGeometria) {
            dto.setGeometriaJson(r.getGeometriaJson());
        }
        return dto;
    }

    private boolean coordenadasValidas(double[] latLon) {
        if (latLon == null || latLon.length != 2) return false;
        double lat = latLon[0];
        double lon = latLon[1];
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) return false;
        if (Math.abs(lat) < 0.0001 && Math.abs(lon) < 0.0001) return false;
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }
}

