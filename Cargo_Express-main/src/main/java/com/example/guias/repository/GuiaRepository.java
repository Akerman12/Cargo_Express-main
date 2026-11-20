package com.example.guias.repository;

import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuiaRepository extends JpaRepository<Guia, Long> {
    // Consultas que excluyen los registros marcados como eliminados
    List<Guia> findByEstadoNot(String estado);
    List<Guia> findByUsuarioAndEstadoNot(Usuario usuario, String estado);
    List<Guia> findByRepartidorAsignadoAndEstadoNotIn(Usuario repartidor, List<String> estados);

    List<Guia> findByRepartidorAsignadoAndRutaIsNullAndEstadoNotIn(Usuario repartidor, List<String> estados);

    // Consulta privada de SAI-Bot: solo guias del propio cliente
    Optional<Guia> findByIdAndUsuario(Long id, Usuario usuario);

    // Ciudades con mas movimiento (para el asistente SAI)
    @Query("select g.ciudadOrigen, count(g) from Guia g group by g.ciudadOrigen order by count(g) desc")
    List<Object[]> ciudadesOrigenFrecuentes(Pageable pageable);

    @Query("select g.ciudadDestino, count(g) from Guia g group by g.ciudadDestino order by count(g) desc")
    List<Object[]> ciudadesDestinoFrecuentes(Pageable pageable);
}
