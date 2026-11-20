package com.example.guias.controller;

import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import com.example.guias.service.VehiculoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/vehiculos")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class VehiculoController {

    /** Letras/números con guion opcional, 5 a 10 caracteres (ej: ABC-123, ABC123, XYZ12D). */
    private static final Pattern PLACA = Pattern.compile("^[A-Z0-9-]{5,10}$");

    @Autowired
    private VehiculoService vehiculoService;

    @GetMapping
    public String listarVehiculos(Model model) {
        List<Vehiculo> vehiculos = vehiculoService.obtenerTodos();
        List<Usuario> repartidoresSinVehiculo = vehiculoService.obtenerRepartidoresDisponiblesSinVehiculo();

        model.addAttribute("vehiculos", vehiculos);
        model.addAttribute("repartidoresSinVehiculo", repartidoresSinVehiculo);
        model.addAttribute("totalVehiculos", vehiculos.size());
        model.addAttribute("totalAsignados", vehiculos.stream().filter(v -> VehiculoService.ASIGNADO.equalsIgnoreCase(v.getEstado())).count());
        model.addAttribute("totalDisponibles", vehiculos.stream().filter(v -> VehiculoService.DISPONIBLE.equalsIgnoreCase(v.getEstado())).count());
        model.addAttribute("totalMantenimiento", vehiculos.stream().filter(v -> VehiculoService.MANTENIMIENTO.equalsIgnoreCase(v.getEstado())).count());
        return "admin-vehiculos";
    }

    /**
     * Crea o edita una furgoneta. Solo se aceptan del formulario placa, modelo y capacidades:
     * el estado y el conductor se gestionan únicamente por sus propios endpoints.
     */
    @PostMapping("/guardar")
    public String guardarVehiculo(@RequestParam(required = false) Long id,
                                  @RequestParam String placa,
                                  @RequestParam String modelo,
                                  @RequestParam(required = false) String tipo,
                                  @RequestParam Double capacidadPesoKg,
                                  @RequestParam Double capacidadVolumenM3,
                                  RedirectAttributes redirectAttributes) {
        String placaNorm = placa == null ? "" : placa.toUpperCase().trim();

        if (!PLACA.matcher(placaNorm).matches()) {
            redirectAttributes.addFlashAttribute("error", "Placa inválida. Use 5 a 10 letras/números (ej: ABC-123).");
            return "redirect:/admin/vehiculos";
        }
        if (modelo == null || modelo.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El modelo es obligatorio.");
            return "redirect:/admin/vehiculos";
        }
        if (capacidadPesoKg == null || capacidadPesoKg <= 0 || capacidadVolumenM3 == null || capacidadVolumenM3 <= 0) {
            redirectAttributes.addFlashAttribute("error", "Las capacidades de peso y volumen deben ser mayores a 0.");
            return "redirect:/admin/vehiculos";
        }

        Optional<Vehiculo> conMismaPlaca = vehiculoService.buscarPorPlaca(placaNorm);
        if (conMismaPlaca.isPresent() && !conMismaPlaca.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un vehículo con la placa " + placaNorm + ".");
            return "redirect:/admin/vehiculos";
        }

        try {
            Vehiculo vehiculo;
            if (id != null) {
                Optional<Vehiculo> existente = vehiculoService.buscarPorId(id);
                if (existente.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Vehículo no encontrado.");
                    return "redirect:/admin/vehiculos";
                }
                vehiculo = existente.get(); // conserva estado y conductor
            } else {
                vehiculo = new Vehiculo();
            }
            vehiculo.setPlaca(placaNorm);
            vehiculo.setModelo(modelo.trim());
            vehiculo.setTipo((tipo == null || tipo.isBlank()) ? "FURGONETA" : tipo.trim().toUpperCase());
            vehiculo.setCapacidadPesoKg(capacidadPesoKg);
            vehiculo.setCapacidadVolumenM3(capacidadVolumenM3);

            vehiculoService.guardarVehiculo(vehiculo);
            redirectAttributes.addFlashAttribute("mensaje", "Vehículo [" + placaNorm + "] guardado exitosamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar: la placa " + placaNorm + " ya existe.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el vehículo: " + e.getMessage());
        }
        return "redirect:/admin/vehiculos";
    }

    @PostMapping("/asignar-repartidor")
    public String asignarRepartidor(@RequestParam Long vehiculoId,
                                    @RequestParam(required = false) Long repartidorId,
                                    RedirectAttributes redirectAttributes) {
        try {
            String error = vehiculoService.asignarRepartidor(vehiculoId, repartidorId);
            if (error != null) {
                redirectAttributes.addFlashAttribute("error", error);
            } else if (repartidorId != null && repartidorId > 0) {
                redirectAttributes.addFlashAttribute("mensaje", "Conductor vinculado correctamente a la furgoneta.");
            } else {
                redirectAttributes.addFlashAttribute("mensaje", "Furgoneta liberada. Conductor desvinculado.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error en la asignación: " + e.getMessage());
        }
        return "redirect:/admin/vehiculos";
    }

    @PostMapping("/cambiar-estado")
    public String cambiarEstado(@RequestParam Long id,
                                @RequestParam String nuevoEstado,
                                RedirectAttributes redirectAttributes) {
        String error = vehiculoService.cambiarEstado(id, nuevoEstado);
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Estado del vehículo actualizado a " + nuevoEstado + ".");
        }
        return "redirect:/admin/vehiculos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarVehiculo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String error = vehiculoService.eliminarVehiculo(id);
            if (error != null) {
                redirectAttributes.addFlashAttribute("error", error);
            } else {
                redirectAttributes.addFlashAttribute("mensaje", "Vehículo eliminado del sistema.");
            }
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar: el vehículo tiene rutas asociadas.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el vehículo: " + e.getMessage());
        }
        return "redirect:/admin/vehiculos";
    }
}
