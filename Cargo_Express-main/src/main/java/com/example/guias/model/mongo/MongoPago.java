package com.example.guias.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "pagos")
public class MongoPago {

    @Id
    private String id;
    
    private Long sqlId;

    private double monto;
    private String metodo;
    private LocalDateTime fecha;
    private String titular;

    private String guiaId; 

    public MongoPago() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSqlId() { return sqlId; }
    public void setSqlId(Long sqlId) { this.sqlId = sqlId; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public String getGuiaId() { return guiaId; }
    public void setGuiaId(String guiaId) { this.guiaId = guiaId; }
}
