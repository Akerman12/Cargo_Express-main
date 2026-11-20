
package com.example.guias.service;

import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ValidacionClienteService {

    private final UsuarioRepository usuarioRepository;

    public ValidacionClienteService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Usuario> validarCliente(String nombre, String email) {

        if (nombre == null || nombre.isBlank()
                || email == null || email.isBlank()) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(usuario ->
                    usuario.getNombre() != null
                    && usuario.getNombre().trim()
                        .equalsIgnoreCase(nombre.trim())
                    && usuario.isAprobado()
                );
    }
}
