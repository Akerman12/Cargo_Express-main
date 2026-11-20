package com.example.guias.model;

public class DriverLocationMessage {
    private String username;
    private double latitud;
    private double longitud;

    public DriverLocationMessage() {
    }

    public DriverLocationMessage(String username, double latitud, double longitud) {
        this.username = username;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
