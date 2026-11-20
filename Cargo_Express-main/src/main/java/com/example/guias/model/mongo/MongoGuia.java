package com.example.guias.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "guias")
public class MongoGuia {

    @Id
    private String id;
    
    
    private Long sqlId;

    private String remitente;
    private String destinatario;
    private String direccionEnvio;
    private String direccionRecibo;
    private String cedulaRemitente;
    private String cedulaDestinatario;
    private String ciudadOrigen;
    private String ciudadDestino;
    private String departamentoOrigen;
    private String departamentoDestino;
    private String tipoCarga;
    private double pesoKg;
    private double alturaCm;
    private double anchoCm;
    private double valorDeclarado;
    private double precio;
    private LocalDateTime fechaCreacion;
    private String estado;
    private String estadoPago;
    
    private Long usuarioId; 
    
    private Long repartidorAsignadoId;

    private String modificadoPor;
    private String firmaEntrega;
    private String notasEntrega;
    private LocalDateTime fechaEntrega;
    private Double tiempoEstimadoDias;

    public MongoGuia() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getSqlId() { return sqlId; }
    public void setSqlId(Long sqlId) { this.sqlId = sqlId; }

    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }

    public String getDireccionRecibo() { return direccionRecibo; }
    public void setDireccionRecibo(String direccionRecibo) { this.direccionRecibo = direccionRecibo; }

    public String getCedulaRemitente() { return cedulaRemitente; }
    public void setCedulaRemitente(String cedulaRemitente) { this.cedulaRemitente = cedulaRemitente; }

    public String getCedulaDestinatario() { return cedulaDestinatario; }
    public void setCedulaDestinatario(String cedulaDestinatario) { this.cedulaDestinatario = cedulaDestinatario; }

    public String getCiudadOrigen() { return ciudadOrigen; }
    public void setCiudadOrigen(String ciudadOrigen) { this.ciudadOrigen = ciudadOrigen; }

    public String getCiudadDestino() { return ciudadDestino; }
    public void setCiudadDestino(String ciudadDestino) { this.ciudadDestino = ciudadDestino; }

    public String getDepartamentoOrigen() { return departamentoOrigen; }
    public void setDepartamentoOrigen(String departamentoOrigen) { this.departamentoOrigen = departamentoOrigen; }

    public String getDepartamentoDestino() { return departamentoDestino; }
    public void setDepartamentoDestino(String departamentoDestino) { this.departamentoDestino = departamentoDestino; }

    public String getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }

    public double getPesoKg() { return pesoKg; }
    public void setPesoKg(double pesoKg) { this.pesoKg = pesoKg; }

    public double getAlturaCm() { return alturaCm; }
    public void setAlturaCm(double alturaCm) { this.alturaCm = alturaCm; }

    public double getAnchoCm() { return anchoCm; }
    public void setAnchoCm(double anchoCm) { this.anchoCm = anchoCm; }

    public double getValorDeclarado() { return valorDeclarado; }
    public void setValorDeclarado(double valorDeclarado) { this.valorDeclarado = valorDeclarado; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getRepartidorAsignadoId() { return repartidorAsignadoId; }
    public void setRepartidorAsignadoId(Long repartidorAsignadoId) { this.repartidorAsignadoId = repartidorAsignadoId; }

    public String getModificadoPor() { return modificadoPor; }
    public void setModificadoPor(String modificadoPor) { this.modificadoPor = modificadoPor; }

    public String getFirmaEntrega() { return firmaEntrega; }
    public void setFirmaEntrega(String firmaEntrega) { this.firmaEntrega = firmaEntrega; }

    public String getNotasEntrega() { return notasEntrega; }
    public void setNotasEntrega(String notasEntrega) { this.notasEntrega = notasEntrega; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public Double getTiempoEstimadoDias() { return tiempoEstimadoDias; }
    public void setTiempoEstimadoDias(Double tiempoEstimadoDias) { this.tiempoEstimadoDias = tiempoEstimadoDias; }
}
