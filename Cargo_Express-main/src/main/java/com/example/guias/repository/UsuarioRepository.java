package com.example.guias.repository;

import com.example.guias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import com.example.guias.model.Rol;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByRol(Rol rol);
}
