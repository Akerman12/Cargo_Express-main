package com.example.guias.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario") 
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String username;

    @Column
    private String password; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Rol rol = Rol.ROLE_CLIENTE;

    @Column(nullable = false)
    private boolean aprobado = false; 

    @Email(message = "Debe ingresar un correo electrónico válido")
    @Column(unique = true)
    private String email;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Guia> guias = new HashSet<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToOne(mappedBy = "repartidor", fetch = FetchType.LAZY)
    private Vehiculo vehiculo;

    // Región donde opera el repartidor (obligatoria para repartidores; null para clientes/admin)
    @Enumerated(EnumType.STRING)
    @Column(name = "region_operacion", length = 30)
    private Region regionOperacion;

    
    public Usuario() {}

    public Usuario(String nombre, String username, String password, Rol rol) {
        this.nombre = nombre;
        this.username = username;
        this.password = password;
        this.rol = rol != null ? rol : Rol.ROLE_CLIENTE;
    }

   
    public void agregarGuia(Guia guia) {
        guias.add(guia);
        guia.setUsuario(this);
    }

    public void eliminarGuia(Guia guia) {
        guias.remove(guia);
        guia.setUsuario(null);
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Set<Guia> getGuias() {
        return guias;
    }

    public void setGuias(Set<Guia> guias) {
        this.guias = guias;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public void setAprobado(boolean aprobado) {
        this.aprobado = aprobado;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Region getRegionOperacion() {
        return regionOperacion;
    }

    public void setRegionOperacion(Region regionOperacion) {
        this.regionOperacion = regionOperacion;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    @PrePersist
    @PreUpdate
    public void syncEmail() {
        if (this.email == null || this.email.isBlank()) {
            this.email = this.username;
        }
        if (this.username == null || this.username.isBlank()) {
            this.username = this.email;
        }
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", username='" + username + '\'' +
                ", rol=" + rol +
                '}';
    }
}
