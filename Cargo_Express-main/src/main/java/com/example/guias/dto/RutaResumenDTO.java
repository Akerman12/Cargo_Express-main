package com.example.guias.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RutaResumenDTO {

    private Long id;
    private LocalDateTime fecha;
    private String estado;
    private String capitalHub;
    private Double hubLat;
    private Double hubLon;
    private Double distanciaTotalKm;
    private Integer tiempoEstimadoMinutos;
    private Long repartidorId;
    private String repartidorNombre;
    private String vehiculoPlaca;
    private String vehiculoModelo;
    private int cantidadParadas;
    private double pesoTotalKg;
    private String geometriaJson;
    private String tipo = "ENTREGA";
    private List<RutaPlanDTO.ParadaDTO> paradas = new ArrayList<>();

    public RutaResumenDTO() {
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

    public Double getHubLat() {
        return hubLat;
    }

    public void setHubLat(Double hubLat) {
        this.hubLat = hubLat;
    }

    public Double getHubLon() {
        return hubLon;
    }

    public void setHubLon(Double hubLon) {
        this.hubLon = hubLon;
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

    public Long getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(Long repartidorId) {
        this.repartidorId = repartidorId;
    }

    public String getRepartidorNombre() {
        return repartidorNombre;
    }

    public void setRepartidorNombre(String repartidorNombre) {
        this.repartidorNombre = repartidorNombre;
    }

    public String getVehiculoPlaca() {
        return vehiculoPlaca;
    }

    public void setVehiculoPlaca(String vehiculoPlaca) {
        this.vehiculoPlaca = vehiculoPlaca;
    }

    public String getVehiculoModelo() {
        return vehiculoModelo;
    }

    public void setVehiculoModelo(String vehiculoModelo) {
        this.vehiculoModelo = vehiculoModelo;
    }

    public int getCantidadParadas() {
        return cantidadParadas;
    }

    public void setCantidadParadas(int cantidadParadas) {
        this.cantidadParadas = cantidadParadas;
    }

    public double getPesoTotalKg() {
        return pesoTotalKg;
    }

    public void setPesoTotalKg(double pesoTotalKg) {
        this.pesoTotalKg = pesoTotalKg;
    }

    public String getGeometriaJson() {
        return geometriaJson;
    }

    public void setGeometriaJson(String geometriaJson) {
        this.geometriaJson = geometriaJson;
    }

    public List<RutaPlanDTO.ParadaDTO> getParadas() {
        return paradas;
    }

    public void setParadas(List<RutaPlanDTO.ParadaDTO> paradas) {
        this.paradas = paradas;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
