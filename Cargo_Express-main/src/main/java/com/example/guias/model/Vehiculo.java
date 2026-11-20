package com.example.guias.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    @Column(name = "placa", unique = true, nullable = false, length = 20)
    private String placa;

    @NotBlank(message = "El modelo es obligatorio")
    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo = "FURGONETA";

    @NotNull(message = "La capacidad en peso es obligatoria")
    @Positive(message = "La capacidad de peso debe ser mayor a 0 kg")
    @Column(name = "capacidad_peso_kg", nullable = false)
    private Double capacidadPesoKg;

    @NotNull(message = "La capacidad volumétrica es obligatoria")
    @Positive(message = "La capacidad en volumen debe ser mayor a 0 m³")
    @Column(name = "capacidad_volumen_m3", nullable = false)
    private Double capacidadVolumenM3;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "DISPONIBLE"; // DISPONIBLE, ASIGNADO, MANTENIMIENTO

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id", unique = true, nullable = true)
    private Usuario repartidor;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public Vehiculo() {
    }

    public Vehiculo(String placa, String modelo, String tipo, Double capacidadPesoKg, Double capacidadVolumenM3) {
        this.placa = placa;
        this.modelo = modelo;
        this.tipo = (tipo != null && !tipo.isBlank()) ? tipo : "FURGONETA";
        this.capacidadPesoKg = capacidadPesoKg;
        this.capacidadVolumenM3 = capacidadVolumenM3;
        this.estado = "DISPONIBLE";
        this.fechaRegistro = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = (placa != null) ? placa.toUpperCase().trim() : null;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getCapacidadPesoKg() {
        return capacidadPesoKg;
    }

    public void setCapacidadPesoKg(Double capacidadPesoKg) {
        this.capacidadPesoKg = capacidadPesoKg;
    }

    public Double getCapacidadVolumenM3() {
        return capacidadVolumenM3;
    }

    public void setCapacidadVolumenM3(Double capacidadVolumenM3) {
        this.capacidadVolumenM3 = capacidadVolumenM3;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(Usuario repartidor) {
        this.repartidor = repartidor;
        if (repartidor != null) {
            this.estado = "ASIGNADO";
        } else if ("ASIGNADO".equalsIgnoreCase(this.estado)) {
            this.estado = "DISPONIBLE";
        }
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean estaAsignado() {
        return this.repartidor != null;
    }
}

