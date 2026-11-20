package com.example.guias.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "direcciones")
public class MongoDireccion {

    @Id
    private String id;
    
    private Long sqlId;

    private String alias;
    private String nombreContacto;
    private String direccion;
    private String ciudad;
    private String departamento;
    private String telefono;

    private String usuarioId; 

    public MongoDireccion() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSqlId() { return sqlId; }
    public void setSqlId(Long sqlId) { this.sqlId = sqlId; }

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

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
}
