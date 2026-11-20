package com.example.guias.model;


public enum Rol {

    ROLE_CLIENTE("Cliente"),
    ROLE_REPARTIDOR("Repartidor"),
    ROLE_ADMIN("Administrador");

    private final String displayName;

    Rol(String displayName) {
        this.displayName = displayName;
    }

   
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
