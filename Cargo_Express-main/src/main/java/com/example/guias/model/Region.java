package com.example.guias.model;

import com.example.guias.util.DistanciaUtil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Regiones naturales de Colombia usadas para limitar la operación de los repartidores.
 * Un repartidor solo puede recoger/entregar guías cuyo departamento (origen o destino según la fase)
 * pertenezca a su región.
 */
public enum Region {

    CARIBE("Región Caribe",
            "Atlántico", "Bolívar", "Cesar", "Córdoba", "La Guajira", "Magdalena", "Sucre"),
    ANDINA("Región Andina",
            "Antioquia", "Boyacá", "Caldas", "Cundinamarca", "Huila", "Norte de Santander",
            "Quindío", "Risaralda", "Santander", "Tolima", "Bogotá D.C."),
    PACIFICA("Región Pacífica",
            "Cauca", "Chocó", "Nariño", "Valle del Cauca"),
    ORINOQUIA("Región de la Orinoquía",
            "Arauca", "Casanare", "Meta", "Vichada"),
    AMAZONIA("Región Amazónica",
            "Amazonas", "Caquetá", "Guainía", "Guaviare", "Putumayo", "Vaupés"),
    INSULAR("Región Insular",
            "San Andrés y Providencia");

    private static final Map<String, Region> POR_DEPARTAMENTO = new HashMap<>();

    static {
        for (Region r : values()) {
            for (String d : r.departamentos) {
                POR_DEPARTAMENTO.put(clave(d), r);
            }
        }
        // Variantes habituales de escritura
        POR_DEPARTAMENTO.put(clave("Bogotá"), ANDINA);
        POR_DEPARTAMENTO.put(clave("Bogota D.C"), ANDINA);
        POR_DEPARTAMENTO.put(clave("San Andrés"), INSULAR);
        POR_DEPARTAMENTO.put(clave("San Andres Providencia"), INSULAR);
        POR_DEPARTAMENTO.put(clave("Archipiélago de San Andrés, Providencia y Santa Catalina"), INSULAR);
    }

    private final String etiqueta;
    private final List<String> departamentos;

    Region(String etiqueta, String... departamentos) {
        this.etiqueta = etiqueta;
        this.departamentos = List.of(departamentos);
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public List<String> getDepartamentos() {
        return departamentos;
    }

    private static String clave(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace(".", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Región a la que pertenece un departamento; null si es vacío o desconocido. */
    public static Region deDepartamento(String departamento) {
        if (departamento == null || departamento.isBlank()) return null;
        return POR_DEPARTAMENTO.get(clave(departamento));
    }

    /** Capitales (hubs) de la región, en mayúsculas, sin repetidos. */
    public List<String> getCapitales() {
        Set<String> r = new LinkedHashSet<>();
        for (String d : departamentos) {
            r.add(DistanciaUtil.obtenerCapitalPorDepartamento(d).toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(r);
    }

    public boolean contieneCapital(String capital) {
        if (capital == null) return false;
        String k = clave(capital);
        for (String c : getCapitales()) {
            if (clave(c).equals(k)) return true;
        }
        return false;
    }
}

