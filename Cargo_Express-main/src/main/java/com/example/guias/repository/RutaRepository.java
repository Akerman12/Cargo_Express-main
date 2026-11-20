package com.example.guias.repository;

import com.example.guias.model.Ruta;
import com.example.guias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

    List<Ruta> findByRepartidorOrderByFechaDesc(Usuario repartidor);

    Optional<Ruta> findTopByRepartidorAndEstadoInOrderByFechaDesc(Usuario repartidor, List<String> estados);

    List<Ruta> findByEstadoOrderByFechaDesc(String estado);

    List<Ruta> findAllByOrderByFechaDesc();

    boolean existsByRepartidorAndEstadoIn(Usuario repartidor, List<String> estados);
}

