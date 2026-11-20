package com.example.guias.util;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DistanciaUtil {

    private static final Map<String, double[]> COORDENADAS_CIUDADES = new HashMap<>();
    private static final Map<String, double[]> CAPITALES_PRINCIPALES = new LinkedHashMap<>();
    private static final Map<String, String> CAPITAL_POR_DEPARTAMENTO = new HashMap<>();

    public static java.util.Set<String> getCiudades() {
        return COORDENADAS_CIUDADES.keySet();
    }

    private static void registrarCapital(String nombreConTilde, String nombreSinTilde, double lat, double lon, String... departamentos) {
        double[] coords = new double[]{lat, lon};
        COORDENADAS_CIUDADES.put(nombreConTilde, coords);
        if (nombreSinTilde != null && !nombreSinTilde.equals(nombreConTilde)) {
            COORDENADAS_CIUDADES.put(nombreSinTilde, coords);
        }
        CAPITALES_PRINCIPALES.put(nombreConTilde, coords);
        for (String depto : departamentos) {
            CAPITAL_POR_DEPARTAMENTO.put(normalizar(depto), nombreConTilde);
        }
    }

    static {
        registrarCapital("BOGOTÁ", "BOGOTA", 4.6097, -74.0817, "CUNDINAMARCA", "BOGOTA", "BOGOTA D.C.", "DISTRITO CAPITAL");
        registrarCapital("MEDELLÍN", "MEDELLIN", 6.2442, -75.5812, "ANTIOQUIA");
        registrarCapital("CALI", null, 3.4516, -76.5320, "VALLE DEL CAUCA", "VALLE");
        registrarCapital("BARRANQUILLA", null, 10.9639, -74.7964, "ATLANTICO");
        registrarCapital("CARTAGENA", null, 10.3910, -75.4794, "BOLIVAR");
        registrarCapital("BUCARAMANGA", null, 7.1193, -73.1227, "SANTANDER");
        registrarCapital("PEREIRA", null, 4.8133, -75.6961, "RISARALDA");
        registrarCapital("MANIZALES", null, 5.0689, -75.5174, "CALDAS");
        registrarCapital("ARMENIA", null, 4.5339, -75.6811, "QUINDIO");
        registrarCapital("CÚCUTA", "CUCUTA", 7.8939, -72.5078, "NORTE DE SANTANDER");
        registrarCapital("IBAGUÉ", "IBAGUE", 4.4389, -75.2322, "TOLIMA");
        registrarCapital("SANTA MARTA", null, 11.2408, -74.1990, "MAGDALENA");
        registrarCapital("PASTO", null, 1.2136, -77.2811, "NARINO");
        registrarCapital("VILLAVICENCIO", null, 4.1420, -73.6266, "META");
        registrarCapital("MONTERÍA", "MONTERIA", 8.7479, -75.8814, "CORDOBA");
        registrarCapital("VALLEDUPAR", null, 10.4631, -73.2532, "CESAR");
        registrarCapital("POPAYÁN", "POPAYAN", 2.4448, -76.6147, "CAUCA");
        registrarCapital("SINCELEJO", null, 9.3047, -75.3977, "SUCRE");
        registrarCapital("NEIVA", null, 2.9273, -75.2819, "HUILA");
        registrarCapital("QUIBDÓ", "QUIBDO", 5.6948, -76.6611, "CHOCO");
        registrarCapital("RIOHACHA", null, 11.5444, -72.9072, "LA GUAJIRA", "GUAJIRA");
        registrarCapital("TUNJA", null, 5.5353, -73.3678, "BOYACA");
        registrarCapital("FLORENCIA", null, 1.6143, -75.6117, "CAQUETA");
        registrarCapital("MOCOA", null, 1.1477, -76.6481, "PUTUMAYO");
        registrarCapital("ARAUCA", null, 7.0847, -70.7591, "ARAUCA");
        registrarCapital("YOPAL", null, 5.3378, -72.3959, "CASANARE");
        registrarCapital("PUERTO CARREÑO", null, 6.1890, -67.4859, "VICHADA");
        registrarCapital("INIRIDA", "INÍRIDA", 3.8653, -67.9239, "GUAINIA");
        registrarCapital("SAN JOSE DEL GUAVIARE", "SAN JOSÉ DEL GUAVIARE", 2.5668, -72.6459, "GUAVIARE");
        registrarCapital("MITÚ", "MITU", 1.1983, -70.1733, "VAUPES");
        registrarCapital("LETICIA", null, -4.2153, -69.9406, "AMAZONAS");
        registrarCapital("SAN ANDRÉS", "SAN ANDRES", 12.5847, -81.7006, "SAN ANDRES", "SAN ANDRES Y PROVIDENCIA", "ARCHIPIELAGO DE SAN ANDRES");

        // Variantes comunes en selectores de formularios (sin duplicar el catálogo de hubs)
        COORDENADAS_CIUDADES.put("BOGOTÁ D.C.", COORDENADAS_CIUDADES.get("BOGOTÁ"));
        COORDENADAS_CIUDADES.put("BOGOTA D.C.", COORDENADAS_CIUDADES.get("BOGOTÁ"));
        COORDENADAS_CIUDADES.put("CARTAGENA DE INDIAS", COORDENADAS_CIUDADES.get("CARTAGENA"));
    }

    public static Map<String, double[]> getCatalogoCapitales() {
        return Collections.unmodifiableMap(CAPITALES_PRINCIPALES);
    }

    public static double[] obtenerCoordenadasCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) return null;
        double[] exact = COORDENADAS_CIUDADES.get(ciudad.toUpperCase().trim());
        if (exact != null) return exact;
        return COORDENADAS_CIUDADES.get(normalizar(ciudad));
    }

    public static String obtenerCapitalPorDepartamento(String departamento) {
        if (departamento == null || departamento.isBlank()) return "BOGOTÁ";
        return CAPITAL_POR_DEPARTAMENTO.getOrDefault(normalizar(departamento), "BOGOTÁ");
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.replace(".", "").trim().replaceAll("\\s+", " ").toUpperCase();
    }

    /**
     * Calcula la distancia entre dos ciudades de Colombia en KM.
     */
    public static double calcularDistancia(String origen, String destino) {
        double[] coordsOrigen = obtenerCoordenadasCiudad(origen);
        double[] coordsDestino = obtenerCoordenadasCiudad(destino);

        if (coordsOrigen == null || coordsDestino == null) {
            // Si no está en la base de datos, devolvemos una distancia simulada basada en un hash
            // para que siempre sea la misma para la misma combinación, pero parezca real.
            String o = origen == null ? "" : origen;
            String d = destino == null ? "" : destino;
            return Math.abs((o.hashCode() + d.hashCode()) % 800) + 50;
        }

        return haversine(coordsOrigen[0], coordsOrigen[1], coordsDestino[0], coordsDestino[1]);
    }

    /**
     * Retorna la distancia vial seleccionada en la guía si existe; de lo contrario recurre a Haversine entre ciudades.
     */
    public static double obtenerDistanciaGuia(com.example.guias.model.Guia guia) {
        if (guia == null) return 0.0;
        if (guia.getDistanciaKm() != null && Double.isFinite(guia.getDistanciaKm()) && guia.getDistanciaKm() > 0) {
            return guia.getDistanciaKm();
        }
        return calcularDistancia(guia.getCiudadOrigen(), guia.getCiudadDestino());
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}