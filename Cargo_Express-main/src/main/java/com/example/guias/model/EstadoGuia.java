package com.example.guias.model;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo único de estados de una guía y de las transiciones permitidas.
 * El estado se sigue guardando como texto en la columna {@code estado} (sin migración de esquema).
 *
 * Flujo: Pendiente -> Recogido | No Recogido -> En Sucursal -> En Tránsito -> Entregado | Novedad.
 */
public final class EstadoGuia {

    public static final String PENDIENTE = "Pendiente";
    public static final String RECOGIDO = "Recogido";
    public static final String NO_RECOGIDO = "No Recogido";
    public static final String EN_SUCURSAL = "En Sucursal";
    public static final String EN_TRANSITO = "En Tránsito";
    public static final String NOVEDAD = "Novedad";
    public static final String ENTREGADO = "Entregado";
    public static final String CANCELADO = "Cancelado";
    public static final String ELIMINADA = "Eliminada";

    /** Estados que puede fijar un repartidor desde su pantalla (No Recogido y Novedad llevan motivo y tienen su propio endpoint). */
    public static final Set<String> OPERATIVOS_REPARTIDOR = Set.of(RECOGIDO, EN_SUCURSAL, EN_TRANSITO);

    private static final Map<String, Set<String>> TRANSICIONES = new LinkedHashMap<>();

    static {
        TRANSICIONES.put(PENDIENTE, setOf(RECOGIDO, NO_RECOGIDO, CANCELADO));
        TRANSICIONES.put(NO_RECOGIDO, setOf(PENDIENTE, CANCELADO));
        TRANSICIONES.put(RECOGIDO, setOf(EN_SUCURSAL, NO_RECOGIDO, CANCELADO));
        TRANSICIONES.put(EN_SUCURSAL, setOf(EN_TRANSITO, CANCELADO));
        TRANSICIONES.put(EN_TRANSITO, setOf(ENTREGADO, NOVEDAD, EN_SUCURSAL));
        TRANSICIONES.put(NOVEDAD, setOf(EN_SUCURSAL, EN_TRANSITO, ENTREGADO));
        TRANSICIONES.put(ENTREGADO, setOf());
        TRANSICIONES.put(CANCELADO, setOf());
        TRANSICIONES.put(ELIMINADA, setOf());
    }

    private EstadoGuia() {
    }

    private static Set<String> setOf(String... valores) {
        Set<String> s = new LinkedHashSet<>();
        for (String v : valores) {
            s.add(v);
        }
        return s;
    }

    private static String clave(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Devuelve el nombre canónico del estado. Reconoce los alias históricos:
     * "En Bodega" -> "En Sucursal"; "En tránsito" / "En Reparto" -> "En Tránsito".
     * Un estado desconocido se devuelve tal cual (sin espacios sobrantes).
     */
    public static String normalizar(String estado) {
        if (estado == null) return null;
        String k = clave(estado);
        switch (k) {
            case "pendiente": return PENDIENTE;
            case "recogido": return RECOGIDO;
            case "no recogido": return NO_RECOGIDO;
            case "en sucursal":
            case "en bodega": return EN_SUCURSAL;
            case "en transito":
            case "en reparto": return EN_TRANSITO;
            case "novedad": return NOVEDAD;
            case "entregado": return ENTREGADO;
            case "cancelado": return CANCELADO;
            case "eliminada": return ELIMINADA;
            default: return estado.trim();
        }
    }

    /** true si el estado de la guía equivale (con alias) al estado canónico indicado. */
    public static boolean es(String estado, String canonico) {
        String n = normalizar(estado);
        return n != null && n.equalsIgnoreCase(canonico);
    }

    public static boolean esUnoDe(String estado, String... canonicos) {
        for (String c : canonicos) {
            if (es(estado, c)) return true;
        }
        return false;
    }

    /** Una guía sin estado se considera Pendiente. */
    public static boolean transicionValida(String desde, String hasta) {
        String d = desde == null ? PENDIENTE : normalizar(desde);
        String h = normalizar(hasta);
        if (h == null || d == null) return false;
        if (d.equalsIgnoreCase(h)) return true;
        Set<String> permitidos = TRANSICIONES.get(d);
        return permitidos != null && permitidos.contains(h);
    }

    public static Set<String> destinosPosibles(String desde) {
        Set<String> r = TRANSICIONES.get(normalizar(desde));
        return r == null ? Set.of() : r;
    }
}

