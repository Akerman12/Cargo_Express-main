package com.example.guias.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.guias.model.Usuario;
import com.example.guias.model.Rol;
import com.example.guias.repository.UsuarioRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        OAuth2User oAuth2User = super.loadUser(userRequest);

        
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("No se pudo obtener el correo del usuario de Google.");
        }

        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(email);
        Usuario usuario;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();

           
            if (nombre != null && !nombre.equals(usuario.getNombre())) {
                usuario.setNombre(nombre);
                usuarioRepository.save(usuario);
            }

           
            if (usuario.getRol() == null) {
                usuario.setRol(Rol.ROLE_CLIENTE);
                usuarioRepository.save(usuario);
            }

        } else {
            
            usuario = new Usuario();
            usuario.setUsername(email);
            usuario.setNombre(nombre != null ? nombre : email);
            usuario.setPassword(null); 
            usuario.setRol(Rol.ROLE_CLIENTE); 

            usuarioRepository.save(usuario);
            System.out.println("✅ Nuevo usuario de Google guardado en la BD: " + email);
        }

        
        String rolNombre = usuario.getRol().name();
        if (!rolNombre.startsWith("ROLE_")) {
            rolNombre = "ROLE_" + rolNombre;
        }

       
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(rolNombre)),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}
