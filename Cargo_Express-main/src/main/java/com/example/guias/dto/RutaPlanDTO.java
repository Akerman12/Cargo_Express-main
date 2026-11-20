package com.example.guias.dto;

import java.util.ArrayList;
import java.util.List;

public class RutaPlanDTO {

    private Long repartidorId;
    private String repartidorNombre;
    private String vehiculoPlaca;
    private String vehiculoModelo;
    private Double capacidadPesoKg;
    private Double pesoCargadoKg;
    private boolean tieneRutaActiva;
    private Long rutaActivaId;
    private String estadoRutaActiva;
    private String capitalSugerida;
    private Double hubLat;
    private Double hubLon;
    private List<ParadaDTO> paradas = new ArrayList<>();

    public RutaPlanDTO() {
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

    public Double getCapacidadPesoKg() {
        return capacidadPesoKg;
    }

    public void setCapacidadPesoKg(Double capacidadPesoKg) {
        this.capacidadPesoKg = capacidadPesoKg;
    }

    public Double getPesoCargadoKg() {
        return pesoCargadoKg;
    }

    public void setPesoCargadoKg(Double pesoCargadoKg) {
        this.pesoCargadoKg = pesoCargadoKg;
    }

    public boolean isTieneRutaActiva() {
        return tieneRutaActiva;
    }

    public void setTieneRutaActiva(boolean tieneRutaActiva) {
        this.tieneRutaActiva = tieneRutaActiva;
    }

    public Long getRutaActivaId() {
        return rutaActivaId;
    }

    public void setRutaActivaId(Long rutaActivaId) {
        this.rutaActivaId = rutaActivaId;
    }

    public String getEstadoRutaActiva() {
        return estadoRutaActiva;
    }

    public void setEstadoRutaActiva(String estadoRutaActiva) {
        this.estadoRutaActiva = estadoRutaActiva;
    }

    public String getCapitalSugerida() {
        return capitalSugerida;
    }

    public void setCapitalSugerida(String capitalSugerida) {
        this.capitalSugerida = capitalSugerida;
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

    public List<ParadaDTO> getParadas() {
        return paradas;
    }

    public void setParadas(List<ParadaDTO> paradas) {
        this.paradas = paradas;
    }

    private String tipo = "ENTREGA";
    private String regionOperacion;
    private List<String> capitalesRegion = new ArrayList<>();

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getRegionOperacion() {
        return regionOperacion;
    }

    public void setRegionOperacion(String regionOperacion) {
        this.regionOperacion = regionOperacion;
    }

    public List<String> getCapitalesRegion() {
        return capitalesRegion;
    }

    public void setCapitalesRegion(List<String> capitalesRegion) {
        this.capitalesRegion = capitalesRegion;
    }

    public static class ParadaDTO {
        private Long guiaId;
        private Integer ordenEntrega;
        private String destinatario;
        private String direccionRecibo;
        private String ciudadDestino;
        private String departamentoDestino;
        private Double pesoKg;
        private String estado;
        private Double latDestino;
        private Double lonDestino;
        private Double ciudadLat;
        private Double ciudadLon;

        public ParadaDTO() {
        }

        public Long getGuiaId() {
            return guiaId;
        }

        public void setGuiaId(Long guiaId) {
            this.guiaId = guiaId;
        }

        public Integer getOrdenEntrega() {
            return ordenEntrega;
        }

        public void setOrdenEntrega(Integer ordenEntrega) {
            this.ordenEntrega = ordenEntrega;
        }

        public String getDestinatario() {
            return destinatario;
        }

        public void setDestinatario(String destinatario) {
            this.destinatario = destinatario;
        }

        public String getDireccionRecibo() {
            return direccionRecibo;
        }

        public void setDireccionRecibo(String direccionRecibo) {
            this.direccionRecibo = direccionRecibo;
        }

        public String getCiudadDestino() {
            return ciudadDestino;
        }

        public void setCiudadDestino(String ciudadDestino) {
            this.ciudadDestino = ciudadDestino;
        }

        public String getDepartamentoDestino() {
            return departamentoDestino;
        }

        public void setDepartamentoDestino(String departamentoDestino) {
            this.departamentoDestino = departamentoDestino;
        }

        public Double getPesoKg() {
            return pesoKg;
        }

        public void setPesoKg(Double pesoKg) {
            this.pesoKg = pesoKg;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public Double getLatDestino() {
            return latDestino;
        }

        public void setLatDestino(Double latDestino) {
            this.latDestino = latDestino;
        }

        public Double getLonDestino() {
            return lonDestino;
        }

        public void setLonDestino(Double lonDestino) {
            this.lonDestino = lonDestino;
        }

        public Double getCiudadLat() {
            return ciudadLat;
        }

        public void setCiudadLat(Double ciudadLat) {
            this.ciudadLat = ciudadLat;
        }

        public Double getCiudadLon() {
            return ciudadLon;
        }

        public void setCiudadLon(Double ciudadLon) {
            this.ciudadLon = ciudadLon;
        }
    }
}

