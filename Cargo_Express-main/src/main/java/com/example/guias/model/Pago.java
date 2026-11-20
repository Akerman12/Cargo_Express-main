package com.example.guias.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monto", nullable = false)
    private double monto;

    @Column(name = "metodo", nullable = false)
    private String metodo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "titular")
    private String titular;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id", nullable = false)
    private Guia guia;

    public Pago() {}

    public Pago(double monto, String metodo, String titular, Guia guia) {
        this.monto = monto;
        this.metodo = metodo;
        this.titular = titular;
        this.guia = guia;
        this.fecha = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public Guia getGuia() { return guia; }
    public void setGuia(Guia guia) { this.guia = guia; }
}
