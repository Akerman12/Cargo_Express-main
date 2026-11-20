package com.example.guias.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "ASIGNADA"; // ASIGNADA, EN_CURSO, COMPLETADA

    @Column(name = "capital_hub", length = 100)
    private String capitalHub;

    @Column(name = "distancia_total_km")
    private Double distanciaTotalKm = 0.0;

    @Column(name = "tiempo_estimado_minutos")
    private Integer tiempoEstimadoMinutos = 0;

    @Lob
    @Column(name = "geometria_json", columnDefinition = "LONGTEXT")
    private String geometriaJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id", nullable = false)
    private Usuario repartidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = true)
    private Vehiculo vehiculo;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "ruta", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @OrderBy("ordenEntrega ASC")
    private List<Guia> guias = new ArrayList<>();

    /** RECOGIDA (paradas = origen de cada guía) o ENTREGA (paradas = destino). */
    @Column(name = "tipo", length = 20)
    private String tipo = "ENTREGA";

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "rutaRecogida", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @OrderBy("ordenRecogida ASC")
    private List<Guia> guiasRecogida = new ArrayList<>();

    public Ruta() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCapitalHub() {
        return capitalHub;
    }

    public void setCapitalHub(String capitalHub) {
        this.capitalHub = capitalHub;
    }

    public Double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }

    public void setDistanciaTotalKm(Double distanciaTotalKm) {
        this.distanciaTotalKm = distanciaTotalKm;
    }

    public Integer getTiempoEstimadoMinutos() {
        return tiempoEstimadoMinutos;
    }

    public void setTiempoEstimadoMinutos(Integer tiempoEstimadoMinutos) {
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }

    public String getGeometriaJson() {
        return geometriaJson;
    }

    public void setGeometriaJson(String geometriaJson) {
        this.geometriaJson = geometriaJson;
    }

    public Usuario getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(Usuario repartidor) {
        this.repartidor = repartidor;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public List<Guia> getGuias() {
        return guias;
    }

    public void setGuias(List<Guia> guias) {
        this.guias = guias;
    }

    public String getTipo() {
        return tipo == null ? "ENTREGA" : tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean esRecogida() {
        return "RECOGIDA".equalsIgnoreCase(tipo);
    }

    public List<Guia> getGuiasRecogida() {
        return guiasRecogida;
    }

    public void setGuiasRecogida(List<Guia> guiasRecogida) {
        this.guiasRecogida = guiasRecogida;
    }

    /** Paradas de la ruta según su tipo. */
    public List<Guia> getParadas() {
        return esRecogida() ? guiasRecogida : guias;
    }

    /** Agrega una parada en el siguiente orden, según el tipo de ruta. */
    public void agregarParada(Guia guia) {
        if (esRecogida()) {
            if (guiasRecogida == null) {
                guiasRecogida = new ArrayList<>();
            }
            guiasRecogida.add(guia);
            guia.setRutaRecogida(this);
            guia.setOrdenRecogida(guiasRecogida.size());
        } else {
            agregarGuia(guia);
        }
    }

    public void agregarGuia(Guia guia) {
        if (guias == null) {
            guias = new ArrayList<>();
        }
        guias.add(guia);
        guia.setRuta(this);
        guia.setOrdenEntrega(guias.size());
    }
}

