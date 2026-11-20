package com.example.guias.controller;

import com.example.guias.dto.RutaPlanDTO;
import com.example.guias.dto.RutaResumenDTO;
import com.example.guias.model.Ruta;
import com.example.guias.service.RutaService;
import com.example.guias.service.VehiculoService;
import com.example.guias.util.DistanciaUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/rutas")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class RutaAdminController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RutaService rutaService;

    @Autowired
    private VehiculoService vehiculoService;

    @GetMapping
    public String verPlanificador(Model model) {
        List<RutaResumenDTO> rutas = rutaService.obtenerTodasLasRutas();
        model.addAttribute("repartidores", vehiculoService.obtenerRepartidoresConVehiculoDTO());
        model.addAttribute("capitales", DistanciaUtil.getCatalogoCapitales());
        model.addAttribute("rutas", rutas);
        model.addAttribute("totalRutas", rutas.size());
        model.addAttribute("totalAsignadas", rutas.stream()
                .filter(r -> RutaService.ASIGNADA.equalsIgnoreCase(r.getEstado())).count());
        model.addAttribute("totalEnCurso", rutas.stream()
                .filter(r -> RutaService.EN_CURSO.equalsIgnoreCase(r.getEstado())).count());
        model.addAttribute("totalCompletadas", rutas.stream()
                .filter(r -> RutaService.COMPLETADA.equalsIgnoreCase(r.getEstado())).count());
        return "admin-rutas";
    }

    @GetMapping("/datos-repartidor/{repartidorId}")
    @ResponseBody
    public ResponseEntity<?> datosRepartidor(@PathVariable Long repartidorId,
                                             @RequestParam(defaultValue = "ENTREGA") String tipo) {
        try {
            RutaPlanDTO dto = rutaService.obtenerDatosRepartidorParaPlanificar(repartidorId, tipo);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/detalle/{rutaId}")
    @ResponseBody
    public ResponseEntity<?> detalleRuta(@PathVariable Long rutaId) {
        try {
            RutaResumenDTO dto = rutaService.obtenerDetalleRuta(rutaId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/asignar")
    public String asignarRuta(@RequestParam(required = false) Long repartidorId,
                              @RequestParam(required = false) String capitalHub,
                              @RequestParam(required = false) String guiaIdsJson,
                              @RequestParam(required = false) String coordenadasJson,
                              @RequestParam(required = false) Double distanciaTotalKm,
                              @RequestParam(required = false) Integer tiempoEstimadoMinutos,
                              @RequestParam(required = false) String geometriaJson,
                              @RequestParam(required = false, defaultValue = "ENTREGA") String tipo,
                              RedirectAttributes redirectAttributes) {
        try {
            List<Long> guiaIdsOrdenadas = parsearGuiaIds(guiaIdsJson);
            Map<Long, double[]> coordsActualizadas = parsearCoordenadas(coordenadasJson);

            Ruta guardada = rutaService.crearYAsignarRuta(
                    repartidorId,
                    capitalHub,
                    guiaIdsOrdenadas,
                    coordsActualizadas,
                    distanciaTotalKm,
                    tiempoEstimadoMinutos,
                    geometriaJson,
                    tipo
            );

            redirectAttributes.addFlashAttribute("mensaje",
                    "✅ Ruta de " + (guardada.esRecogida() ? "recogida" : "entrega") + " #" + guardada.getId() + " asignada a " + guardada.getRepartidor().getNombre()
                            + " desde hub " + guardada.getCapitalHub() + " (" + guardada.getParadas().size()
                            + " parada(s), " + guardada.getDistanciaTotalKm() + " km).");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "⚠️ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "⚠️ Error al procesar los datos de la ruta.");
        }
        return "redirect:/admin/rutas";
    }

    @PostMapping("/cancelar/{id}")
    public String cancelarRuta(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rutaService.cancelarRuta(id);
            redirectAttributes.addFlashAttribute("mensaje",
                    "🗑️ Ruta #" + id + " cancelada. Las guías quedaron disponibles para planificar una nueva ruta.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "⚠️ " + e.getMessage());
        }
        return "redirect:/admin/rutas";
    }

    private List<Long> parsearGuiaIds(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
    }

    private Map<Long, double[]> parsearCoordenadas(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Double>> raw = objectMapper.readValue(
                json, new TypeReference<Map<String, Map<String, Double>>>() {});
        Map<Long, double[]> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : raw.entrySet()) {
            Map<String, Double> val = entry.getValue();
            if (val != null && val.get("lat") != null && val.get("lon") != null) {
                result.put(Long.parseLong(entry.getKey()), new double[]{val.get("lat"), val.get("lon")});
            }
        }
        return result;
    }
}

