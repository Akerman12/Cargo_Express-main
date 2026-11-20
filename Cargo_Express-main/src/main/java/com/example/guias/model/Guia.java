package com.example.guias.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Guia")
public class Guia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El remitente no puede estar vacío")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre del remitente solo puede contener letras y espacios")
    @Column(name = "remitente", nullable = false)
    private String remitente;

    @NotBlank(message = "El destinatario no puede estar vacío")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre del destinatario solo puede contener letras y espacios")
    @Column(name = "destinatario", nullable = false)
    private String destinatario;

    @NotBlank(message = "La dirección de envío no puede estar vacía")
    @Column(name = "direccionEnvio", nullable = false)
    private String direccionEnvio;

    @NotBlank(message = "La dirección de recibo no puede estar vacía")
    @Column(name = "direccionRecibo", nullable = false)
    private String direccionRecibo;

    @NotBlank(message = "La cédula del remitente es obligatoria")
    @Column(name = "cedulaRemitente", nullable = false)
    private String cedulaRemitente;

    @NotBlank(message = "La cédula del destinatario es obligatoria")
    @Column(name = "cedulaDestinatario", nullable = false)
    private String cedulaDestinatario;

    @NotBlank(message = "La ciudad de origen es obligatoria")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s\\.]+$", message = "La ciudad de origen solo puede contener letras")
    @Column(name = "ciudadOrigen", nullable = false)
    private String ciudadOrigen;

    @NotBlank(message = "La ciudad de destino es obligatoria")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s\\.]+$", message = "La ciudad de destino solo puede contener letras")
    @Column(name = "ciudadDestino", nullable = false)
    private String ciudadDestino;

    @NotBlank(message = "El departamento de origen es obligatorio")
    @Column(name = "departamentoOrigen", nullable = false)
    private String departamentoOrigen;

    @NotBlank(message = "El departamento de destino es obligatorio")
    @Column(name = "departamentoDestino", nullable = false)
    private String departamentoDestino;

    @NotBlank(message = "El tipo de carga es obligatorio")
    @Column(name = "tipoCarga", nullable = false)
    private String tipoCarga;

    @Positive(message = "El peso debe ser mayor que cero")
    @Column(name = "pesoKg", nullable = false)
    private double pesoKg;

    @Positive(message = "La altura debe ser mayor que cero")
    @Column(name = "alturaCm", nullable = false)
    private double alturaCm;

    @Positive(message = "El ancho debe ser mayor que cero")
    @Column(name = "anchoCm", nullable = false)
    private double anchoCm;

    // Opcional: guias antiguas no lo tienen (se asume 20 cm al cotizar)
    @Positive(message = "El largo debe ser mayor que cero")
    @Column(name = "largoCm")
    private Double largoCm;

    @PositiveOrZero(message = "El valor declarado no puede ser negativo")
    @Column(name = "valorDeclarado", nullable = false)
    private double valorDeclarado;

    @Column(name = "precio", nullable = false)
    private double precio;

    @Column(name = "fechaCreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "estado", nullable = false)
    private String estado = "Pendiente";

    @Column(name = "estadoPago", nullable = false)
    private String estadoPago = "PENDIENTE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id", nullable = true)
    private Usuario repartidorAsignado;

    @Column(name = "modificadoPor")
    private String modificadoPor;

    @Column(name = "firmaEntrega", columnDefinition = "LONGTEXT")
    private String firmaEntrega;

    @Column(name = "notasEntrega", columnDefinition = "TEXT")
    private String notasEntrega;

    @Column(name = "fechaEntrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "tiempoEstimadoDias")
    private Double tiempoEstimadoDias;

    @Column(name = "latOrigen")
    private Double latOrigen;

    @Column(name = "lonOrigen")
    private Double lonOrigen;

    @Column(name = "latDestino")
    private Double latDestino;

    @Column(name = "lonDestino")
    private Double lonDestino;

    @Column(name = "distanciaKm")
    private Double distanciaKm;

    @Column(name = "tiempoEstimadoMinutos")
    private Integer tiempoEstimadoMinutos;

    @Column(name = "motivoNovedad")
    private String motivoNovedad;

    @Column(name = "ordenEntrega")
    private Integer ordenEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = true)
    private Ruta ruta;

    // --- Fase de recogida (multi-parada) y responsables por fase ---
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_recogida_id", nullable = true)
    private Ruta rutaRecogida;

    @Column(name = "ordenRecogida")
    private Integer ordenRecogida;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_recogida_id", nullable = true)
    private Usuario repartidorRecogida;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_entrega_id", nullable = true)
    private Usuario repartidorEntrega;

    public Guia() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getDireccionEnvio() {
        return direccionEnvio;
    }

    public void setDireccionEnvio(String direccionEnvio) {
        this.direccionEnvio = direccionEnvio;
    }

    public String getDireccionRecibo() {
        return direccionRecibo;
    }

    public void setDireccionRecibo(String direccionRecibo) {
        this.direccionRecibo = direccionRecibo;
    }

    public String getCedulaRemitente() {
        return cedulaRemitente;
    }

    public void setCedulaRemitente(String cedulaRemitente) {
        this.cedulaRemitente = cedulaRemitente;
    }

    public String getCedulaDestinatario() {
        return cedulaDestinatario;
    }

    public void setCedulaDestinatario(String cedulaDestinatario) {
        this.cedulaDestinatario = cedulaDestinatario;
    }

    public String getCiudadOrigen() {
        return ciudadOrigen;
    }

    public void setCiudadOrigen(String ciudadOrigen) {
        this.ciudadOrigen = ciudadOrigen;
    }

    public String getCiudadDestino() {
        return ciudadDestino;
    }

    public void setCiudadDestino(String ciudadDestino) {
        this.ciudadDestino = ciudadDestino;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Double getLargoCm() {
        return largoCm;
    }

    public void setLargoCm(Double largoCm) {
        this.largoCm = largoCm;
    }

    public double getAlturaCm() {
        return alturaCm;
    }

    public void setAlturaCm(double alturaCm) {
        this.alturaCm = alturaCm;
    }

    public double getAnchoCm() {
        return anchoCm;
    }

    public void setAnchoCm(double anchoCm) {
        this.anchoCm = anchoCm;
    }

    public double getValorDeclarado() {
        return valorDeclarado;
    }

    public void setValorDeclarado(double valorDeclarado) {
        this.valorDeclarado = valorDeclarado;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getRepartidorAsignado() {
        return repartidorAsignado;
    }

    public void setRepartidorAsignado(Usuario repartidorAsignado) {
        this.repartidorAsignado = repartidorAsignado;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    public String getDepartamentoOrigen() {
        return departamentoOrigen;
    }

    public void setDepartamentoOrigen(String departamentoOrigen) {
        this.departamentoOrigen = departamentoOrigen;
    }

    public String getDepartamentoDestino() {
        return departamentoDestino;
    }

    public void setDepartamentoDestino(String departamentoDestino) {
        this.departamentoDestino = departamentoDestino;
    }

    public String getFirmaEntrega() {
        return firmaEntrega;
    }

    public void setFirmaEntrega(String firmaEntrega) {
        this.firmaEntrega = firmaEntrega;
    }

    public String getNotasEntrega() {
        return notasEntrega;
    }

    public void setNotasEntrega(String notasEntrega) {
        this.notasEntrega = notasEntrega;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Double getTiempoEstimadoDias() {
        return tiempoEstimadoDias;
    }

    public void setTiempoEstimadoDias(Double tiempoEstimadoDias) {
        this.tiempoEstimadoDias = tiempoEstimadoDias;
    }

    public Double getLatOrigen() {
        return latOrigen;
    }

    public void setLatOrigen(Double latOrigen) {
        this.latOrigen = latOrigen;
    }

    public Double getLonOrigen() {
        return lonOrigen;
    }

    public void setLonOrigen(Double lonOrigen) {
        this.lonOrigen = lonOrigen;
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

    public Double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public Integer getTiempoEstimadoMinutos() {
        return tiempoEstimadoMinutos;
    }

    public void setTiempoEstimadoMinutos(Integer tiempoEstimadoMinutos) {
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }

    public String getMotivoNovedad() {
        return motivoNovedad;
    }

    public void setMotivoNovedad(String motivoNovedad) {
        this.motivoNovedad = motivoNovedad;
    }

    public Integer getOrdenEntrega() {
        return ordenEntrega;
    }

    public void setOrdenEntrega(Integer ordenEntrega) {
        this.ordenEntrega = ordenEntrega;
    }

    public Ruta getRuta() {
        return ruta;
    }

    public void setRuta(Ruta ruta) {
        this.ruta = ruta;
    }

    public Ruta getRutaRecogida() {
        return rutaRecogida;
    }

    public void setRutaRecogida(Ruta rutaRecogida) {
        this.rutaRecogida = rutaRecogida;
    }

    public Integer getOrdenRecogida() {
        return ordenRecogida;
    }

    public void setOrdenRecogida(Integer ordenRecogida) {
        this.ordenRecogida = ordenRecogida;
    }

    public Usuario getRepartidorRecogida() {
        return repartidorRecogida;
    }

    public void setRepartidorRecogida(Usuario repartidorRecogida) {
        this.repartidorRecogida = repartidorRecogida;
    }

    public Usuario getRepartidorEntrega() {
        return repartidorEntrega;
    }

    public void setRepartidorEntrega(Usuario repartidorEntrega) {
        this.repartidorEntrega = repartidorEntrega;
    }

    /** Región (nombre del enum) del departamento de origen; vacío si no se puede determinar. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getRegionOrigenNombre() {
        Region r = Region.deDepartamento(departamentoOrigen);
        return r == null ? "" : r.name();
    }

    /** Región (nombre del enum) del departamento de destino; vacío si no se puede determinar. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getRegionDestinoNombre() {
        Region r = Region.deDepartamento(departamentoDestino);
        return r == null ? "" : r.name();
    }
}
