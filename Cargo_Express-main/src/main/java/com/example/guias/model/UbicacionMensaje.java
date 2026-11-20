package com.example.guias.model;

public class UbicacionMensaje {
    private Long guiaId;
    private double latitud;
    private double longitud;

    public UbicacionMensaje() {
    }

    public UbicacionMensaje(Long guiaId, double latitud, double longitud) {
        this.guiaId = guiaId;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Long getGuiaId() {
        return guiaId;
    }

    public void setGuiaId(Long guiaId) {
        this.guiaId = guiaId;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
}
