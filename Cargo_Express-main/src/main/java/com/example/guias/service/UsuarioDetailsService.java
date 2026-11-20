package com.example.guias.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.guias.model.Usuario;
import com.example.guias.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("❌ Usuario no encontrado: " + username));

        
        String rol = (usuario.getRol() != null) ? usuario.getRol().name() : "ROLE_CLIENTE";

        
        if (!rol.startsWith("ROLE_")) {
            rol = "ROLE_" + rol;
        }

     
        String password = usuario.getPassword();
        if (password == null || password.isBlank()) {
           
            password = "{noop}OAUTH_USER";
        }

       
        return User.builder()
                .username(usuario.getUsername())
                .password(password)
                .authorities(List.of(new SimpleGrantedAuthority(rol)))
                .accountLocked(!usuario.isAprobado())
                .build();
    }
}
