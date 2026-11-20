package com.example.guias.repository;

import com.example.guias.model.Usuario;
import com.example.guias.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlaca(String placa);

    Optional<Vehiculo> findByRepartidor(Usuario repartidor);

    Optional<Vehiculo> findByRepartidorId(Long repartidorId);

    List<Vehiculo> findByRepartidorIsNull();

    List<Vehiculo> findByRepartidorIsNotNull();

    boolean existsByPlaca(String placa);
}
