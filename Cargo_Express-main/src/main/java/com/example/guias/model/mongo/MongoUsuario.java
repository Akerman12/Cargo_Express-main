package com.example.guias.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "usuarios")
public class MongoUsuario {

    @Id
    private String id;
    
   
    private Long sqlId;

    private String nombre;
    private String username;
    private String password;
    private String rol;
    private boolean aprobado;
    private String email;

   
    private Set<String> guiaIds = new HashSet<>();

    public MongoUsuario() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSqlId() { return sqlId; }
    public void setSqlId(Long sqlId) { this.sqlId = sqlId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean aprobado) { this.aprobado = aprobado; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<String> getGuiaIds() { return guiaIds; }
    public void setGuiaIds(Set<String> guiaIds) { this.guiaIds = guiaIds; }
}
