package com.example.guias.service;

import com.example.guias.model.Guia;
import com.example.guias.model.Usuario; 
import com.example.guias.repository.GuiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GuiaService {

    @Autowired
    private GuiaRepository guiaRepository;

    public List<Guia> obtenerTodas() {
        // Solo retornamos guías que no estén eliminadas
        return guiaRepository.findByEstadoNot("Eliminada");
    }

    public List<Guia> obtenerTodasIncluyendoEliminadas() {
        return guiaRepository.findAll();
    }

    public Optional<Guia> buscarPorId(Long id) {
        return guiaRepository.findById(id);
    }

    public Optional<Guia> buscarPorIdYUsuario(Long id, Usuario usuario) {
        return guiaRepository.findByIdAndUsuario(id, usuario);
    }

    public Guia guardarGuia(Guia guia) {
        return guiaRepository.save(guia);
    }

    public void eliminarGuia(Long id) {
        // Ahora el borrado físico está deshabilitado por seguridad
        // guiaRepository.deleteById(id);
    }

    public void marcarComoEliminada(Guia guia) {
        guia.setEstado(com.example.guias.model.EstadoGuia.ELIMINADA);
        guia.setRepartidorAsignado(null);
        guiaRepository.save(guia);
    }

    public List<Guia> obtenerPorUsuario(Usuario usuario) {
        // El usuario solo ve sus guías activas
        return guiaRepository.findByUsuarioAndEstadoNot(usuario, "Eliminada"); 
    }

    public List<Guia> obtenerPorRepartidorAsignado(Usuario repartidor) {
        return guiaRepository.findByRepartidorAsignadoAndEstadoNotIn(repartidor, List.of("Entregado", "Eliminada"));
    }

    public List<Guia> obtenerPendientesDeRuta(Usuario repartidor) {
        return guiaRepository.findByRepartidorAsignadoAndRutaIsNullAndEstadoNotIn(
                repartidor, List.of("Entregado", "Eliminada", "Cancelado"));
    }

    public double calcularDistanciaTotal() {
        List<Guia> guias = obtenerTodas();
        return guias.stream()
                .mapToDouble(com.example.guias.util.DistanciaUtil::obtenerDistanciaGuia)
                .sum();
    }
}