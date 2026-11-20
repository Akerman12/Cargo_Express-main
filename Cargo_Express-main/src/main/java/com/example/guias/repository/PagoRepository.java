package com.example.guias.repository;

import com.example.guias.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    java.util.Optional<Pago> findByGuiaId(Long guiaId);
}
