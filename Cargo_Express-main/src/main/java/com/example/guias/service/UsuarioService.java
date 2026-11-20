package com.example.guias.service;

import com.example.guias.model.PasswordResetToken;
import com.example.guias.model.Usuario;
import com.example.guias.repository.PasswordResetTokenRepository;
import com.example.guias.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, 
                          PasswordResetTokenRepository tokenRepository, 
                          BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> findByUsername(String email) {
        return usuarioRepository.findByUsername(email);
    }

    @Transactional
    public String createPasswordResetTokenForUser(Usuario usuario) {
        // Eliminar tokens previos para este usuario
        tokenRepository.deleteByUsuario(usuario);
        
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(token, usuario);
        tokenRepository.save(myToken);
        return token;
    }

    public Optional<Usuario> getUserByPasswordResetToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired())
                .map(PasswordResetToken::getUsuario);
    }

    @Transactional
    public void changeUserPassword(Usuario usuario, String newPassword) {
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
        tokenRepository.deleteByUsuario(usuario);
    }
}
