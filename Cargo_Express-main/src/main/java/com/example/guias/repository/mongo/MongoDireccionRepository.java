package com.example.guias.repository.mongo;

import com.example.guias.model.mongo.MongoDireccion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoDireccionRepository extends MongoRepository<MongoDireccion, String> {
    List<MongoDireccion> findByUsuarioId(String usuarioId);
    MongoDireccion findBySqlId(Long sqlId);
}
