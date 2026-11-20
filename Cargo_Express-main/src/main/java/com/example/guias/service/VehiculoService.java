package com.example.guias.service;

import com.example.guias.dto.RepartidorVehiculoDTO;
import com.example.guias.model.Guia;
import com.example.guias.model.Rol;
import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehiculoService {

    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String ASIGNADO = "ASIGNADO";
    public static final String MANTENIMIENTO = "MANTENIMIENTO";

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GuiaService guiaService;

    // ------------------------------------------------------------------ consultas

    public List<Vehiculo> obtenerTodos() {
        return vehiculoRepository.findAll();
    }

    public Optional<Vehiculo> buscarPorId(Long id) {
        return vehiculoRepository.findById(id);
    }

    public Optional<Vehiculo> buscarPorPlaca(String placa) {
        if (placa == null) return Optional.empty();
        return vehiculoRepository.findByPlaca(placa.toUpperCase().trim());
    }

    public Optional<Vehiculo> buscarPorRepartidor(Usuario repartidor) {
        if (repartidor == null) return Optional.empty();
        return vehiculoRepository.findByRepartidor(repartidor);
    }

    /** IDs de los repartidores que ya tienen vehículo (una sola consulta). */
    private Set<Long> idsRepartidoresConVehiculo() {
        return vehiculoRepository.findByRepartidorIsNotNull().stream()
                .map(v -> v.getRepartidor().getId())
                .collect(Collectors.toSet());
    }

    /** Repartidores aprobados que NO tienen vehículo asignado. */
    public List<Usuario> obtenerRepartidoresDisponiblesSinVehiculo() {
        Set<Long> conVehiculo = idsRepartidoresConVehiculo();
        return usuarioRepository.findByRol(Rol.ROLE_REPARTIDOR).stream()
                .filter(Usuario::isAprobado)
                .filter(rep -> !conVehiculo.contains(rep.getId()))
                .collect(Collectors.toList());
    }

    /** Repartidores aprobados que SÍ tienen vehículo asignado. */
    public List<Usuario> obtenerRepartidoresConVehiculo() {
        Set<Long> conVehiculo = idsRepartidoresConVehiculo();
        return usuarioRepository.findByRol(Rol.ROLE_REPARTIDOR).stream()
                .filter(Usuario::isAprobado)
                .filter(rep -> conVehiculo.contains(rep.getId()))
                .collect(Collectors.toList());
    }

    /** Repartidores con vehículo + su nivel de carga, listos para el desplegable de asignación. */
    public List<RepartidorVehiculoDTO> obtenerRepartidoresConVehiculoDTO() {
        return obtenerRepartidoresConVehiculo().stream().map(rep -> {
            Optional<Vehiculo> vOpt = buscarPorRepartidor(rep);
            if (vOpt.isEmpty()) return null;
            Vehiculo v = vOpt.get();
            // Una sola consulta de guías por repartidor
            double pesoOcupado = guiaService.obtenerPorRepartidorAsignado(rep).stream()
                    .mapToDouble(Guia::getPesoKg).sum();
            double porcentaje = v.getCapacidadPesoKg() > 0
                    ? Math.min(100.0, Math.round(pesoOcupado / v.getCapacidadPesoKg() * 1000.0) / 10.0)
                    : 0.0;
            RepartidorVehiculoDTO dto = new RepartidorVehiculoDTO(rep.getId(), rep.getNombre(), rep.getUsername(),
                    v.getPlaca(), v.getModelo(), v.getCapacidadPesoKg(), pesoOcupado, porcentaje);
            if (rep.getRegionOperacion() != null) {
                dto.setRegionOperacion(rep.getRegionOperacion().name());
                dto.setRegionEtiqueta(rep.getRegionOperacion().getEtiqueta());
            }
            return dto;
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ escritura

    @Transactional
    public Vehiculo guardarVehiculo(Vehiculo vehiculo) {
        if (vehiculo.getPlaca() != null) {
            vehiculo.setPlaca(vehiculo.getPlaca().toUpperCase().trim());
        }
        return vehiculoRepository.save(vehiculo);
    }

    /**
     * Mensaje si el vehículo NO puede quedar sin conductor (porque este tiene paquetes activos),
     * o null si se puede liberar sin problema. Evita paquetes asignados a un repartidor sin furgoneta,
     * que dejarían sin control el límite de capacidad.
     */
    public String motivoNoLiberable(Vehiculo v) {
        if (v.getRepartidor() == null) return null;
        int activas = guiaService.obtenerPorRepartidorAsignado(v.getRepartidor()).size();
        if (activas > 0) {
            return "⚠️ " + v.getRepartidor().getNombre() + " tiene " + activas
                    + " guía(s) activa(s) asignada(s). Reasígnelas o entréguelas antes de desvincular la furgoneta ["
                    + v.getPlaca() + "].";
        }
        return null;
    }

    /**
     * Vincula (o desvincula si repartidorId es null/0) un conductor a una furgoneta.
     *
     * @return null si salió bien; si no, el mensaje de error para mostrar al administrador.
     */
    @Transactional
    public String asignarRepartidor(Long vehiculoId, Long repartidorId) {
        Optional<Vehiculo> vehiculoOpt = vehiculoRepository.findById(vehiculoId);
        if (vehiculoOpt.isEmpty()) return "Vehículo no encontrado.";
        Vehiculo vehiculo = vehiculoOpt.get();

        // --- Desvincular
        if (repartidorId == null || repartidorId <= 0) {
            String bloqueo = motivoNoLiberable(vehiculo);
            if (bloqueo != null) return bloqueo;
            vehiculo.setRepartidor(null); // pasa a DISPONIBLE solo si estaba ASIGNADO (no saca de MANTENIMIENTO)
            vehiculoRepository.save(vehiculo);
            return null;
        }

        // --- Vincular
        if (MANTENIMIENTO.equalsIgnoreCase(vehiculo.getEstado())) {
            return "La furgoneta [" + vehiculo.getPlaca() + "] está en mantenimiento. Reactívela antes de asignarle un conductor.";
        }

        Optional<Usuario> repartidorOpt = usuarioRepository.findById(repartidorId);
        if (repartidorOpt.isEmpty()) return "Repartidor no encontrado.";
        Usuario nuevo = repartidorOpt.get();
        if (nuevo.getRol() != Rol.ROLE_REPARTIDOR || !nuevo.isAprobado()) {
            return "El usuario seleccionado no es un repartidor aprobado.";
        }

        // Si ya es el conductor actual, no hay nada que hacer
        if (vehiculo.getRepartidor() != null && vehiculo.getRepartidor().getId().equals(nuevo.getId())) {
            return null;
        }

        // Si la furgoneta ya tenía OTRO conductor con paquetes, no se le puede quitar la furgoneta
        String bloqueo = motivoNoLiberable(vehiculo);
        if (bloqueo != null) return bloqueo;

        // Si el nuevo conductor ya tenía otra furgoneta, se libera (solo si no tiene paquetes activos)
        Optional<Vehiculo> previo = vehiculoRepository.findByRepartidor(nuevo);
        if (previo.isPresent() && !previo.get().getId().equals(vehiculoId)) {
            Vehiculo anterior = previo.get();
            anterior.setRepartidor(null);
            // flush inmediato: la columna repartidor_id es UNIQUE y MySQL no tolera dos filas
            // con el mismo conductor ni siquiera dentro de la misma transacción
            vehiculoRepository.saveAndFlush(anterior);
        }

        vehiculo.setRepartidor(nuevo); // pasa a ASIGNADO
        vehiculoRepository.save(vehiculo);
        return null;
    }

    /** Cambia el estado manualmente (solo DISPONIBLE <-> MANTENIMIENTO). @return null si OK, o el error. */
    @Transactional
    public String cambiarEstado(Long id, String nuevoEstado) {
        Optional<Vehiculo> vOpt = vehiculoRepository.findById(id);
        if (vOpt.isEmpty()) return "Vehículo no encontrado.";
        Vehiculo v = vOpt.get();

        if (MANTENIMIENTO.equalsIgnoreCase(nuevoEstado)) {
            String bloqueo = motivoNoLiberable(v);
            if (bloqueo != null) return bloqueo;
            v.setRepartidor(null);
            v.setEstado(MANTENIMIENTO);
        } else if (DISPONIBLE.equalsIgnoreCase(nuevoEstado)) {
            if (v.getRepartidor() != null) {
                return "La furgoneta tiene conductor asignado; su estado es ASIGNADO.";
            }
            v.setEstado(DISPONIBLE);
        } else {
            return "Estado no válido: " + nuevoEstado;
        }
        vehiculoRepository.save(v);
        return null;
    }

    /** Elimina una furgoneta. @return null si OK, o el error. */
    @Transactional
    public String eliminarVehiculo(Long id) {
        Optional<Vehiculo> vOpt = vehiculoRepository.findById(id);
        if (vOpt.isEmpty()) return "Vehículo no encontrado.";
        Vehiculo v = vOpt.get();
        String bloqueo = motivoNoLiberable(v);
        if (bloqueo != null) return bloqueo;
        v.setRepartidor(null);
        vehiculoRepository.delete(v);
        return null;
    }

    /**
     * Libera la furgoneta de un usuario que deja de ser repartidor o se elimina del sistema.
     * No valida paquetes: el llamador decide (AdminController los desasigna antes).
     */
    @Transactional
    public void liberarVehiculoDe(Usuario usuario) {
        vehiculoRepository.findByRepartidor(usuario).ifPresent(v -> {
            v.setRepartidor(null);
            vehiculoRepository.saveAndFlush(v);
        });
    }

    // ------------------------------------------------------------------ capacidad

    /**
     * Volumen estimado de una guía en m³.
     * NOTA: Guia no tiene campo "largo"; se usa 20 cm fijo, igual que el peso volumétrico
     * de PriceCalculatorService. Si se agrega "largoCm" a Guia, cambiar aquí.
     */
    public double volumenGuiaM3(Guia g) {
        return (g.getAnchoCm() * g.getAlturaCm() * 20.0) / 1_000_000.0;
    }

    public double calcularPesoOcupado(Usuario repartidor) {
        if (repartidor == null) return 0.0;
        return guiaService.obtenerPorRepartidorAsignado(repartidor).stream()
                .mapToDouble(Guia::getPesoKg).sum();
    }

    public double calcularVolumenOcupadoM3(Usuario repartidor) {
        if (repartidor == null) return 0.0;
        return guiaService.obtenerPorRepartidorAsignado(repartidor).stream()
                .mapToDouble(this::volumenGuiaM3).sum();
    }

    /**
     * Valida si el vehículo del repartidor soporta la guía. La guía que se está asignando se
     * excluye de la carga actual (evita doble conteo si ya estaba asignada al mismo repartidor).
     *
     * @return null si cabe; si no, el mensaje de error listo para mostrar al administrador.
     */
    public String validarAsignacion(Usuario repartidor, Guia guia) {
        Optional<Vehiculo> vOpt = buscarPorRepartidor(repartidor);
        if (vOpt.isEmpty()) {
            return "⚠️ El repartidor " + repartidor.getNombre()
                    + " no tiene ninguna furgoneta asignada. Asígnele un vehículo en Gestión de Flota.";
        }
        Vehiculo v = vOpt.get();

        List<Guia> otras = guiaService.obtenerPorRepartidorAsignado(repartidor).stream()
                .filter(g -> !g.getId().equals(guia.getId()))
                .collect(Collectors.toList());

        double pesoActual = otras.stream().mapToDouble(Guia::getPesoKg).sum();
        double pesoTotal = pesoActual + guia.getPesoKg();
        if (pesoTotal > v.getCapacidadPesoKg()) {
            return "⚠️ Capacidad superada: la furgoneta [" + v.getPlaca() + " - " + v.getModelo()
                    + "] soporta máximo " + Math.round(v.getCapacidadPesoKg()) + " kg. Actualmente lleva "
                    + Math.round(pesoActual) + " kg y el paquete pesa " + Math.round(guia.getPesoKg())
                    + " kg (total requerido: " + Math.round(pesoTotal) + " kg).";
        }

        if (v.getCapacidadVolumenM3() != null && v.getCapacidadVolumenM3() > 0) {
            double volActual = otras.stream().mapToDouble(this::volumenGuiaM3).sum();
            double volTotal = volActual + volumenGuiaM3(guia);
            if (volTotal > v.getCapacidadVolumenM3()) {
                return "⚠️ Volumen superado: la furgoneta [" + v.getPlaca() + "] soporta máximo "
                        + v.getCapacidadVolumenM3() + " m³. Con este paquete ocuparía "
                        + String.format("%.2f", volTotal) + " m³.";
            }
        }
        return null;
    }
}
