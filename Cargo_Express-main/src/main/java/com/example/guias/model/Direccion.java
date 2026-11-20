package com.example.guias.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Direccion")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String alias; 

    @Column(nullable = false)
    private String nombreContacto;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private String departamento;

    @Column(nullable = false)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Direccion() {}

    public Direccion(String alias, String nombreContacto, String direccion, String ciudad, String departamento, String telefono, Usuario usuario) {
        this.alias = alias;
        this.nombreContacto = nombreContacto;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.departamento = departamento;
        this.telefono = telefono;
        this.usuario = usuario;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
