package com.example.guias.controller;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.guias.model.EstadoGuia;
import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.service.GuiaService;

/**
 * Datos agregados para las graficas del panel principal (/menu).
 * Respeta el mismo criterio de visibilidad que MenuController:
 * admin y repartidor ven todas las guias; un cliente solo ve las suyas.
 */
@Controller
public class DashboardStatsController {

    private static final String[] MESES = {
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    };

    @Autowired
    private GuiaService guiaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/menu/estadisticas")
    @ResponseBody
    public Map<String, Object> obtenerEstadisticas(Authentication authentication) {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        if (authentication == null) {
            respuesta.put("error", "No autenticado");
            return respuesta;
        }

        List<Guia> guias = obtenerGuiasVisibles(authentication);

        long total = guias.size();
        double pesoTotal = guias.stream()
                .mapToDouble(Guia::getPesoKg)
                .sum();
        double ingresoTotal = guias.stream()
                .mapToDouble(Guia::getPrecio)
                .sum();

        respuesta.put("total", total);
        respuesta.put("pesoTotal", redondear(pesoTotal, 1));
        respuesta.put("pesoPromedio", total > 0 ? redondear(pesoTotal / total, 1) : 0);
        respuesta.put("ingresoTotal", redondear(ingresoTotal, 0));
        respuesta.put("precioPromedio", total > 0 ? redondear(ingresoTotal / total, 0) : 0);
        respuesta.put("ciudadesDistintas", contarCiudadesDistintas(guias));

        respuesta.put("mensual", serieMensual(guias));
        respuesta.put("anual", serieAnual(guias));
        respuesta.put("destinos", topCiudades(guias, true, 8));
        respuesta.put("origenes", topCiudades(guias, false, 8));
        respuesta.put("tipos", distribucionTipos(guias));
        respuesta.put("pesos", distribucionPesos(guias));
        respuesta.put("estados", distribucionEstados(guias));

        respuesta.put("tipoTop", primerLabel(respuesta.get("tipos")));
        respuesta.put("destinoTop", primerLabel(respuesta.get("destinos")));

        return respuesta;
    }

    private List<Guia> obtenerGuiasVisibles(Authentication authentication) {
        boolean adminORepartidor = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol -> rol.equals("ROLE_ADMIN") || rol.equals("ROLE_REPARTIDOR"));

        if (adminORepartidor) {
            return guiaService.obtenerTodas();
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(authentication.getName());
        return usuarioOpt
                .map(guiaService::obtenerPorUsuario)
                .orElseGet(List::of);
    }

    private Map<String, Object> serieMensual(List<Guia> guias) {
        YearMonth ultimoMes = guias.stream()
                .map(Guia::getFechaCreacion)
                .filter(Objects::nonNull)
                .map(fecha -> YearMonth.from(fecha))
                .max(Comparator.naturalOrder())
                .orElse(YearMonth.now());

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth mes = ultimoMes.minusMonths(i);
            long cantidad = guias.stream()
                    .filter(g -> g.getFechaCreacion() != null)
                    .filter(g -> YearMonth.from(g.getFechaCreacion()).equals(mes))
                    .count();

            labels.add(MESES[mes.getMonthValue() - 1] + " " + mes.getYear());
            data.add(cantidad);
        }

        return serie(labels, data);
    }

    private Map<String, Object> serieAnual(List<Guia> guias) {
        Map<Integer, Long> porAnio = new TreeMap<>();
        for (Guia guia : guias) {
            if (guia.getFechaCreacion() == null) {
                continue;
            }
            int anio = guia.getFechaCreacion().getYear();
            porAnio.merge(anio, 1L, Long::sum);
        }

        if (porAnio.isEmpty()) {
            porAnio.put(LocalDateTime.now().getYear(), 0L);
        }

        List<String> labels = porAnio.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        List<Long> data = new ArrayList<>(porAnio.values());

        return serie(labels, data);
    }

    private Map<String, Object> topCiudades(List<Guia> guias, boolean destino, int limite) {
        Map<String, Long> conteo = guias.stream()
                .map(g -> destino ? g.getCiudadDestino() : g.getCiudadOrigen())
                .filter(this::textoValido)
                .map(String::trim)
                .collect(Collectors.groupingBy(ciudad -> ciudad, Collectors.counting()));

        List<Map.Entry<String, Long>> ordenadas = conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(limite)
                .collect(Collectors.toList());

        List<String> labels = ordenadas.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<Long> data = ordenadas.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        return serie(labels, data);
    }

    private Map<String, Object> distribucionTipos(List<Guia> guias) {
        Map<String, Long> conteo = guias.stream()
                .map(g -> textoValido(g.getTipoCarga()) ? g.getTipoCarga().trim() : "Sin clasificar")
                .collect(Collectors.groupingBy(tipo -> tipo, Collectors.counting()));

        return serieDesdeMapa(conteo, 6);
    }

    private Map<String, Object> distribucionPesos(List<Guia> guias) {
        String[] rangos = {"0 - 1 kg", "1 - 5 kg", "5 - 10 kg", "10 - 20 kg", "20 - 50 kg", "50+ kg"};
        long[] conteo = new long[rangos.length];

        for (Guia guia : guias) {
            double peso = guia.getPesoKg();
            int indice;
            if (peso < 1) {
                indice = 0;
            } else if (peso < 5) {
                indice = 1;
            } else if (peso < 10) {
                indice = 2;
            } else if (peso < 20) {
                indice = 3;
            } else if (peso < 50) {
                indice = 4;
            } else {
                indice = 5;
            }
            conteo[indice]++;
        }

        List<String> labels = List.of(rangos);
        List<Long> data = new ArrayList<>();
        for (long valor : conteo) {
            data.add(valor);
        }

        return serie(labels, data);
    }

    private Map<String, Object> distribucionEstados(List<Guia> guias) {
        Map<String, Long> conteo = guias.stream()
                .map(g -> textoValido(g.getEstado()) ? EstadoGuia.normalizar(g.getEstado()) : "Sin estado")
                .collect(Collectors.groupingBy(estado -> estado, Collectors.counting()));

        return serieDesdeMapa(conteo, 10);
    }

    private Map<String, Object> serieDesdeMapa(Map<String, Long> conteo, int limite) {
        List<Map.Entry<String, Long>> ordenadas = conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        long otros = 0;

        for (int i = 0; i < ordenadas.size(); i++) {
            if (i < limite) {
                labels.add(ordenadas.get(i).getKey());
                data.add(ordenadas.get(i).getValue());
            } else {
                otros += ordenadas.get(i).getValue();
            }
        }

        if (otros > 0) {
            labels.add("Otros");
            data.add(otros);
        }

        return serie(labels, data);
    }

    private Map<String, Object> serie(List<String> labels, List<Long> data) {
        Map<String, Object> serie = new LinkedHashMap<>();
        serie.put("labels", labels);
        serie.put("data", data);
        return serie;
    }

    @SuppressWarnings("unchecked")
    private String primerLabel(Object serie) {
        if (!(serie instanceof Map)) {
            return "";
        }
        Object labels = ((Map<String, Object>) serie).get("labels");
        if (labels instanceof List && !((List<?>) labels).isEmpty()) {
            return String.valueOf(((List<?>) labels).get(0));
        }
        return "";
    }

    private long contarCiudadesDistintas(List<Guia> guias) {
        return guias.stream()
                .map(Guia::getCiudadDestino)
                .filter(this::textoValido)
                .map(String::trim)
                .map(ciudad -> ciudad.toLowerCase(Locale.ROOT))
                .distinct()
                .count();
    }

    private boolean textoValido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }
}
