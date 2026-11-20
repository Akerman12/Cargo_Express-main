package com.example.guias.dto;

public class RepartidorVehiculoDTO {

    private Long id;
    private String nombre;
    private String username;
    private String placa;
    private String modelo;
    private Double capacidadPesoKg;
    private Double pesoOcupadoKg;
    private Double pesoDisponibleKg;
    private Double porcentajeOcupacion;
    private String displayLabel;
    private String regionOperacion;
    private String regionEtiqueta;

    public RepartidorVehiculoDTO() {
    }

    public RepartidorVehiculoDTO(Long id, String nombre, String username, String placa, String modelo,
                                 Double capacidadPesoKg, Double pesoOcupadoKg, Double porcentajeOcupacion) {
        this.id = id;
        this.nombre = nombre;
        this.username = username;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadPesoKg = capacidadPesoKg;
        this.pesoOcupadoKg = pesoOcupadoKg;
        this.pesoDisponibleKg = Math.max(0.0, capacidadPesoKg - pesoOcupadoKg);
        this.porcentajeOcupacion = porcentajeOcupacion;
        this.displayLabel = nombre + " — Furgoneta [" + placa + "] (Carga: " + Math.round(porcentajeOcupacion) + "% | " 
                + Math.round(pesoOcupadoKg) + "/" + Math.round(capacidadPesoKg) + " kg)";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Double getCapacidadPesoKg() {
        return capacidadPesoKg;
    }

    public void setCapacidadPesoKg(Double capacidadPesoKg) {
        this.capacidadPesoKg = capacidadPesoKg;
    }

    public Double getPesoOcupadoKg() {
        return pesoOcupadoKg;
    }

    public void setPesoOcupadoKg(Double pesoOcupadoKg) {
        this.pesoOcupadoKg = pesoOcupadoKg;
    }

    public Double getPesoDisponibleKg() {
        return pesoDisponibleKg;
    }

    public void setPesoDisponibleKg(Double pesoDisponibleKg) {
        this.pesoDisponibleKg = pesoDisponibleKg;
    }

    public Double getPorcentajeOcupacion() {
        return porcentajeOcupacion;
    }

    public void setPorcentajeOcupacion(Double porcentajeOcupacion) {
        this.porcentajeOcupacion = porcentajeOcupacion;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getRegionOperacion() {
        return regionOperacion;
    }

    public void setRegionOperacion(String regionOperacion) {
        this.regionOperacion = regionOperacion;
    }

    public String getRegionEtiqueta() {
        return regionEtiqueta;
    }

    public void setRegionEtiqueta(String regionEtiqueta) {
        this.regionEtiqueta = regionEtiqueta;
    }
}
